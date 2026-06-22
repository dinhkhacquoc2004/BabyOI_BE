from __future__ import annotations

import re
import unicodedata
from typing import Any

from src.constants import (
    DOMAIN_GENERAL,
    DOMAIN_GROWTH,
    DOMAIN_NUTRITION,
    DOMAIN_ROUTINE,
    DOMAIN_VACCINATION,
    GENERAL_SAFETY_KEYWORDS,
    GROWTH_KEYWORDS,
    NUTRITION_KEYWORDS,
    RED_FLAG_KEYWORDS,
    ROUTINE_KEYWORDS,
    SUPPORTED_DOMAINS,
    VACCINATION_KEYWORDS,
)
from src.gemini_client import GeminiClient
from src.utils.json_utils import ensure_list, parse_json_object


VALID_INTENTS = {
    "nutrition",
    "growth",
    "vaccination",
    "routine",
    "symptom",
    "general_care",
    "out_of_scope",
}

OUT_OF_SCOPE_KEYWORDS = [
    "chứng khoán",
    "đầu tư",
    "crypto",
    "bitcoin",
    "bất động sản",
    "lập trình",
    "du lịch",
    "game",
]

SYMPTOM_KEYWORDS = [
    "ốm",
    "bệnh",
    "sốt",
    "ho",
    "sổ mũi",
    "nghẹt mũi",
    "tiêu chảy",
    "nôn",
    "ói",
    "trớ",
    "phát ban",
    "mẩn đỏ",
    "khó thở",
    "thở nhanh",
    "co giật",
    "li bì",
    "lừ đừ",
    "bỏ bú",
    "bú kém",
    "mất nước",
    "phân máu",
    "đau tai",
    "chảy mủ tai",
    "đỏ mắt",
    "hăm tã",
    "vàng da",
    "rốn đỏ",
]

NUTRITION_SEARCH_KEYWORDS = [
    "an",
    "an dam",
    "sua",
    "bu",
    "dinh duong",
    "dinh duong cho be",
    "mon",
    "mon an",
    "vai mon",
    "goi y mon",
    "chao",
    "bot",
    "trung",
    "thuc don",
    "khau phan",
    "calo",
    "kcal",
    "nang luong",
    "chat dam",
    "protein",
    "tinh bot",
    "chat beo",
    "vitamin",
    "khoang chat",
    "tdee",
    "bmr",
    "bmi",
    "bieng an",
    "tang can",
    "giam can",
]

GROWTH_SEARCH_KEYWORDS = [
    "can nang",
    "nang",
    "chieu cao",
    "cao",
    "cham tang can",
    "tang truong",
    "phat trien",
    "nhe can",
]

VACCINATION_SEARCH_KEYWORDS = [
    "vaccine",
    "vacxin",
    "vac xin",
    "tiem",
    "tiem chung",
    "lich tiem",
    "mui tiem",
]

ROUTINE_SEARCH_KEYWORDS = [
    "lich sinh hoat",
    "sinh hoat",
    "nep sinh hoat",
    "lich ngu",
    "gio ngu",
    "ngu ngay",
    "ngu dem",
    "giac ngu",
    "thuc giac",
    "cu bu",
    "gio an",
    "tam",
    "choi",
    "van dong",
    "man hinh",
    "routine",
    "schedule",
    "sleep",
    "nap",
    "bedtime",
]

SYMPTOM_SEARCH_KEYWORDS = [
    "om",
    "be om",
    "benh",
    "benh thuong gap",
    "sot",
    "ho",
    "so mui",
    "nghet mui",
    "viem phoi",
    "tho nhanh",
    "tieu chay",
    "non",
    "oi",
    "tro",
    "phat ban",
    "man do",
    "kho tho",
    "co giat",
    "li bi",
    "lu du",
    "bo bu",
    "bu kem",
    "mat nuoc",
    "phan mau",
    "dau tai",
    "chay mu tai",
    "do mat",
    "ham ta",
    "vang da",
    "ron do",
]

OUT_OF_SCOPE_SEARCH_KEYWORDS = [
    "chung khoan",
    "dau tu",
    "crypto",
    "bitcoin",
    "bat dong san",
    "lap trinh",
    "du lich",
    "game",
]


