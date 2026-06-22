from __future__ import annotations

import json
import unittest

from src.agents.answer_validation_agent import AnswerValidationAgent
from src.agents.medical_knowledge_retrieval_agent import MedicalKnowledgeRetrievalAgent
from src.agents.nutrition_agent import NutritionAgent
from src.agents.query_understanding_agent import QueryUnderstandingAgent
from src.api import _compact_raw_result, build_user_friendly_answer
from src.api_debug_formatter import format_debug_log
from src.retrieval.domain_selector import select_retrieval_domains


class StaticGemini:
    def __init__(self, payload: dict | str):
        self.payload = payload

    def generate_text(self, prompt: str) -> str:
        if isinstance(self.payload, str):
            return self.payload
        return json.dumps(self.payload, ensure_ascii=False)


class RaisingGemini:
    def generate_text(self, prompt: str) -> str:
        raise RuntimeError("quota")


class DummyVectorStore:
    collection_name = "baby_oi_general_medical_knowledge"


class NutritionRetrievalRegressionTest(unittest.TestCase):
    def test_simple_nutrition_range_does_not_add_general_or_symptoms(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))

        query = agent.fallback_understanding("dinh dưỡng cho bé từ 1 đến 3 tháng tuổi")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["intent"], "nutrition")
        self.assertEqual(query["child_age_months"], 3.0)
        self.assertEqual(query["mentioned_symptoms"], [])
        self.assertNotIn("child_age_months", query["missing_critical_info"])
        self.assertEqual(domains, ["nutrition"])

    def test_gemini_hallucinated_general_and_symptom_are_tightened(self) -> None:
        agent = QueryUnderstandingAgent(
            StaticGemini(
                {
                    "original_input": "dinh dưỡng cho bé từ 1 đến 3 tháng tuổi",
                    "cleaned_input": "dinh dưỡng cho bé từ 1 đến 3 tháng tuổi",
                    "language": "vi",
                    "child_age_months": None,
                    "mentioned_symptoms": ["ho", "nôn"],
                    "mentioned_conditions": [],
                    "missing_critical_info": ["child_age_months"],
                    "intent": "nutrition",
                    "primary_domain": "nutrition",
                    "candidate_domains": ["nutrition", "general"],
                    "retrieval_required": True,
                    "out_of_scope": False,
                    "confidence": 0.8,
                    "reasoning_summary": "nutrition",
                }
            )
        )

        query = agent.understand("dinh dưỡng cho bé từ 1 đến 3 tháng tuổi")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["child_age_months"], 3.0)
        self.assertEqual(query["mentioned_symptoms"], [])
        self.assertEqual(query["candidate_domains"], ["nutrition"])
        self.assertNotIn("child_age_months", query["missing_critical_info"])
        self.assertEqual(domains, ["nutrition"])

    def test_nutrition_with_real_symptom_adds_general(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))

        query = agent.fallback_understanding("bé 2 tháng bú ít và nôn nhiều")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["child_age_months"], 2.0)
        self.assertEqual(domains, ["nutrition", "general"])

    def test_multi_domain_nutrition_vaccination_stays_specialized(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))

        query = agent.fallback_understanding("bé 6 tháng ăn dặm và lịch tiêm")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["child_age_months"], 6.0)
        self.assertEqual(domains, ["nutrition", "vaccination"])

    def test_calorie_and_tdee_question_routes_to_nutrition(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))

        query = agent.fallback_understanding("Dựa trên TDEE, khẩu phần nên có bao nhiêu calo và protein?")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["intent"], "nutrition")
        self.assertEqual(domains, ["nutrition"])

    def test_profile_header_does_not_turn_ho_so_into_cough_symptom(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched_question = (
            "[Thông tin hồ sơ]\n- Loại hồ sơ: CHILD\n- ageMonths: 9\n\n"
            "[Câu hỏi hiện tại]\nCho tôi vài món phù hợp với bé nhà tôi"
        )

        query = agent.understand(enriched_question)
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["intent"], "nutrition")
        self.assertEqual(query["child_age_months"], 9.0)
        self.assertEqual(query["mentioned_symptoms"], [])
        self.assertEqual(domains, ["nutrition"])

    def test_nutrition_fallback_still_suggests_age_appropriate_dishes(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)

        answer = agent._fallback_answer(
            cleaned_input={
                "child_age_months": 9,
                "cleaned_question": "Cho tôi vài món ăn cho bé",
                "profile_food_catalog": (
                    "251|Cháo đặc cá hồi khoai lang|FOR_BABY_9_11_MONTHS_DEVELOPMENT|155 kcal|9.0 g protein ;; "
                    "252|Nui nhỏ thịt bò cà chua|FOR_BABY_9_11_MONTHS_DEVELOPMENT|165 kcal|10.0 g protein"
                ),
            },
            contexts=[],
            missing_fields=[],
            error=RuntimeError("quota"),
        )

        self.assertIn("Cháo đặc cá hồi khoai lang", answer)
        self.assertIn("Nui nhỏ thịt bò cà chua", answer)
        self.assertNotIn("quota", answer)
        self.assertNotIn("Lỗi kỹ thuật", answer)

    def test_selected_profile_age_overrides_age_typed_in_follow_up(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched_question = (
            "[Thông tin hồ sơ]\n"
            "- ageMonths: 12\n"
            "- profileType: CHILD\n"
            "- foodAgeGroup: FOR_BABY_12_18_MONTHS\n"
            "- profileFoodCatalog: 261|Cơm nát cá hồi bông cải|FOR_BABY_12_18_MONTHS_DEVELOPMENT|225 kcal|13 g protein\n\n"
            "[Lịch sử hội thoại gần đây]\n"
            "USER: Cho tôi món ăn phù hợp\n"
            "ASSISTANT: Đây là gợi ý dinh dưỡng.\n\n"
            "[Câu hỏi hiện tại]\n"
            "Bé mới có 7 tháng"
        )

        query = agent.understand(enriched_question)
        cleaned = agent.to_cleaned_input(query)

        self.assertEqual(query["intent"], "nutrition")
        self.assertEqual(query["child_age_months"], 12.0)
        self.assertEqual(cleaned["profile_age_months"], 12.0)
        self.assertEqual(cleaned["food_age_group"], "FOR_BABY_12_18_MONTHS")
        self.assertIn("Cơm nát cá hồi bông cải", cleaned["profile_food_catalog"])

    def test_dish_fallback_never_uses_food_outside_profile_catalog(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)
        answer = agent._fallback_answer(
            cleaned_input={
                "child_age_months": 7,
                "cleaned_question": "Cho tôi vài món ăn",
                "profile_food_catalog": (
                    "241|Cháo mịn cá hồi bí đỏ|FOR_BABY_6_8_MONTHS_DEVELOPMENT|110 kcal|7 g protein"
                ),
            },
            contexts=[],
            missing_fields=[],
            error=RuntimeError("quota"),
        )

        self.assertIn("Cháo mịn cá hồi bí đỏ", answer)
        self.assertNotIn("cơm mềm", answer.lower())

    def test_dish_suggestion_filters_requested_ingredient_not_first_items(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)
        answer = agent._dish_suggestion(
            cleaned_input={
                "profile_type": "CHILD",
                "food_age_group": "FOR_BABY_9_11_MONTHS",
                "requested_ingredients": ["Cá hồi"],
                "profile_food_catalog": (
                    "250|Cháo gà cà rốt|FOR_BABY_9_11_MONTHS|140 kcal|8 g protein|Thịt gà, Gạo tẻ, Cà rốt ;; "
                    "251|Cháo cá hồi khoai lang|FOR_BABY_9_11_MONTHS|155 kcal|9 g protein|Cá hồi, Khoai lang, Bông cải"
                ),
            },
            age_months=9,
        )

        self.assertIn("Cháo cá hồi khoai lang", answer)
        self.assertNotIn("Cháo gà cà rốt", answer)

    def test_generic_chicken_alias_matches_chicken_parts(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)
        answer = agent._dish_suggestion(
            cleaned_input={
                "profile_type": "MOTHER",
                "goal_code": "FOR_MOTHER_DIET",
                "goal_label": "giảm cân",
                "requested_ingredients": ["gà"],
                "profile_food_catalog": (
                    "1|Cá hồi áp chảo|FOR_MOTHER_DIET|420 kcal|30 g protein|Cá hồi, Khoai tây ;; "
                    "2|Salad ức gà gạo lứt|FOR_MOTHER_DIET|430 kcal|34 g protein|Ức gà, Gạo lứt, Bơ"
                ),
            },
            age_months=0,
        )

        self.assertIn("Salad ức gà gạo lứt", answer)
        self.assertNotIn("Cá hồi áp chảo", answer)

    def test_mother_keep_shape_goal_is_extracted_and_filters_catalog(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched_question = (
            "[Thông tin hồ sơ]\n- profileType: MOTHER\n- name: Lan\n\n"
            "[Câu hỏi hiện tại]\nGợi ý món giúp mẹ giữ dáng"
        )
        cleaned = agent.to_cleaned_input(agent.understand(enriched_question))

        self.assertEqual(cleaned["goal_code"], "FOR_MOTHER_DIET")
        self.assertEqual(cleaned["goal_label"], "giữ dáng")

    def test_mother_increase_milk_goal_does_not_request_child_age(self) -> None:
        query_agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched_question = (
            "[Thông tin hồ sơ]\n- profileType: MOTHER\n- name: Lan\n"
            "- profileFoodCatalog: 201|Cháo yến mạch cá hồi rau ngót|FOR_MOTHER_INCREASE_MILK_SUPPLY|455 kcal|31 g protein|Cá hồi, Yến mạch, Rau ngót\n\n"
            "[Câu hỏi hiện tại]\nTôi muốn tăng sữa"
        )
        cleaned = query_agent.to_cleaned_input(query_agent.understand(enriched_question))
        nutrition_agent = NutritionAgent.__new__(NutritionAgent)
        nutrition_agent.collection_name = "baby_oi_nutrition_knowledge"
        result = nutrition_agent.answer(cleaned, {"red_flags": []}, pre_retrieved_contexts=[])

        self.assertEqual(cleaned["goal_code"], "FOR_MOTHER_INCREASE_MILK_SUPPLY")
        self.assertIn("Cháo yến mạch cá hồi rau ngót", result["answer"])
        self.assertNotIn("bé hiện bao nhiêu tháng", result["answer"])

    def test_food_analysis_uses_database_fields_and_marks_missing_data(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)
        answer = agent._food_analysis({
            "requested_food_names": ["Cháo cá hồi bí đỏ"],
            "profile_food_catalog": (
                "201|Cháo cá hồi bí đỏ|FOR_MOTHER_DIET|420 kcal|28 g protein|Cá hồi, Bí đỏ, Gạo tẻ|"
                "45 g carbs|14 g fat|6 g fiber|4 g sugar|320 mg sodium|Giàu đạm theo dữ liệu món||"
                "Dùng khẩu phần vừa|Nấu chín kỹ"
            ),
        })

        self.assertIn("Điểm mạnh: Giàu đạm theo dữ liệu món", answer)
        self.assertIn("Điểm hạn chế: Database chưa có", answer)
        self.assertIn("Lưu ý: Dùng khẩu phần vừa", answer)
        self.assertIn("Chế biến: Nấu chín kỹ", answer)

    def test_analysis_intent_accepts_natural_and_follow_up_phrases(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)

        self.assertTrue(agent._asks_for_analysis("Phân tích tôi món súp bí đỏ cá hồi", {}))
        self.assertTrue(agent._asks_for_analysis("Thông tin chi tiết của nó như nào?", {}))

    def test_current_goal_overrides_previous_conversation_goal(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: MOTHER\n- name: Linh\n\n"
            "[Lịch sử hội thoại gần đây]\nUSER: Cho tôi món giảm cân\nASSISTANT: Đây là các món giữ dáng.\n\n"
            "[Câu hỏi hiện tại]\nTôi muốn tăng sữa"
        )
        cleaned = agent.to_cleaned_input(agent.understand(enriched))

        self.assertEqual(cleaned["goal_code"], "FOR_MOTHER_INCREASE_MILK_SUPPLY")
        self.assertEqual(cleaned["patient_type"], "mother")

    def test_mother_profile_can_ask_about_baby_without_profile_switch(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: MOTHER\n- name: Linh\n"
            "- ageMonths: 360\n- weightKg: 55\n- tdeeKcal: 1900\n"
            "- profileFoodCatalog: 1|Salad|FOR_MOTHER_DIET|400 kcal|20 g protein|Ức gà\n\n"
            "[Câu hỏi hiện tại]\nBé 8 tháng đang sốt thì theo dõi thế nào?"
        )
        query = agent.understand(enriched)
        cleaned = agent.to_cleaned_input(query)

        self.assertEqual(query["intent"], "symptom")
        self.assertEqual(cleaned["patient_type"], "baby")
        self.assertEqual(cleaned["profile_type"], "CHILD")
        self.assertEqual(cleaned["child_age_months"], 8.0)
        self.assertFalse(cleaned["uses_selected_profile_data"])
        self.assertIsNone(cleaned["profile_weight_kg"])
        self.assertIsNone(cleaned["profile_food_catalog"])
        self.assertEqual(cleaned["request_purpose"], "symptom_care")

    def test_child_profile_can_ask_about_mother_without_profile_switch(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: CHILD\n- name: An\n- ageMonths: 8\n\n"
            "[Câu hỏi hiện tại]\nMẹ sau sinh muốn tăng sữa nên ăn thế nào?"
        )
        cleaned = agent.to_cleaned_input(agent.understand(enriched))

        self.assertEqual(cleaned["patient_type"], "mother")
        self.assertEqual(cleaned["profile_type"], "MOTHER")
        self.assertFalse(cleaned["uses_selected_profile_data"])
        self.assertIsNone(cleaned["child_age_months"])

    def test_child_profile_mother_question_uses_cross_mother_catalog(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: CHILD\n- name: An\n- ageMonths: 8\n"
            "- childFoodCatalogIndex: 101|Cháo gà|FOR_BABY_6_8_MONTHS|110 kcal|7 g protein|Thịt gà\n"
            "- motherFoodCatalogIndex: "
            "1|Salad ức gà gạo lứt|FOR_MOTHER_DIET|430 kcal|34 g protein|Ức gà, Gạo lứt ;; "
            "201|Cháo yến mạch cá hồi rau ngót|FOR_MOTHER_INCREASE_MILK_SUPPLY|455 kcal|31 g protein|Cá hồi, Yến mạch, Rau ngót\n\n"
            "[Câu hỏi hiện tại]\nMẹ muốn giảm cân, gợi ý món gà"
        )
        cleaned = agent.to_cleaned_input(agent.understand(enriched))
        nutrition_agent = NutritionAgent.__new__(NutritionAgent)
        answer = nutrition_agent._dish_suggestion(cleaned, age_months=0)

        self.assertEqual(cleaned["patient_type"], "mother")
        self.assertFalse(cleaned["uses_selected_profile_data"])
        self.assertIn("Salad ức gà gạo lứt", answer)
        self.assertNotIn("Cháo gà", answer)
        self.assertNotIn("FOR_MOTHER", answer)

    def test_mother_profile_baby_question_uses_cross_child_catalog(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: MOTHER\n- name: Linh\n"
            "- motherFoodCatalogIndex: 1|Salad|FOR_MOTHER_DIET|400 kcal|20 g protein|Ức gà\n"
            "- childFoodCatalogIndex: "
            "101|Cháo mịn cá hồi bí đỏ|FOR_BABY_6_8_MONTHS|110 kcal|7 g protein|Cá hồi, Bí đỏ ;; "
            "251|Cháo đặc cá hồi khoai lang|FOR_BABY_9_11_MONTHS|155 kcal|9 g protein|Cá hồi, Khoai lang\n\n"
            "[Câu hỏi hiện tại]\nBé 8 tháng ăn món cá hồi nào?"
        )
        cleaned = agent.to_cleaned_input(agent.understand(enriched))
        nutrition_agent = NutritionAgent.__new__(NutritionAgent)
        answer = nutrition_agent._dish_suggestion(cleaned, age_months=8)

        self.assertEqual(cleaned["patient_type"], "baby")
        self.assertIn("Cháo mịn cá hồi bí đỏ", answer)
        self.assertNotIn("Cháo đặc cá hồi khoai lang", answer)
        self.assertNotIn("FOR_BABY", answer)

    def test_complete_catalog_index_prevents_first_thirty_goal_false_negative(self) -> None:
        agent = NutritionAgent.__new__(NutritionAgent)
        answer = agent._dish_suggestion({
            "profile_type": "MOTHER",
            "goal_code": "FOR_MOTHER_INCREASE_MILK_SUPPLY",
            "goal_label": "tăng tiết sữa",
            "profile_food_catalog": "1|Salad|FOR_MOTHER_DIET|400 kcal|20 g protein|Ức gà",
            "profile_food_catalog_index": (
                "1|Salad|FOR_MOTHER_DIET|400 kcal|20 g protein|Ức gà ;; "
                "201|Cháo yến mạch cá hồi rau ngót|FOR_MOTHER_INCREASE_MILK_SUPPLY|455 kcal|31 g protein|Cá hồi, Yến mạch, Rau ngót"
            ),
        }, age_months=0)

        self.assertIn("Cháo yến mạch cá hồi rau ngót", answer)
        self.assertNotIn("chưa có món chuyên", answer)

    def test_current_health_topic_overrides_previous_nutrition_topic(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: MOTHER\n- name: Linh\n- ageMonths: 360\n\n"
            "[Lịch sử hội thoại gần đây]\n"
            "USER: Tôi muốn giảm cân\nASSISTANT: Đây là gợi ý dinh dưỡng.\n\n"
            "[Câu hỏi hiện tại]\nBé 6 tháng sốt 38.5 độ thì theo dõi sao?"
        )
        query = agent.understand(enriched)
        cleaned = agent.to_cleaned_input(query)
        domains = select_retrieval_domains(query, {"safety_level": "urgent"})

        self.assertEqual(query["intent"], "symptom")
        self.assertEqual(cleaned["patient_type"], "baby")
        self.assertEqual(cleaned["child_age_months"], 6.0)
        self.assertEqual(cleaned["request_purpose"], "symptom_care")
        self.assertIn("general", domains)
        self.assertIsNone(cleaned["goal_code"])

    def test_current_routine_topic_overrides_previous_vaccination_topic(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched = (
            "[Thông tin hồ sơ]\n- profileType: CHILD\n- name: An\n- ageMonths: 9\n\n"
            "[Lịch sử hội thoại gần đây]\n"
            "USER: Lịch tiêm của bé thế nào?\nASSISTANT: Đây là thông tin tiêm chủng.\n\n"
            "[Câu hỏi hiện tại]\nGiờ ngủ cho bé hôm nay nên sắp xếp sao?"
        )
        query = agent.understand(enriched)
        cleaned = agent.to_cleaned_input(query)

        self.assertEqual(query["intent"], "routine")
        self.assertEqual(cleaned["request_purpose"], "routine_planning")
        self.assertEqual(cleaned["patient_type"], "baby")

    def test_mother_subject_does_not_require_child_age_for_growth(self) -> None:
        from src.agents.growth_agent import GrowthAgent

        agent = GrowthAgent.__new__(GrowthAgent)
        missing = agent._missing_fields({
            "patient_type": "mother",
            "weight_kg": 55.0,
            "height_cm": 160.0,
            "missing_important_fields": ["child_age_months", "child_gender"],
        })

        self.assertNotIn("child_age_months", missing)
        self.assertNotIn("child_gender", missing)

    def test_user_answer_removes_dangling_number_before_internal_sources(self) -> None:
        answer = build_user_friendly_answer({
            "answer": "3. Gợi ý món ăn\n\n5. Nguồn nội bộ đã dùng: nutrition_knowledge.txt",
            "safety_result": {"safety_level": "normal"},
            "selected_agent": "nutrition",
        })

        self.assertEqual(answer, "3. Gợi ý món ăn")

    def test_user_answer_never_exposes_internal_food_type_code(self) -> None:
        answer = build_user_friendly_answer({
            "answer": "Món này thuộc FOR_MOTHER_INCREASE_MILK_SUPPLY.",
            "safety_result": {"safety_level": "normal"},
            "selected_agent": "nutrition",
        })

        self.assertNotIn("FOR_MOTHER", answer)
        self.assertIn("hỗ trợ duy trì nguồn sữa", answer)

    def test_api_diagnostics_do_not_echo_large_catalogs(self) -> None:
        large_catalog = "food|" * 100_000
        result = {
            "cleaned_input": {
                "patient_type": "mother",
                "profile_food_catalog": large_catalog,
                "profile_food_catalog_index": large_catalog,
            },
            "query_understanding_result": {
                "original_input": large_catalog,
                "cleaned_input": large_catalog,
                "intent": "nutrition",
                "primary_domain": "nutrition",
                "candidate_domains": ["nutrition"],
            },
            "intent_result": {"intent": "nutrition"},
            "routing_result": {"primary_domain": "nutrition"},
            "safety_result": {"safety_level": "normal"},
            "retrieval_debug": {"final_context_count": 0},
            "validation_result": {},
            "retrieved_contexts": [],
            "selected_agent": "nutrition",
            "used_domains": [],
            "answer": "Câu trả lời ngắn.",
            "used_collection": "nutrition",
        }

        compact = _compact_raw_result(result)
        debug = format_debug_log(result)

        self.assertNotIn("profile_food_catalog", str(compact))
        self.assertNotIn(large_catalog[:100], str(compact))
        self.assertNotIn(large_catalog[:100], debug)
        self.assertLess(len(str(compact)), 10_000)
        self.assertLess(len(debug), 10_000)

    def test_validation_gate_skips_simple_nutrition_under_three_months(self) -> None:
        validator = AnswerValidationAgent(StaticGemini("not json"))

        use_gemini, reasons = validator.should_use_gemini_validation(
            answer=(
                "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ. "
                "Bé 1-3 tháng chủ yếu bú mẹ hoặc sữa công thức, chưa ăn dặm."
            ),
            user_question="dinh dưỡng cho bé từ 1 đến 3 tháng tuổi",
            cleaned_input={
                "child_age_months": 3.0,
                "main_symptoms": [],
                "vomit_status": None,
                "feeding_status": None,
            },
            intent_result={"intent": "nutrition"},
            safety_result={"safety_level": "normal", "red_flags": []},
            retrieved_contexts=[],
        )

        self.assertFalse(use_gemini)
        self.assertEqual(reasons, [])

    def test_validation_gate_uses_gemini_for_symptom_or_danger_signs(self) -> None:
        validator = AnswerValidationAgent(StaticGemini("not json"))

        use_gemini, reasons = validator.should_use_gemini_validation(
            answer="Nên theo dõi và đi khám nếu nặng hơn.",
            user_question="bé sốt 39 độ bỏ bú",
            cleaned_input={
                "child_age_months": None,
                "main_symptoms": ["sốt", "bỏ bú"],
                "vomit_status": None,
                "feeding_status": "bỏ bú",
            },
            intent_result={"intent": "symptom"},
            safety_result={"safety_level": "urgent", "red_flags": ["bỏ bú"]},
            retrieved_contexts=[],
        )

        self.assertTrue(use_gemini)
        self.assertIn("intent is symptom", reasons)

    def test_routine_question_uses_only_routine_collection(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))

        query = agent.understand("Lịch sinh hoạt và giờ ngủ cho bé 7 tháng")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["intent"], "routine")
        self.assertEqual(query["child_age_months"], 7.0)
        self.assertEqual(domains, ["routine"])

    def test_selected_profile_routine_is_preserved_in_cleaned_input(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched_question = (
            "[Thông tin hồ sơ]\n"
            "- id: 12\n"
            "- ageMonths: 7\n"
            "- profileType: CHILD\n"
            "- profileRoutineToday: 07:00 Bé thức dậy (WAKE, đã hoàn thành) ;; "
            "09:00 Giấc ngủ 1 (NAP, chưa hoàn thành)\n\n"
            "[Câu hỏi hiện tại]\n"
            "Lịch ngủ hôm nay có phù hợp không?"
        )

        query = agent.understand(enriched_question)
        cleaned = agent.to_cleaned_input(query)

        self.assertEqual(query["intent"], "routine")
        self.assertEqual(query["child_age_months"], 7.0)
        self.assertIn("07:00 Bé thức dậy", cleaned["profile_routine_today"])

    def test_child_sick_question_routes_to_general_collection(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))

        query = agent.understand("Bé 8 tháng bị ốm, sốt và sổ mũi thì theo dõi chăm sóc sao?")
        domains = select_retrieval_domains(query, {"safety_level": "normal"})

        self.assertEqual(query["intent"], "symptom")
        self.assertEqual(query["child_age_months"], 8.0)
        self.assertEqual(domains, ["general"])

    def test_sick_and_poor_appetite_uses_profile_and_both_domains(self) -> None:
        agent = QueryUnderstandingAgent(StaticGemini("not json"))
        enriched_question = (
            "[Thông tin hồ sơ]\n"
            "- name: Thao\n"
            "- profileType: CHILD\n"
            "- ageMonths: 7\n"
            "- sex: MALE\n"
            "- healthRecordDate: 2026-06-20\n"
            "- weightKg: 10.0\n"
            "- heightCm: 70.0\n"
            "- previousHealthRecordDate: 2026-05-20\n"
            "- previousWeightKg: 9.4\n"
            "- weightChangeKg: 0.6\n"
            "- profileIllnessHistory: 2026-06-01 đến 2026-06-03: Cảm lạnh\n"
            "- restrictedFoods: Tôm ;; Trứng\n\n"
            "[Câu hỏi hiện tại]\n"
            "Bé đang bị ốm và biếng ăn tôi nên làm như nào?"
        )

        query = agent.understand(enriched_question)
        cleaned = agent.to_cleaned_input(query)
        domains = select_retrieval_domains(query, {"safety_level": "urgent"})

        self.assertEqual(query["intent"], "symptom")
        self.assertEqual(cleaned["profile_name"], "Thao")
        self.assertEqual(cleaned["profile_age_months"], 7.0)
        self.assertEqual(cleaned["child_age_months"], 7.0)
        self.assertEqual(cleaned["profile_weight_kg"], 10.0)
        self.assertEqual(cleaned["health_record_date"], "2026-06-20")
        self.assertEqual(cleaned["feeding_status"], "biếng ăn")
        self.assertEqual(cleaned["allergies"], ["Tôm", "Trứng"])
        self.assertIn("Cảm lạnh", cleaned["profile_illness_history"])
        self.assertEqual(domains, ["nutrition", "general"])

    def test_general_medical_fallback_uses_retrieved_checklist_when_model_fails(self) -> None:
        agent = MedicalKnowledgeRetrievalAgent(RaisingGemini(), DummyVectorStore())

        result = agent.answer(
            user_question="Bé sốt và tiêu chảy thì chăm sóc sao?",
            intent="general_care",
            cleaned_input={
                "profile_type": "CHILD",
                "profile_name": "Thao",
                "profile_age_months": 7.0,
                "profile_weight_kg": 10.0,
                "profile_height_cm": 70.0,
                "health_record_date": "2026-06-20",
                "allergies": ["Tôm"],
            },
            pre_retrieved_contexts=[
                {
                    "document": "- Tiếp tục bú mẹ; tiếp tục cho ăn phù hợp tuổi nếu bé ăn được.\n"
                    "- Cần khám sớm/khẩn nếu phân có máu, nôn liên tục hoặc có dấu mất nước.",
                    "metadata": {
                        "domain": "general",
                        "collection": "baby_oi_general_medical_knowledge",
                    },
                }
            ],
        )

        self.assertIn("Tiếp tục bú mẹ", result["answer"])
        self.assertIn("dấu mất nước", result["answer"])
        self.assertIn("bé Thao, hiện 7 tháng", result["answer"])
        self.assertIn("ngày 2026-06-20: 10 kg, 70 cm", result["answer"])
        self.assertIn("Hồ sơ đang hạn chế: Tôm", result["answer"])
        self.assertNotIn("quota", result["answer"])


if __name__ == "__main__":
    unittest.main()
