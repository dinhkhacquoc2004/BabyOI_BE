from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src.agents.query_understanding_agent import QueryUnderstandingAgent
from src.retrieval.domain_selector import select_retrieval_domains


class BrokenGemini:
    def generate_text(self, prompt: str) -> str:
        return "not json"


def main() -> None:
    agent = QueryUnderstandingAgent(BrokenGemini())
    cases = [
        ("dinh dưỡng cho bé từ 1 đến 3 tháng tuổi", ["nutrition"], 3.0, []),
        ("bé 2 tháng bú ít và nôn nhiều", ["nutrition", "general"], 2.0, ["non"]),
        ("bé 6 tháng ăn dặm như nào", ["nutrition"], 6.0, []),
        ("bé sốt 39 độ bỏ bú", ["nutrition", "general"], None, ["sot", "bo bu"]),
        ("bé 6 tháng ăn dặm và lịch tiêm", ["nutrition", "vaccination"], 6.0, []),
    ]

    for question, expected_domains, expected_age, expected_symptoms in cases:
        query = agent.fallback_understanding(question)
        domains = select_retrieval_domains(query, {"safety_level": "normal"})
        print(
            {
                "question": question,
                "intent": query["intent"],
                "age": query["child_age_months"],
                "symptoms": query["mentioned_symptoms"],
                "domains": domains,
            }
        )
        assert domains == expected_domains, (question, domains, expected_domains)
        if expected_age is not None:
            assert query["child_age_months"] == expected_age, (question, query["child_age_months"])
            assert "child_age_months" not in query["missing_critical_info"], query
        for symptom in expected_symptoms:
            assert symptom in query["mentioned_symptoms"], (question, query["mentioned_symptoms"])


if __name__ == "__main__":
    main()
