from __future__ import annotations

from typing import Any

from src.constants import MEDICAL_DISCLAIMER
from src.gemini_client import GeminiClient
from src.utils.json_utils import ensure_list, parse_json_object


UNSAFE_PHRASES = [
    "chắc chắn bị",
    "chắc chắn là",
    "uống kháng sinh",
    "kháng sinh",
    "liều",
    "mg/kg",
    "không cần đi khám",
    "không cần gặp bác sĩ",
]

MEDICATION_RISK_KEYWORDS = [
    "thuốc",
    "liều",
    "liều lượng",
    "kháng sinh",
    "paracetamol",
    "ibuprofen",
    "hạ sốt",
    "uống bao nhiêu",
    "bao nhiêu ml",
    "mấy lần",
    "mg/kg",
]

DANGER_SIGN_KEYWORDS = [
    "khó thở",
    "co giật",
    "tím tái",
    "li bì",
    "bỏ bú",
    "mất nước",
    "sốt cao",
    "sốt kéo dài",
]

DEFINITE_DIAGNOSIS_PHRASES = [
    "chắc chắn bị",
    "chắc chắn là",
    "kết luận là",
    "đúng là bị",
]


class AnswerValidationAgent:
    def __init__(self, gemini_client: GeminiClient):
        self.gemini_client = gemini_client

    def validate(
        self,
        user_question: str,
        cleaned_input: dict[str, Any],
        intent_result: dict[str, Any],
        safety_result: dict[str, Any],
        draft_answer: str,
        retrieved_contexts: list[dict[str, Any]],
    ) -> dict[str, Any]:
        try:
            raw = self.gemini_client.generate_text(
                self._build_prompt(
                    user_question,
                    cleaned_input,
                    intent_result,
                    safety_result,
                    draft_answer,
                    retrieved_contexts,
                )
            )
            parsed = parse_json_object(raw)
            return self._normalize(parsed, draft_answer, safety_result, retrieved_contexts)
        except Exception:
            return self._fallback(draft_answer, safety_result, retrieved_contexts)

    def validate_with_risk_gate(
        self,
        user_question: str,
        cleaned_input: dict[str, Any],
        intent_result: dict[str, Any],
        safety_result: dict[str, Any],
        draft_answer: str,
        retrieved_contexts: list[dict[str, Any]],
        agent_result: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        use_gemini, reasons = self.should_use_gemini_validation(
            answer=draft_answer,
            user_question=user_question,
            cleaned_input=cleaned_input,
            intent_result=intent_result,
            safety_result=safety_result,
            retrieved_contexts=retrieved_contexts,
            agent_result=agent_result,
        )
        if use_gemini:
            result = self.validate(
                user_question=user_question,
                cleaned_input=cleaned_input,
                intent_result=intent_result,
                safety_result=safety_result,
                draft_answer=draft_answer,
                retrieved_contexts=retrieved_contexts,
            )
            result["validation_mode"] = "gemini"
            result["gemini_validation_used"] = True
            result["validation_decision"] = "Gemini validation used: high risk because " + "; ".join(reasons)
            result["validation_reasons"] = reasons
            return result

        result = self.rule_based_validate(draft_answer, safety_result, retrieved_contexts)
        result["validation_mode"] = "rule_based"
        result["gemini_validation_used"] = False
        result["validation_decision"] = "Gemini validation skipped: low risk"
        result["validation_reasons"] = reasons or ["No high-risk validation trigger matched."]
        return result

    def should_use_gemini_validation(
        self,
        answer: str,
        user_question: str,
        cleaned_input: dict[str, Any],
        intent_result: dict[str, Any],
        safety_result: dict[str, Any],
        retrieved_contexts: list[dict[str, Any]],
        agent_result: dict[str, Any] | None = None,
    ) -> tuple[bool, list[str]]:
        reasons: list[str] = []
        safety_level = str(safety_result.get("safety_level") or "").lower()
        intent = str(intent_result.get("intent") or "").lower()
        combined = f"{user_question}\n{answer}".lower()
        symptom_text = " ".join(
            str(item)
            for item in (
                cleaned_input.get("main_symptoms") or []
            )
        )
        danger_text = "\n".join(
            [
                user_question,
                symptom_text,
                str(cleaned_input.get("vomit_status") or ""),
                str(cleaned_input.get("feeding_status") or ""),
                " ".join(str(item) for item in safety_result.get("red_flags") or []),
            ]
        ).lower()

        if safety_level in {"emergency", "urgent"}:
            reasons.append(f"safety_level is {safety_level}")
        if intent == "symptom":
            reasons.append("intent is symptom")
        if any(keyword in combined for keyword in MEDICATION_RISK_KEYWORDS):
            reasons.append("question/answer mentions medication or dosage")
        if any(phrase in answer.lower() for phrase in DEFINITE_DIAGNOSIS_PHRASES):
            reasons.append("answer may contain a definite diagnosis")
        if any(keyword in danger_text for keyword in DANGER_SIGN_KEYWORDS) and any(
            phrase in answer.lower() for phrase in ["tại nhà", "cho uống", "theo dõi ở nhà", "xử trí tại nhà"]
        ):
            reasons.append("answer may give home treatment guidance for danger signs")
        if any(keyword in danger_text for keyword in DANGER_SIGN_KEYWORDS):
            reasons.append("question mentions danger signs")
        if len(self._context_domains(retrieved_contexts)) > 1 and any(
            phrase in answer.lower() for phrase in ["mâu thuẫn", "không thống nhất", "trái ngược", "xung đột"]
        ):
            reasons.append("retrieved contexts contain multiple domains with possible conflict")
        if (agent_result or {}).get("requires_medical_caution") is True:
            reasons.append("specialized agent requires medical caution")

        return bool(reasons), reasons

    def rule_based_validate(
        self,
        draft_answer: str,
        safety_result: dict[str, Any],
        retrieved_contexts: list[dict[str, Any]],
    ) -> dict[str, Any]:
        result = self._fallback(draft_answer, safety_result, retrieved_contexts)
        if not result["fixed_answer"]:
            result["fixed_answer"] = self._ensure_disclaimer(draft_answer)
        result["validation_notes"] = list(result.get("validation_notes") or [])
        result["validation_notes"].append("Gemini validation skipped; rule-based validation applied.")
        return result

    def _build_prompt(
        self,
        user_question: str,
        cleaned_input: dict[str, Any],
        intent_result: dict[str, Any],
        safety_result: dict[str, Any],
        draft_answer: str,
        retrieved_contexts: list[dict[str, Any]],
    ) -> str:
        context_summaries = [
            {
                "id": item.get("id"),
                "metadata": item.get("metadata"),
                "document_excerpt": str(item.get("document", ""))[:700],
            }
            for item in retrieved_contexts
        ]
        return f"""
Bạn là Answer Validation Agent cho chatbot y khoa mẹ & bé.
Kiểm tra draft answer theo các tiêu chí:
1. Có khẳng định chẩn đoán chắc chắn không?
2. Có kê thuốc hoặc liều thuốc cụ thể không?
3. Có đưa lời khuyên nguy hiểm không?
4. Có bỏ qua red flag không?
5. Có nói quá phạm vi context không?
6. Có thiếu disclaimer "không thay thế bác sĩ" không?
7. Có nguồn nội bộ đã dùng không?
8. Nếu safety_result là urgent/emergency thì answer có đủ cảnh báo đi khám không?

Nếu không an toàn, sửa thành fixed_answer an toàn hơn. Không tự thêm kiến thức y khoa ngoài context.
Nếu không thể sửa chắc chắn, fixed_answer nên là:
"Thông tin hiện tại chưa đủ để tư vấn an toàn. Bạn nên liên hệ bác sĩ/cơ sở y tế để được đánh giá trực tiếp..."

Trả JSON thuần:
{{
  "is_safe": true,
  "is_grounded": true,
  "issues": [],
  "fixed_answer": "",
  "validation_notes": []
}}

User question: {user_question}
Cleaned input: {cleaned_input}
Intent result: {intent_result}
Safety result: {safety_result}
Retrieved contexts: {context_summaries}
Draft answer:
{draft_answer}
""".strip()

    def _normalize(
        self,
        parsed: dict[str, Any],
        draft_answer: str,
        safety_result: dict[str, Any],
        retrieved_contexts: list[dict[str, Any]],
    ) -> dict[str, Any]:
        fallback = self._fallback(draft_answer, safety_result, retrieved_contexts)
        issues = [str(item) for item in ensure_list(parsed.get("issues")) if str(item).strip()]
        notes = [str(item) for item in ensure_list(parsed.get("validation_notes")) if str(item).strip()]
        fixed_answer = str(parsed.get("fixed_answer") or "").strip()
        result = {
            "is_safe": bool(parsed.get("is_safe", fallback["is_safe"])),
            "is_grounded": bool(parsed.get("is_grounded", fallback["is_grounded"])),
            "issues": issues,
            "fixed_answer": fixed_answer,
            "validation_notes": notes,
        }
        rule_result = self._fallback(fixed_answer or draft_answer, safety_result, retrieved_contexts)
        if not rule_result["is_safe"] or not rule_result["is_grounded"]:
            result["is_safe"] = result["is_safe"] and rule_result["is_safe"]
            result["is_grounded"] = result["is_grounded"] and rule_result["is_grounded"]
            result["issues"] = sorted(set(result["issues"]) | set(rule_result["issues"]))
            result["validation_notes"] = sorted(
                set(result["validation_notes"]) | set(rule_result["validation_notes"])
            )
            result["fixed_answer"] = rule_result["fixed_answer"]
        elif fixed_answer:
            result["fixed_answer"] = self._ensure_disclaimer(fixed_answer)
        return result

    def _fallback(
        self,
        draft_answer: str,
        safety_result: dict[str, Any],
        retrieved_contexts: list[dict[str, Any]],
    ) -> dict[str, Any]:
        lowered = draft_answer.lower()
        issues: list[str] = []
        validation_notes: list[str] = ["Fallback rule-based validation."]

        for phrase in UNSAFE_PHRASES:
            if phrase in lowered and not self._is_negated_safety_phrase(lowered, phrase):
                issues.append(f"Phát hiện cụm không an toàn: {phrase}")

        if MEDICAL_DISCLAIMER.lower() not in lowered:
            issues.append("Thiếu disclaimer không thay thế bác sĩ.")

        if retrieved_contexts and "nguồn nội bộ" not in lowered and "collection" not in lowered:
            issues.append("Thiếu mục nguồn nội bộ đã dùng.")

        safety_level = safety_result.get("safety_level")
        if safety_level == "emergency" and not any(word in lowered for word in ["cấp cứu", "gọi cấp cứu"]):
            issues.append("Emergency answer chưa nhấn mạnh cấp cứu.")
        if safety_level == "urgent" and not any(word in lowered for word in ["đi khám", "bác sĩ", "cơ sở y tế"]):
            issues.append("Urgent answer chưa khuyên liên hệ bác sĩ/cơ sở y tế.")

        is_safe = not any("kháng sinh" in issue or "liều" in issue or "không an toàn" in issue for issue in issues)
        is_safe = is_safe and not any(phrase in lowered for phrase in ["không cần đi khám", "không cần gặp bác sĩ"])
        is_grounded = not retrieved_contexts or "nguồn nội bộ" in lowered or "collection" in lowered

        fixed_answer = ""
        if issues:
            fixed_answer = self._make_fixed_answer(
                draft_answer,
                safety_result,
                retrieved_contexts,
                unsafe=not is_safe,
            )

        return {
            "is_safe": is_safe,
            "is_grounded": is_grounded,
            "issues": issues,
            "fixed_answer": fixed_answer,
            "validation_notes": validation_notes,
        }

    def _is_negated_safety_phrase(self, lowered_answer: str, phrase: str) -> bool:
        if phrase not in {"liều", "kháng sinh"}:
            return False
        safe_mentions = [
            f"không đưa {phrase}",
            f"không tự {phrase}",
            f"không kê {phrase}",
            f"không dùng {phrase}",
            f"không cung cấp {phrase}",
        ]
        return any(safe in lowered_answer for safe in safe_mentions)

    def _make_fixed_answer(
        self,
        draft_answer: str,
        safety_result: dict[str, Any],
        retrieved_contexts: list[dict[str, Any]],
        unsafe: bool,
    ) -> str:
        if unsafe:
            return (
                "Thông tin hiện tại chưa đủ để tư vấn an toàn. "
                "Bạn nên liên hệ bác sĩ/cơ sở y tế để được đánh giá trực tiếp. "
                f"{MEDICAL_DISCLAIMER}"
            )

        fixed = draft_answer.strip()
        if safety_result.get("safety_level") == "emergency":
            fixed = (
                "Mô tả có dấu hiệu nguy hiểm. Hãy đưa trẻ đi cấp cứu hoặc gọi cấp cứu ngay. "
                "Không nên chờ chatbot trả lời hoặc tự xử trí tại nhà.\n\n"
                f"{fixed}"
            )
        if retrieved_contexts and "nguồn nội bộ" not in fixed.lower():
            sources = self._format_sources(retrieved_contexts)
            fixed = f"{fixed.rstrip()}\n\nNguồn nội bộ đã dùng: {sources}"
        return self._ensure_disclaimer(fixed)

    def _ensure_disclaimer(self, answer: str) -> str:
        if MEDICAL_DISCLAIMER.lower() in answer.lower():
            return answer
        return f"{answer.rstrip()}\n\n{MEDICAL_DISCLAIMER}"

    def _context_domains(self, retrieved_contexts: list[dict[str, Any]]) -> set[str]:
        domains: set[str] = set()
        for item in retrieved_contexts:
            metadata = item.get("metadata") or {}
            domain = str(item.get("domain") or metadata.get("domain") or "").strip()
            if domain:
                domains.add(domain)
        return domains

    def _format_sources(self, retrieved_contexts: list[dict[str, Any]]) -> str:
        labels = []
        for item in retrieved_contexts:
            metadata = item.get("metadata") or {}
            label = str(metadata.get("topic") or item.get("id") or "unknown")
            if label not in labels:
                labels.append(label)
        return ", ".join(labels) if labels else "(không rõ topic)"
