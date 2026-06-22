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
- routine
- symptom
- general_care
- out_of_scope

Domain retrieval hợp lệ:
- nutrition
- growth
- vaccination
- routine
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
- Với câu hỏi dinh dưỡng/món ăn, hiểu ý nghĩa tự nhiên thay vì chỉ dò từ khóa. Trích xuất nguyên liệu, tên món và mục tiêu mẹ nếu có.
- nutrition_goal_code chỉ được là một trong: FOR_MOTHER_DIET, FOR_MOTHER_CHANGE_DIET, FOR_MOTHER_POSTPARTUM_BREASTFEEDING, FOR_MOTHER_HEALTHY_ENERGY, FOR_MOTHER_DIGESTION_RECOVERY, FOR_MOTHER_INCREASE_MILK_SUPPLY, FOR_MOTHER_SLEEP_STRESS_SUPPORT hoặc null.
- Nếu câu hiện tại dùng "món đó/nó/món vừa nói" thì đọc lịch sử hội thoại để điền requested_food_name và refers_to_previous_food=true.
- Câu hỏi hiện tại luôn ưu tiên hơn lịch sử. Không mang goal, domain, triệu chứng hoặc chủ thể cũ sang khi user đã hỏi chủ đề mới rõ ràng.
- explicit_subject là "mother" nếu user hỏi rõ về mẹ/bản thân mẹ, "baby" nếu hỏi rõ về bé/con/trẻ, còn không rõ thì "selected_profile". Profile đang chọn chỉ là mặc định, không khóa chủ thể câu hỏi.

