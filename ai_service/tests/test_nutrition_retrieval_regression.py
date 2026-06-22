from __future__ import annotations

import json
import unittest

from src.agents.answer_validation_agent import AnswerValidationAgent
from src.agents.medical_knowledge_retrieval_agent import MedicalKnowledgeRetrievalAgent
from src.agents.nutrition_agent import NutritionAgent
from src.agents.query_understanding_agent import QueryUnderstandingAgent
from src.api import build_user_friendly_answer
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

    def test_user_answer_removes_dangling_number_before_internal_sources(self) -> None:
        answer = build_user_friendly_answer({
            "answer": "3. Gợi ý món ăn\n\n5. Nguồn nội bộ đã dùng: nutrition_knowledge.txt",
            "safety_result": {"safety_level": "normal"},
            "selected_agent": "nutrition",
        })

        self.assertEqual(answer, "3. Gợi ý món ăn")

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

    def test_general_medical_fallback_uses_retrieved_checklist_when_model_fails(self) -> None:
        agent = MedicalKnowledgeRetrievalAgent(RaisingGemini(), DummyVectorStore())

        result = agent.answer(
            user_question="Bé sốt và tiêu chảy thì chăm sóc sao?",
            intent="general_care",
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
        self.assertNotIn("quota", result["answer"])


if __name__ == "__main__":
    unittest.main()
