from __future__ import annotations

import re
from typing import Any

from src.constants import RED_FLAG_KEYWORDS
from src.gemini_client import GeminiClient
from src.utils.json_utils import ensure_list, parse_json_object


REQUIRED_KEYS = {
    "original_question",
    "cleaned_question",
    "language",
    "user_type",
    "patient_type",
    "child_age_months",
    "child_gender",
    "weight_kg",
    "height_cm",
    "gestational_week",
    "main_symptoms",
    "symptom_duration",
    "temperature_c",
    "feeding_status",
    "stool_status",
    "vomit_status",
    "allergies",
    "medical_history",
    "medications_used",
    "vaccination_info",
    "nutrition_info",
    "growth_info",
    "red_flag_terms",
    "missing_important_fields",
    "normalized_query_for_retrieval",
}


class InputCleanerAgent:
    def __init__(self, gemini_client: GeminiClient):
        self.gemini_client = gemini_client

    def clean(self, user_question: str) -> dict[str, Any]:
        try:
            raw = self.gemini_client.generate_text(self._build_prompt(user_question))
            parsed = parse_json_object(raw)
            return self._normalize(parsed, user_question)
        except Exception:
            return self._fallback(user_question)

    def _build_prompt(self, user_question: str) -> str:
        return f"""
Bạn là Input Cleaner Agent cho chatbot y khoa mẹ & bé.
Nhiệm vụ: chuẩn hóa câu hỏi thành structured JSON. Không tư vấn y khoa, không chẩn đoán, không kê thuốc.

Trả về JSON thuần, không markdown, đúng các key sau:
{{
  "original_question": "...",
  "cleaned_question": "...",
  "language": "vi",
  "user_type": "parent | pregnant_mother | unknown",
  "patient_type": "baby | child | mother | unknown",
  "child_age_months": null,
  "child_gender": null,
  "weight_kg": null,
  "height_cm": null,
  "gestational_week": null,
  "main_symptoms": [],
  "symptom_duration": null,
  "temperature_c": null,
  "feeding_status": null,
  "stool_status": null,
  "vomit_status": null,
  "allergies": [],
  "medical_history": [],
  "medications_used": [],
  "vaccination_info": null,
  "nutrition_info": null,
  "growth_info": null,
  "red_flag_terms": [],
  "missing_important_fields": [],
  "normalized_query_for_retrieval": "..."
}}

Nếu câu hỏi chung chung như "bé bị ho thì sao", missing_important_fields phải gồm:
child_age_months, symptom_duration, temperature_c, breathing_status, feeding_status.

Câu hỏi: {user_question}
""".strip()

    def _normalize(self, parsed: dict[str, Any], user_question: str) -> dict[str, Any]:
        fallback = self._fallback(user_question)
        result = {key: parsed.get(key, fallback.get(key)) for key in REQUIRED_KEYS}
        result["original_question"] = user_question
        result["cleaned_question"] = str(result.get("cleaned_question") or user_question).strip()
        result["language"] = str(result.get("language") or "vi").strip() or "vi"
        result["user_type"] = self._allowed(result.get("user_type"), {"parent", "pregnant_mother", "unknown"})
        result["patient_type"] = self._allowed(result.get("patient_type"), {"baby", "child", "mother", "unknown"})
        result["child_age_months"] = self._to_float_or_none(result.get("child_age_months"))
        result["weight_kg"] = self._to_float_or_none(result.get("weight_kg"))
        result["height_cm"] = self._to_float_or_none(result.get("height_cm"))
        result["gestational_week"] = self._to_float_or_none(result.get("gestational_week"))
        result["temperature_c"] = self._to_float_or_none(result.get("temperature_c"))

        for key in [
            "main_symptoms",
            "allergies",
            "medical_history",
            "medications_used",
            "red_flag_terms",
            "missing_important_fields",
        ]:
            result[key] = [str(item) for item in ensure_list(result.get(key)) if str(item).strip()]

        if not result.get("normalized_query_for_retrieval"):
            result["normalized_query_for_retrieval"] = result["cleaned_question"]

        result["red_flag_terms"] = sorted(
            set(result["red_flag_terms"]) | set(self._find_red_flags(user_question))
        )
        result["missing_important_fields"] = self._merge_missing_fields(result)
        return result

    def _fallback(self, user_question: str) -> dict[str, Any]:
        text = user_question.strip()
        lowered = text.lower()
        main_symptoms = self._extract_symptoms(lowered)
        child_age_months = self._extract_age_months(lowered)

        result: dict[str, Any] = {
            "original_question": user_question,
            "cleaned_question": " ".join(text.split()),
            "language": "vi",
            "user_type": "pregnant_mother" if "mang thai" in lowered or "thai" in lowered else "parent",
            "patient_type": "mother" if "mẹ" in lowered and "bé" not in lowered else "baby",
            "child_age_months": child_age_months,
            "child_gender": self._extract_gender(lowered),
            "weight_kg": self._extract_number(lowered, r"(\d+(?:[,.]\d+)?)\s*kg"),
            "height_cm": self._extract_number(lowered, r"(\d+(?:[,.]\d+)?)\s*cm"),
            "gestational_week": self._extract_number(lowered, r"(\d+(?:[,.]\d+)?)\s*tuần"),
            "main_symptoms": main_symptoms,
            "symptom_duration": self._extract_duration(lowered),
            "temperature_c": self._extract_number(lowered, r"(\d+(?:[,.]\d+)?)\s*(?:độ|°c|c)"),
            "feeding_status": self._extract_status(lowered, ["bú kém", "bỏ bú", "ăn kém", "bú tốt"]),
            "stool_status": self._extract_status(lowered, ["tiêu chảy", "táo bón", "phân có máu"]),
            "vomit_status": self._extract_status(lowered, ["nôn", "ói", "trớ"]),
            "allergies": self._extract_list_after_keywords(lowered, ["dị ứng"]),
            "medical_history": [],
            "medications_used": self._extract_list_after_keywords(lowered, ["đã uống", "đang uống", "thuốc"]),
            "vaccination_info": text if any(word in lowered for word in ["tiêm", "vắc xin", "vacxin", "vaccine"]) else None,
            "nutrition_info": text if any(word in lowered for word in ["ăn", "sữa", "bú", "dặm", "trứng"]) else None,
            "growth_info": text if any(word in lowered for word in ["nặng", "cao", "cân", "tăng trưởng"]) else None,
            "red_flag_terms": self._find_red_flags(lowered),
            "missing_important_fields": [],
            "normalized_query_for_retrieval": " ".join(text.split()),
        }
        result["missing_important_fields"] = self._merge_missing_fields(result)
        return result

    def _merge_missing_fields(self, result: dict[str, Any]) -> list[str]:
        missing = set(result.get("missing_important_fields") or [])
        symptoms = set(result.get("main_symptoms") or [])
        if symptoms:
            for field in [
                "child_age_months",
                "symptom_duration",
                "temperature_c",
                "breathing_status",
                "feeding_status",
            ]:
                if not result.get(field):
                    missing.add(field)
        if result.get("nutrition_info") and not result.get("child_age_months"):
            missing.add("child_age_months")
        if result.get("growth_info"):
            for field in ["child_age_months", "child_gender", "weight_kg", "height_cm"]:
                if not result.get(field):
                    missing.add(field)
        if result.get("vaccination_info") and not result.get("child_age_months"):
            missing.add("child_age_months")
        return sorted(missing)

    def _extract_symptoms(self, text: str) -> list[str]:
        symptoms = [
            "ho",
            "sốt",
            "nôn",
            "ói",
            "tiêu chảy",
            "táo bón",
            "khó thở",
            "co giật",
            "dị ứng",
            "phát ban",
            "bú kém",
        ]
        return [symptom for symptom in symptoms if symptom in text]

    def _extract_age_months(self, text: str) -> float | None:
        month = self._extract_number(text, r"(\d+(?:[,.]\d+)?)\s*tháng")
        if month is not None:
            return month
        year = self._extract_number(text, r"(\d+(?:[,.]\d+)?)\s*tuổi")
        return year * 12 if year is not None else None

    def _extract_number(self, text: str, pattern: str) -> float | None:
        match = re.search(pattern, text)
        if not match:
            return None
        try:
            return float(match.group(1).replace(",", "."))
        except ValueError:
            return None

    def _extract_duration(self, text: str) -> str | None:
        match = re.search(r"(\d+\s*(?:ngày|giờ|tuần|tháng))", text)
        return match.group(1) if match else None

    def _extract_gender(self, text: str) -> str | None:
        if "bé gái" in text or "con gái" in text:
            return "female"
        if "bé trai" in text or "con trai" in text:
            return "male"
        return None

    def _extract_status(self, text: str, keywords: list[str]) -> str | None:
        found = [keyword for keyword in keywords if keyword in text]
        return ", ".join(found) if found else None

    def _extract_list_after_keywords(self, text: str, keywords: list[str]) -> list[str]:
        return [keyword for keyword in keywords if keyword in text]

    def _find_red_flags(self, text: str) -> list[str]:
        lowered = text.lower()
        return [keyword for keyword in RED_FLAG_KEYWORDS if keyword in lowered]

    def _to_float_or_none(self, value: Any) -> float | None:
        if value in (None, ""):
            return None
        try:
            return float(str(value).replace(",", "."))
        except (TypeError, ValueError):
            return None

    def _allowed(self, value: Any, allowed_values: set[str]) -> str:
        normalized = str(value or "unknown").strip()
        return normalized if normalized in allowed_values else "unknown"