Trả về JSON thuần, không markdown, đúng schema:
{{
  "original_input": "câu hỏi gốc",
  "cleaned_input": "câu hỏi đã chuẩn hóa",
  "language": "vi",
  "child_age_months": null,
  "mentioned_symptoms": [],
  "mentioned_conditions": [],
  "nutrition_goal_code": null,
  "nutrition_goal_label": null,
  "requested_ingredients": [],
  "requested_food_name": null,
  "food_analysis_requested": false,
  "refers_to_previous_food": false,
  "explicit_subject": "selected_profile",
  "request_purpose": "information",
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
        goal_code = str(parsed.get("nutrition_goal_code") or "").strip().upper() or None
        valid_goal_codes = {
            "FOR_MOTHER_DIET", "FOR_MOTHER_CHANGE_DIET", "FOR_MOTHER_POSTPARTUM_BREASTFEEDING",
            "FOR_MOTHER_HEALTHY_ENERGY", "FOR_MOTHER_DIGESTION_RECOVERY",
            "FOR_MOTHER_INCREASE_MILK_SUPPLY", "FOR_MOTHER_SLEEP_STRESS_SUPPORT",
        }
        if goal_code not in valid_goal_codes:
            goal_code = None
        requested_food_name = str(parsed.get("requested_food_name") or "").strip() or None
        fallback_subject = str(fallback.get("explicit_subject") or "selected_profile")
        explicit_subject = str(parsed.get("explicit_subject") or fallback_subject).strip().lower()
        if explicit_subject not in {"mother", "baby", "selected_profile"}:
            explicit_subject = fallback_subject
        # A subject explicitly written in the current question is stronger than
        # the model's interpretation and stronger than the selected profile.
        if fallback_subject in {"mother", "baby"}:
            explicit_subject = fallback_subject
        request_purpose = str(parsed.get("request_purpose") or "").strip().lower()
        valid_purposes = {
            "information", "recommend_food", "analyze_food", "symptom_care",
            "growth_assessment", "routine_planning", "vaccination_guidance",
            "compare_options", "follow_up",
        }
        if request_purpose not in valid_purposes:
            request_purpose = self._request_purpose(
                self._normalize_search(self._current_question_text(user_question)),
                intent,
            )
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
            "nutrition_goal_code": goal_code,
            "nutrition_goal_label": str(parsed.get("nutrition_goal_label") or "").strip() or None,
            "requested_ingredients": self._string_list(parsed.get("requested_ingredients")),
            "requested_food_name": requested_food_name,
            "food_analysis_requested": bool(parsed.get("food_analysis_requested", False)),
            "refers_to_previous_food": bool(parsed.get("refers_to_previous_food", False)),
            "explicit_subject": explicit_subject,
            "request_purpose": request_purpose,
            "missing_critical_info": self._merge_missing(
                missing, intent, child_age_months, explicit_subject
            ),
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
        profile_age_months = self._to_float_or_none(
            self._extract_profile_value(user_question, "ageMonths")
        )
        stated_age_months = self._extract_age_months(search_text)
        selected_profile_type = (
            self._extract_profile_value(user_question, "profileType") or ""
        ).strip().upper()
        explicit_subject = self._explicit_subject(search_text)
        asking_cross_profile_baby = (
            explicit_subject == "baby"
            and selected_profile_type in {"MOTHER", "MOM", "M", "MẸ"}
        )
        child_age_months = (
            stated_age_months
            if asking_cross_profile_baby and stated_age_months is not None
            else profile_age_months
            if profile_age_months is not None
            and selected_profile_type not in {"MOTHER", "MOM", "M", "MẸ"}
            else stated_age_months
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
        elif symptoms or any(
            self._contains_search_keyword(search_text, self._normalize_search(keyword))
            for keyword in GENERAL_SAFETY_KEYWORDS
        ):
            intent = "symptom"
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
            "missing_critical_info": self._merge_missing(
                [], intent, child_age_months, self._explicit_subject(search_text)
            ),
            "intent": intent,
            "primary_domain": domains[0],
            "candidate_domains": domains,
            "retrieval_required": intent != "out_of_scope",
            "out_of_scope": intent == "out_of_scope",
            "confidence": 0.58,
            "reasoning_summary": "Fallback rule-based query understanding.",
            "source": "fallback",
            "explicit_subject": explicit_subject,
            "request_purpose": self._request_purpose(search_text, intent),
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
        profile_food_catalog_index = self._extract_profile_value(original_input, "profileFoodCatalogIndex")
        mother_food_catalog_index = self._extract_profile_value(original_input, "motherFoodCatalogIndex")
        child_food_catalog_index = self._extract_profile_value(original_input, "childFoodCatalogIndex")
        profile_routine_today = self._extract_profile_value(original_input, "profileRoutineToday")
        profile_name = self._extract_profile_value(original_input, "name")
        profile_sex = self._extract_profile_value(original_input, "sex")
        health_record_date = self._extract_profile_value(original_input, "healthRecordDate")
        profile_weight_kg = self._to_float_or_none(self._extract_profile_value(original_input, "weightKg"))
        profile_height_cm = self._to_float_or_none(self._extract_profile_value(original_input, "heightCm"))
        profile_bmi = self._to_float_or_none(self._extract_profile_value(original_input, "bmi"))
        profile_bmr_kcal = self._to_float_or_none(self._extract_profile_value(original_input, "bmrKcal"))
        profile_tdee_kcal = self._to_float_or_none(self._extract_profile_value(original_input, "tdeeKcal"))
        profile_activity_level = self._extract_profile_value(original_input, "activityLevel")
        previous_health_record_date = self._extract_profile_value(original_input, "previousHealthRecordDate")
        previous_weight_kg = self._to_float_or_none(self._extract_profile_value(original_input, "previousWeightKg"))
        previous_height_cm = self._to_float_or_none(self._extract_profile_value(original_input, "previousHeightCm"))
        weight_change_kg = self._to_float_or_none(self._extract_profile_value(original_input, "weightChangeKg"))
        height_change_cm = self._to_float_or_none(self._extract_profile_value(original_input, "heightChangeCm"))
        profile_illness_history = self._extract_profile_value(original_input, "profileIllnessHistory")
        restricted_foods_raw = self._extract_profile_value(original_input, "restrictedFoods") or ""
        restricted_foods = [item.strip() for item in restricted_foods_raw.split(";;") if item.strip()]
        catalog_goal_code = self._extract_profile_value(original_input, "catalogGoalCode")
        catalog_goal_label = self._extract_profile_value(original_input, "catalogGoalLabel")
        requested_ingredients_raw = self._extract_profile_value(original_input, "requestedIngredients") or ""
        requested_ingredients = self._string_list(result.get("requested_ingredients")) or [
            item.strip() for item in requested_ingredients_raw.split(";;") if item.strip()
        ]
        requested_food_names_raw = self._extract_profile_value(original_input, "requestedFoodNames") or ""
        requested_food_names = [item.strip() for item in requested_food_names_raw.split(";;") if item.strip()]
        parsed_food_name = str(result.get("requested_food_name") or "").strip()
        if parsed_food_name and parsed_food_name not in requested_food_names:
            requested_food_names.append(parsed_food_name)
        food_analysis_requested = str(
            self._extract_profile_value(original_input, "foodAnalysisRequested") or "false"
        ).lower() == "true"
        profile_id = self._extract_profile_value(original_input, "id")
        profile_age_raw = self._extract_profile_value(original_input, "ageMonths")
        profile_age_months = self._to_float_or_none(profile_age_raw)
        if profile_age_months is None:
            profile_age_months = age_months

        selected_profile_type = profile_type
        selected_profile_name = profile_name
        normalized_profile_type = (selected_profile_type or "").strip().upper()
        selected_is_mother = normalized_profile_type in {"MOTHER", "MOM", "M", "MẸ"}
        explicit_subject = str(result.get("explicit_subject") or "selected_profile").strip().lower()
        is_mother = (
            True if explicit_subject == "mother"
            else False if explicit_subject == "baby"
            else selected_is_mother
        )
        uses_selected_profile_data = (
            explicit_subject == "selected_profile"
            or (explicit_subject == "mother" and selected_is_mother)
            or (explicit_subject == "baby" and not selected_is_mother)
        )
        if not uses_selected_profile_data:
            profile_name = None
            profile_sex = None
            profile_age_months = age_months if not is_mother else None
            health_record_date = None
            profile_weight_kg = None
            profile_height_cm = None
            profile_bmi = None
            profile_bmr_kcal = None
            profile_tdee_kcal = None
            profile_activity_level = None
            previous_health_record_date = None
            previous_weight_kg = None
            previous_height_cm = None
            weight_change_kg = None
            height_change_cm = None
            profile_illness_history = None
            food_age_group = None
            profile_food_catalog = None
            profile_food_catalog_index = (
                mother_food_catalog_index if is_mother else child_food_catalog_index
            )
            profile_routine_today = None
            restricted_foods = []
            catalog_goal_code = None
            catalog_goal_label = None
        elif is_mother:
            profile_age_months = None
            profile_food_catalog_index = mother_food_catalog_index or profile_food_catalog_index
        else:
            profile_food_catalog_index = child_food_catalog_index or profile_food_catalog_index
        profile_type = "MOTHER" if is_mother else "CHILD"
        patient_type = "mother" if is_mother else "baby"
        subject_label = "mẹ" if is_mother else "bé"

        current_question = self._current_question_text(original_input)
        normalized_question = self._normalize_search(current_question)
        goal_code = str(result.get("nutrition_goal_code") or "").strip() or None
        goal_label = str(result.get("nutrition_goal_label") or "").strip() or None
        fallback_goal_code, fallback_goal_label = self._extract_goal(normalized_question, is_mother)
        goal_code = goal_code or fallback_goal_code
        goal_label = goal_label or fallback_goal_label
        goal_code = goal_code or catalog_goal_code
        goal_label = goal_label or catalog_goal_label

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
            "selected_profile_type": selected_profile_type,
            "selected_profile_name": selected_profile_name,
            "explicit_subject": explicit_subject,
            "uses_selected_profile_data": uses_selected_profile_data,
            "request_purpose": result.get("request_purpose") or "information",
            "profile_type": profile_type,
            "profile_name": profile_name,
            "profile_sex": profile_sex,
            "profile_age_months": profile_age_months,
            "health_record_date": health_record_date,
            "profile_weight_kg": profile_weight_kg,
            "profile_height_cm": profile_height_cm,
            "profile_bmi": profile_bmi,
            "profile_bmr_kcal": profile_bmr_kcal,
            "profile_tdee_kcal": profile_tdee_kcal,
            "profile_activity_level": profile_activity_level,
            "previous_health_record_date": previous_health_record_date,
            "previous_weight_kg": previous_weight_kg,
            "previous_height_cm": previous_height_cm,
            "weight_change_kg": weight_change_kg,
            "height_change_cm": height_change_cm,
            "profile_illness_history": profile_illness_history,
            "food_age_group": food_age_group,
            "profile_food_catalog": profile_food_catalog,
            "profile_food_catalog_index": profile_food_catalog_index,
            "profile_routine_today": profile_routine_today,
            "goal_code": goal_code,
            "goal_label": goal_label,
            "requested_ingredients": requested_ingredients,
            "requested_food_names": requested_food_names,
            "food_analysis_requested": food_analysis_requested or bool(result.get("food_analysis_requested")),
            "refers_to_previous_food": bool(result.get("refers_to_previous_food")),
            "child_age_months": profile_age_months if not is_mother else None,
            "child_gender": profile_sex if not is_mother else None,
            "weight_kg": profile_weight_kg if profile_weight_kg is not None else self._extract_number(text.lower(), r"(\d+(?:[,.]\d+)?)\s*kg"),
            "height_cm": profile_height_cm if profile_height_cm is not None else self._extract_number(text.lower(), r"(\d+(?:[,.]\d+)?)\s*cm"),
            "gestational_week": None,
            "main_symptoms": symptoms,
            "symptom_duration": self._extract_duration(text.lower()),
            "temperature_c": self._extract_number(text.lower(), r"(\d+(?:[,.]\d+)?)\s*(?:độ|°c|c)"),
            "feeding_status": self._extract_status(
                text.lower(),
                ["bỏ bú", "bú kém", "biếng ăn", "không chịu ăn", "bỏ ăn", "ăn kém", "ăn ít", "bú tốt"],
            ),
            "stool_status": self._extract_status(text.lower(), ["tiêu chảy", "táo bón", "phân có máu"]),
            "vomit_status": self._extract_status(text.lower(), ["nôn", "ói", "trớ"]),
            "allergies": restricted_foods,
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

    def _merge_missing(
        self,
        missing: list[str],
        intent: str,
        child_age_months: Any,
        explicit_subject: str = "selected_profile",
    ) -> list[str]:
        values = set(missing)
        if explicit_subject == "mother":
            values.discard("child_age_months")
            values.discard("child_gender")
            return sorted(value for value in values if value)
        if child_age_months is not None:
            values.discard("child_age_months")
        if intent in {"nutrition", "growth", "vaccination", "routine", "symptom"} and child_age_months is None:
            values.add("child_age_months")
        if intent == "growth":
            values.update(["weight_kg", "height_cm"])
        if intent == "symptom":
            values.update(["symptom_duration", "temperature_c", "feeding_status"])
        return sorted(value for value in values if value)

    def _request_purpose(self, normalized_question: str, intent: str) -> str:
        if any(term in normalized_question for term in ("phan tich", "danh gia mon", "diem manh", "diem yeu", "diem han che")):
            return "analyze_food"
        if any(term in normalized_question for term in ("goi y mon", "mon gi", "an gi", "thuc don", "vai mon")):
            return "recommend_food"
        if intent == "symptom":
            return "symptom_care"
        if intent == "growth":
            return "growth_assessment"
        if intent == "routine":
            return "routine_planning"
        if intent == "vaccination":
            return "vaccination_guidance"
        if any(term in normalized_question for term in ("so sanh", "khac nhau", "tot hon")):
            return "compare_options"
        return "information"

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
        ("giu dang", "FOR_MOTHER_DIET", "giữ dáng"),
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

    def _explicit_subject(self, normalized_question: str) -> str:
        baby_terms = (
            "be", "em be", "con toi", "con em", "con minh", "tre nho", "chau be",
        )
        if any(self._contains_search_keyword(normalized_question, term) for term in baby_terms):
            return "baby"
        mother_terms = (
            "me", "sau sinh", "cho con bu", "tang sua", "loi sua", "giu dang",
            "giam can", "kinh nguyet", "mang thai",
        )
        if any(self._contains_search_keyword(normalized_question, term) for term in mother_terms):
            return "mother"
        return "selected_profile"

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