class QueryUnderstandingAgent:
    """Unifies input cleaning, intent routing, and domain planning in one Gemini call."""

    def __init__(self, gemini_client: GeminiClient):
        self.gemini_client = gemini_client

    def understand(self, user_question: str) -> dict[str, Any]:
        fallback = self._fallback(user_question)
        if fallback["intent"] in {DOMAIN_NUTRITION, DOMAIN_ROUTINE}:
            fallback["reasoning_summary"] = (
                f"Rule-based {fallback['intent']} routing to reduce model calls."
            )
            return fallback
        try:
            raw = self.gemini_client.generate_text(self._build_prompt(user_question))
            parsed = parse_json_object(raw)
            return self._normalize(parsed, user_question, source="gemini")
        except Exception as exc:
            result = fallback
            result["reasoning_summary"] = (
                "Fallback rule-based vì Gemini trả JSON lỗi/thiếu field hoặc gọi Gemini lỗi."
            )
            result["fallback_reason"] = str(exc)
            return result

    def fallback_understanding(self, user_question: str) -> dict[str, Any]:
        return self._fallback(user_question)

    def _build_prompt(self, user_question: str) -> str:
        return f"""
Bạn là QueryUnderstandingAgent cho chatbot y khoa mẹ & bé.
Nhiệm vụ: làm sạch input, phân loại intent, chọn domain retrieval. Không tư vấn y khoa.

Intent hợp lệ:
- nutrition
- growth
- vaccination
- symptom
- general_care
- out_of_scope

Domain retrieval hợp lệ:
- nutrition
- growth
- vaccination
- general

Luật:
- Nếu ngoài phạm vi mẹ & bé/y tế trẻ nhỏ, out_of_scope=true, intent="out_of_scope", retrieval_required=false.
- Nếu hỏi ăn dặm, sữa, bú, dinh dưỡng: ưu tiên nutrition.
- Nếu hỏi cân nặng, chiều cao, chậm tăng cân, phát triển: ưu tiên growth.
- Nếu hỏi vaccine, tiêm chủng, lịch tiêm: ưu tiên vaccination.
- Nếu hỏi sốt, ho, tiêu chảy, nôn, phát ban hoặc triệu chứng: intent="symptom", include domain general.
- Với câu hỏi nhiều mảng, candidate_domains có thể gồm nhiều domain, tối đa 4.
- Nếu thiếu thông tin quan trọng, liệt kê trong missing_critical_info.
- mentioned_symptoms chỉ lấy từ câu hỏi user, không lấy từ kiến thức nền, suy luận, context, hoặc ví dụ.
- Nếu user chỉ hỏi dinh dưỡng, bú, sữa, ăn dặm và không nêu triệu chứng thì mentioned_symptoms phải là [].
- Câu single-domain nutrition không có triệu chứng phải trả candidate_domains=["nutrition"], không tự thêm "general".
- Chỉ thêm "general" khi user thật sự nêu triệu chứng hoặc câu hỏi là chăm sóc/y tế tổng quát.

Trả về JSON thuần, không markdown, đúng schema:
{{
  "original_input": "câu hỏi gốc",
  "cleaned_input": "câu hỏi đã chuẩn hóa",
  "language": "vi",
  "child_age_months": null,
  "mentioned_symptoms": [],
  "mentioned_conditions": [],
  "missing_critical_info": [],
  "intent": "general_care",
  "primary_domain": "general",
  "candidate_domains": ["general"],
  "retrieval_required": true,
  "out_of_scope": false,
  "confidence": 0.7,
  "reasoning_summary": "Tóm tắt rất ngắn lý do phân loại."
}}

Vi du bat buoc:
Input: "dinh duong cho be tu 1 den 3 thang tuoi"
Expected JSON fields:
{{
  "intent": "nutrition",
  "primary_domain": "nutrition",
  "candidate_domains": ["nutrition"],
  "mentioned_symptoms": [],
  "child_age_months": 3,
  "missing_critical_info": []
}}

Câu hỏi: {user_question}
""".strip()

    def _normalize(self, parsed: dict[str, Any], user_question: str, source: str) -> dict[str, Any]:
        fallback = self._fallback(user_question)
        cleaned = str(parsed.get("cleaned_input") or parsed.get("cleaned_question") or fallback["cleaned_input"]).strip()
        intent = str(parsed.get("intent") or fallback["intent"]).strip()
        if intent not in VALID_INTENTS:
            intent = fallback["intent"]

        out_of_scope = bool(parsed.get("out_of_scope", intent == "out_of_scope"))
        if out_of_scope:
            intent = "out_of_scope"

        candidate_domains = [
            str(domain).strip()
            for domain in ensure_list(parsed.get("candidate_domains"))
            if str(domain).strip() in SUPPORTED_DOMAINS
        ]
        for domain in fallback["candidate_domains"]:
            if domain not in candidate_domains:
                candidate_domains.append(domain)
        candidate_domains = self._dedupe_domains(candidate_domains) or [DOMAIN_GENERAL]

        primary_domain = str(parsed.get("primary_domain") or fallback["primary_domain"]).strip()
        if primary_domain not in candidate_domains:
            primary_domain = candidate_domains[0]

        confidence = self._confidence(parsed.get("confidence"), fallback["confidence"])
        child_age_months = self._to_float_or_none(parsed.get("child_age_months"))
        if child_age_months is None:
            child_age_months = fallback["child_age_months"]
        symptoms = self._filter_symptoms_from_user(
            self._string_list(parsed.get("mentioned_symptoms")),
            user_question,
        ) or fallback["mentioned_symptoms"]
        conditions = self._string_list(parsed.get("mentioned_conditions"))
        missing = self._string_list(parsed.get("missing_critical_info")) or fallback["missing_critical_info"]

        if out_of_scope:
            candidate_domains = [DOMAIN_GENERAL]
            primary_domain = DOMAIN_GENERAL
        else:
            candidate_domains = self._tighten_candidate_domains(
                candidate_domains,
                fallback["candidate_domains"],
                symptoms,
                primary_domain,
                intent,
            )
            if primary_domain not in candidate_domains:
                primary_domain = candidate_domains[0]

        return {
            "original_input": user_question,
            "cleaned_input": cleaned or user_question.strip(),
            "language": str(parsed.get("language") or "vi").strip() or "vi",
            "child_age_months": child_age_months,
            "mentioned_symptoms": symptoms,
            "mentioned_conditions": conditions,
            "missing_critical_info": self._merge_missing(missing, intent, child_age_months),
            "intent": intent,
            "primary_domain": primary_domain,
            "candidate_domains": candidate_domains[:4],
            "retrieval_required": bool(parsed.get("retrieval_required", intent != "out_of_scope")),
            "out_of_scope": out_of_scope,
            "confidence": confidence,
            "reasoning_summary": str(parsed.get("reasoning_summary") or fallback["reasoning_summary"]),
            "source": source,
        }

    def _fallback(self, user_question: str) -> dict[str, Any]:
        cleaned = " ".join(user_question.strip().split())
        current_question = self._current_question_text(user_question)
        lowered = current_question.lower()
        search_text = self._normalize_search(current_question)
        full_search_text = self._normalize_search(user_question)
        profile_age_months = self._extract_number(
            full_search_text,
            r"(?:^|\s|-)agemonths\s*:\s*(\d+(?:[,.]\d+)?)",
        )
        child_age_months = (
            profile_age_months
            if profile_age_months is not None
            else self._extract_age_months(search_text)
        )
        symptoms = [
            keyword
            for keyword in SYMPTOM_KEYWORDS
            if self._contains_search_keyword(search_text, self._normalize_search(keyword))
        ]
        symptoms.extend(
            keyword
            for keyword in SYMPTOM_SEARCH_KEYWORDS
            if self._contains_search_keyword(search_text, keyword) and keyword not in symptoms
        )

        if any(self._contains_search_keyword(search_text, keyword) for keyword in OUT_OF_SCOPE_SEARCH_KEYWORDS) or any(
            keyword in lowered for keyword in OUT_OF_SCOPE_KEYWORDS
        ):
            intent = "out_of_scope"
        elif any(self._contains_search_keyword(search_text, keyword) for keyword in VACCINATION_SEARCH_KEYWORDS) or self._looks_like_vaccination(search_text) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in VACCINATION_KEYWORDS
        ):
            intent = "vaccination"
        elif any(self._contains_search_keyword(search_text, keyword) for keyword in GROWTH_SEARCH_KEYWORDS) or self._looks_like_growth(search_text) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in GROWTH_KEYWORDS
        ):
            intent = "growth"
        elif any(
            self._contains_search_keyword(search_text, keyword)
            for keyword in ROUTINE_SEARCH_KEYWORDS
        ) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in ROUTINE_KEYWORDS
        ):
            intent = "routine"
        elif (
            any(self._contains_search_keyword(search_text, keyword) for keyword in NUTRITION_SEARCH_KEYWORDS)
            or self._looks_like_nutrition(search_text)
            or self._is_nutrition_follow_up(user_question, search_text)
            or any(
                self._contains_search_keyword(search_text, self._normalize_search(keyword))
                for keyword in NUTRITION_KEYWORDS
            )
        ):
            intent = "nutrition"
        elif symptoms or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in GENERAL_SAFETY_KEYWORDS
        ):
            intent = "symptom"
        else:
            intent = "general_care"

        domains = self._domains_for_text(lowered, intent)
        if intent == "out_of_scope":
            domains = [DOMAIN_GENERAL]

        return {
            "original_input": user_question,
            "cleaned_input": " ".join(current_question.strip().split()),
            "language": "vi",
            "child_age_months": child_age_months,
            "mentioned_symptoms": symptoms,
            "mentioned_conditions": [],
            "missing_critical_info": self._merge_missing([], intent, child_age_months),
            "intent": intent,
            "primary_domain": domains[0],
            "candidate_domains": domains,
            "retrieval_required": intent != "out_of_scope",
            "out_of_scope": intent == "out_of_scope",
            "confidence": 0.58,
            "reasoning_summary": "Fallback rule-based query understanding.",
            "source": "fallback",
        }

    def to_cleaned_input(self, result: dict[str, Any]) -> dict[str, Any]:
        text = str(result.get("cleaned_input") or result.get("original_input") or "")
        symptoms = self._string_list(result.get("mentioned_symptoms"))
        missing = self._string_list(result.get("missing_critical_info"))
        age_months = result.get("child_age_months")
        original_input = str(result.get("original_input") or "")
        profile_type = self._extract_profile_value(original_input, "profileType")
        food_age_group = self._extract_profile_value(original_input, "foodAgeGroup")
        profile_food_catalog = self._extract_profile_value(original_input, "profileFoodCatalog")
        profile_routine_today = self._extract_profile_value(original_input, "profileRoutineToday")
        profile_id = self._extract_profile_value(original_input, "id")
        profile_age_raw = self._extract_profile_value(original_input, "ageMonths")
        profile_age_months = self._to_float_or_none(profile_age_raw)
        if profile_age_months is None:
            profile_age_months = age_months

        normalized_profile_type = (profile_type or "").strip().upper()
        is_mother = normalized_profile_type in {"MOTHER", "MOM", "M", "MẸ"}
        patient_type = "mother" if is_mother else "baby"
        subject_label = "mẹ" if is_mother else "bé"

        current_question = self._current_question_text(original_input)
        normalized_question = self._normalize_search(current_question)
        goal_code, goal_label = self._extract_goal(normalized_question, is_mother)

        if isinstance(profile_age_months, (int, float)) and not is_mother:
            retrieval_query = f"{text} Bé {profile_age_months:g} tháng"
        else:
            retrieval_query = text
        if goal_label:
            retrieval_query = f"{retrieval_query} (mục tiêu: {goal_label})"

        return {
            "original_question": result.get("original_input"),
            "cleaned_question": text,
            "language": result.get("language") or "vi",
            "user_type": "parent",
            "patient_type": patient_type,
            "subject_label": subject_label,
            "profile_id": profile_id,
            "profile_type": profile_type,
            "profile_age_months": profile_age_months,
            "food_age_group": food_age_group,
            "profile_food_catalog": profile_food_catalog,
            "profile_routine_today": profile_routine_today,
            "goal_code": goal_code,
            "goal_label": goal_label,
            "child_age_months": result.get("child_age_months") if not is_mother else None,
            "child_gender": None,
            "weight_kg": self._extract_number(text.lower(), r"(\d+(?:[,.]\d+)?)\s*kg"),
            "height_cm": self._extract_number(text.lower(), r"(\d+(?:[,.]\d+)?)\s*cm"),
            "gestational_week": None,
            "main_symptoms": symptoms,
            "symptom_duration": self._extract_duration(text.lower()),
            "temperature_c": self._extract_number(text.lower(), r"(\d+(?:[,.]\d+)?)\s*(?:độ|°c|c)"),
            "feeding_status": self._extract_status(text.lower(), ["bú kém", "bỏ bú", "ăn kém", "bú tốt"]),
            "stool_status": self._extract_status(text.lower(), ["tiêu chảy", "táo bón", "phân có máu"]),
            "vomit_status": self._extract_status(text.lower(), ["nôn", "ói", "trớ"]),
            "allergies": [],
            "medical_history": self._string_list(result.get("mentioned_conditions")),
            "medications_used": [],
            "vaccination_info": text if result.get("intent") == "vaccination" else None,
            "nutrition_info": text if DOMAIN_NUTRITION in result.get("candidate_domains", []) else None,
            "growth_info": text if DOMAIN_GROWTH in result.get("candidate_domains", []) else None,
            "red_flag_terms": [keyword for keyword in RED_FLAG_KEYWORDS if keyword in text.lower()],
            "missing_important_fields": missing,
            "normalized_query_for_retrieval": retrieval_query,
        }

    def to_intent_result(self, result: dict[str, Any]) -> dict[str, Any]:
        intent = str(result.get("intent") or "general_care")
        return {
            "intent": intent,
            "confidence": result.get("confidence", 0.5),
            "reason": result.get("reasoning_summary", ""),
            "need_retrieval": bool(result.get("retrieval_required", intent != "out_of_scope")),
        }

    def to_routing_result(self, result: dict[str, Any]) -> dict[str, Any]:
        return {
            "primary_domain": result.get("primary_domain") or DOMAIN_GENERAL,
            "candidate_domains": result.get("candidate_domains") or [DOMAIN_GENERAL],
            "reason": result.get("reasoning_summary", ""),
            "confidence": result.get("confidence", 0.5),
        }

    def _domains_for_text(self, text: str, intent: str) -> list[str]:
        search_text = self._normalize_search(text)
        domains: list[str] = []
        if intent == "nutrition":
            domains.append(DOMAIN_NUTRITION)
        elif intent == "growth":
            domains.append(DOMAIN_GROWTH)
        elif intent == "vaccination":
            domains.append(DOMAIN_VACCINATION)
        elif intent == "routine":
            domains.append(DOMAIN_ROUTINE)
        elif intent in {"symptom", "general_care"}:
            domains.append(DOMAIN_GENERAL)

        if any(self._contains_search_keyword(search_text, keyword) for keyword in GROWTH_SEARCH_KEYWORDS) or self._looks_like_growth(search_text) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in GROWTH_KEYWORDS
        ):
            domains.append(DOMAIN_GROWTH)
        if any(self._contains_search_keyword(search_text, keyword) for keyword in NUTRITION_SEARCH_KEYWORDS) or self._looks_like_nutrition(search_text) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in NUTRITION_KEYWORDS
        ):
            domains.append(DOMAIN_NUTRITION)
        if any(self._contains_search_keyword(search_text, keyword) for keyword in VACCINATION_SEARCH_KEYWORDS) or self._looks_like_vaccination(search_text) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in VACCINATION_KEYWORDS
        ):
            domains.append(DOMAIN_VACCINATION)
        if any(
            self._contains_search_keyword(search_text, keyword)
            for keyword in ROUTINE_SEARCH_KEYWORDS
        ) or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in ROUTINE_KEYWORDS
        ):
            domains.append(DOMAIN_ROUTINE)
        if (
            any(self._contains_search_keyword(search_text, keyword) for keyword in SYMPTOM_SEARCH_KEYWORDS)
            or any(
                self._contains_search_keyword(search_text, self._normalize_search(keyword))
                for keyword in GENERAL_SAFETY_KEYWORDS
            )
            or intent in {"symptom", "general_care"}
        ):
            domains.append(DOMAIN_GENERAL)

        return self._dedupe_domains(domains) or [DOMAIN_GENERAL]

    def _merge_missing(self, missing: list[str], intent: str, child_age_months: Any) -> list[str]:
        values = set(missing)
        if child_age_months is not None:
            values.discard("child_age_months")
        if intent in {"nutrition", "growth", "vaccination", "routine", "symptom"} and child_age_months is None:
            values.add("child_age_months")
        if intent == "growth":
            values.update(["weight_kg", "height_cm"])
        if intent == "symptom":
            values.update(["symptom_duration", "temperature_c", "feeding_status"])
        return sorted(value for value in values if value)

    def _dedupe_domains(self, domains: list[str]) -> list[str]:
        result: list[str] = []
        for domain in domains:
            if domain in SUPPORTED_DOMAINS and domain not in result:
                result.append(domain)
        return result

    def _string_list(self, value: Any) -> list[str]:
        return [str(item).strip() for item in ensure_list(value) if str(item).strip()]

    def _confidence(self, value: Any, fallback: float) -> float:
        try:
            return max(0.0, min(float(value), 1.0))
        except (TypeError, ValueError):
            return fallback

    def _to_float_or_none(self, value: Any) -> float | None:
        if value in (None, ""):
            return None
        try:
            return float(str(value).replace(",", "."))
        except (TypeError, ValueError):
            return None

    def _extract_age_months(self, text: str) -> float | None:
        range_match = re.search(
            r"(\d+(?:[,.]\d+)?)\s*(?:-|den|toi|tới|đến)\s*(\d+(?:[,.]\d+)?)\s*th.ng",
            text,
        )
        if range_match:
            try:
                return max(
                    float(range_match.group(1).replace(",", ".")),
                    float(range_match.group(2).replace(",", ".")),
                )
            except ValueError:
                pass

        month = self._extract_number(text, r"(\d+(?:[,.]\d+)?)\s*th.ng")
        if month is not None:
            return month
        year = self._extract_number(text, r"(\d+(?:[,.]\d+)?)\s*tu.i")
        return year * 12 if year is not None else None

    def _normalize_search(self, text: str) -> str:
        normalized = unicodedata.normalize("NFD", text.lower())
        without_marks = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
        return without_marks.replace("đ", "d")

    def _contains_search_keyword(self, text: str, keyword: str) -> bool:
        pattern = r"(?<![a-z0-9])" + re.escape(keyword) + r"(?![a-z0-9])"
        return re.search(pattern, text) is not None

    def _filter_symptoms_from_user(self, symptoms: list[str], user_question: str) -> list[str]:
        search_text = self._normalize_search(self._current_question_text(user_question))
        result: list[str] = []
        for symptom in symptoms:
            normalized = self._normalize_search(symptom)
            if normalized and self._contains_search_keyword(search_text, normalized):
                result.append(symptom)
        return result

    def _current_question_text(self, text: str) -> str:
        for marker in ("[Câu hỏi hiện tại]", "[Cau hoi hien tai]"):
            if marker in text:
                return text.rsplit(marker, 1)[1].strip()
        return text.strip()

    def _is_nutrition_follow_up(self, full_text: str, current_search_text: str) -> bool:
        if not re.search(r"\d+(?:[,.]\d+)?\s*thang", current_search_text):
            return False
        history_text = full_text.split("[Câu hỏi hiện tại]", 1)[0]
        normalized_history = self._normalize_search(history_text)
        return self._looks_like_nutrition(normalized_history) or any(
            self._contains_search_keyword(normalized_history, keyword)
            for keyword in NUTRITION_SEARCH_KEYWORDS
        )

    def _extract_profile_value(self, text: str, key: str) -> str | None:
        match = re.search(
            rf"(?im)^\s*-\s*{re.escape(key)}\s*:\s*(.+?)\s*$",
            text,
        )
        return match.group(1).strip() if match else None

    # Map common Vietnamese goal phrases (already normalized: no diacritics, lowercase)
    # to (goal_code, human label). Order matters: most specific phrases first.
    _MOTHER_GOAL_PATTERNS: tuple[tuple[str, str, str], ...] = (
        ("tang sua", "FOR_MOTHER_INCREASE_MILK_SUPPLY", "tăng tiết sữa"),
        ("loi sua", "FOR_MOTHER_INCREASE_MILK_SUPPLY", "lợi sữa"),
        ("nhieu sua", "FOR_MOTHER_INCREASE_MILK_SUPPLY", "tăng tiết sữa"),
        ("sau sinh", "FOR_MOTHER_POSTPARTUM_BREASTFEEDING", "sau sinh, đang cho con bú"),
        ("cho con bu", "FOR_MOTHER_POSTPARTUM_BREASTFEEDING", "đang cho con bú"),
        ("giam can", "FOR_MOTHER_DIET", "giảm cân"),
        ("lay lai voc dang", "FOR_MOTHER_DIET", "lấy lại vóc dáng"),
        ("an kieng", "FOR_MOTHER_DIET", "ăn kiêng"),
        ("tieu hoa", "FOR_MOTHER_DIGESTION_RECOVERY", "hỗ trợ tiêu hóa"),
        ("tao bon", "FOR_MOTHER_DIGESTION_RECOVERY", "hỗ trợ tiêu hóa"),
        ("nang luong", "FOR_MOTHER_HEALTHY_ENERGY", "tăng năng lượng"),
        ("tang can", "FOR_MOTHER_HEALTHY_ENERGY", "tăng cân lành mạnh"),
        ("met moi", "FOR_MOTHER_HEALTHY_ENERGY", "tăng năng lượng"),
        ("doi mon", "FOR_MOTHER_CHANGE_DIET", "đổi món"),
        ("an ngon", "FOR_MOTHER_CHANGE_DIET", "đa dạng món"),
        ("ngu", "FOR_MOTHER_SLEEP_STRESS_SUPPORT", "hỗ trợ giấc ngủ/giảm stress"),
        ("stress", "FOR_MOTHER_SLEEP_STRESS_SUPPORT", "hỗ trợ giấc ngủ/giảm stress"),
        ("cang thang", "FOR_MOTHER_SLEEP_STRESS_SUPPORT", "hỗ trợ giấc ngủ/giảm stress"),
    )

    def _extract_goal(self, normalized_question: str, is_mother: bool) -> tuple[str | None, str | None]:
        if not normalized_question:
            return None, None
        if is_mother:
            for keyword, code, label in self._MOTHER_GOAL_PATTERNS:
                if keyword in normalized_question:
                    return code, label
        return None, None

    def _tighten_candidate_domains(
        self,
        parsed_domains: list[str],
        fallback_domains: list[str],
        symptoms: list[str],
        primary_domain: str,
        intent: str,
    ) -> list[str]:
        domains = self._dedupe_domains(parsed_domains + fallback_domains)
        specialized = [
            domain
            for domain in domains
            if domain in {DOMAIN_NUTRITION, DOMAIN_GROWTH, DOMAIN_VACCINATION, DOMAIN_ROUTINE}
        ]
        if not symptoms and len(specialized) == 1:
            return specialized
        if not symptoms and intent in {DOMAIN_NUTRITION, DOMAIN_GROWTH, DOMAIN_VACCINATION, DOMAIN_ROUTINE}:
            related = [
                domain
                for domain in specialized
                if domain in fallback_domains or domain == primary_domain
            ]
            return related or [intent]
        return domains or [DOMAIN_GENERAL]

    def _looks_like_growth(self, text: str) -> bool:
        return bool(re.search(r"ch.m\s+t.ng\s+c.n|c.n\s+n.ng|chi.u\s+cao|ph.t\s+tri.n", text))

    def _looks_like_nutrition(self, text: str) -> bool:
        return bool(
            re.search(
                r"(?<![a-z0-9])(?:an|bu|sua)(?![a-z0-9])|an\s+dam|dinh\s+duong",
                text,
            )
        )

    def _looks_like_vaccination(self, text: str) -> bool:
        return bool(re.search(r"vaccine|vacxin|v.c\s*xin|ti.m|l.ch\s+ti.m", text))

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

    def _extract_status(self, text: str, keywords: list[str]) -> str | None:
        found = [keyword for keyword in keywords if keyword in text]
        return ", ".join(found) if found else None
