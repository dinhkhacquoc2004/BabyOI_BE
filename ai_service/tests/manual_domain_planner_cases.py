from __future__ import annotations

import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT))

from src.agents.domain_planner_agent import DomainPlannerAgent


class FakeGeminiClient:
    def generate_text(self, prompt: str) -> str:
        raise RuntimeError("Force rule-based fallback for manual planner checks.")


def main() -> None:
    planner = DomainPlannerAgent(FakeGeminiClient())
    cases = [
        (
            "Bé 7 tháng ăn dặm nhưng đang sốt có tiêm vaccine được không?",
            ["nutrition", "vaccination", "general"],
        ),
        ("Bé 8 tháng chậm tăng cân nên ăn gì?", ["growth", "nutrition"]),
        ("Bé bị sốt sau tiêm thì chăm sóc thế nào?", ["vaccination", "general"]),
        ("Lịch tiêm vaccine cho bé 2 tháng?", ["vaccination"]),
        ("Bé 6 tháng bắt đầu ăn dặm như thế nào?", ["nutrition"]),
    ]

    for question, expected_domains in cases:
        result = planner.plan(question)
        actual_domains = result["candidate_domains"]
        status = "OK" if actual_domains == expected_domains else "FAIL"
        print(f"{status}: {question}")
        print(f"  expected={expected_domains}")
        print(f"  actual={actual_domains}")


if __name__ == "__main__":
    main()
