from __future__ import annotations

from typing import Any

from src.constants import MEDICAL_DISCLAIMER
from src.gemini_client import GeminiClient
from src.utils.json_utils import parse_json_object


EMERGENCY_RULES = {
    "khó thở": ["khó thở", "thở rít", "thở lõm", "rút lõm"],
    "tím tái": ["tím tái", "môi tím", "người tím"],
    "co giật": ["co giật", "giật toàn thân"],
    "li bì": ["li bì", "lừ đừ", "khó đánh thức"],
    "mất ý thức": ["mất ý thức", "ngất"],
    "nôn ra máu": ["nôn ra máu", "ói ra máu"],
    "nôn xanh/vàng": ["nôn xanh", "nôn vàng", "dịch xanh", "dịch vàng"],
    "mất nước nặng": ["mắt trũng", "khóc không có nước mắt", "tiểu rất ít", "môi khô"],
    "bỏ bú hoàn toàn": ["bỏ bú hoàn toàn", "không bú được"],
}

URGENT_KEYWORDS = [
    "sốt",
    "tiêu chảy nhiều",
    "nôn nhiều",
    "bú kém",
    "ăn kém",
    "biếng ăn",
    "không chịu ăn",
    "bỏ ăn",
    "kéo dài",
    "nhiều lần",
    "phân có máu",
]


class SafetyTriageAgent:
    def __init__(self, gemini_client: GeminiClient):
        self.gemini_client = gemini_client

    def triage_rule_based(self, user_question: str) -> dict[str, Any]:
        return self._rule_based_triage(user_question)

    def triage(self, user_question: str) -> dict[str, Any]:
        rule_result = self._rule_based_triage(user_question)
        if rule_result["safety_level"] == "emergency":
            return rule_result

        try:
            gemini_result = self._gemini_triage(user_question)
            if gemini_result["safety_level"] == "emergency" or rule_result["safety_level"] == "normal":
                return gemini_result
        except Exception:
            pass

        return rule_result

    def _rule_based_triage(self, user_question: str) -> dict[str, Any]:
        text = self._current_question(user_question).lower()
        red_flags = []
        for label, keywords in EMERGENCY_RULES.items():
            if any(keyword in text for keyword in keywords):
                red_flags.append(label)

        if "dưới 3 tháng" in text and "sốt" in text:
            red_flags.append("trẻ dưới 3 tháng có sốt")
        if "sốt cao" in text and any(keyword in text for keyword in ["mệt", "li bì", "co giật", "khó thở"]):
            red_flags.append("sốt cao kèm bất thường")

        if red_flags:
            return {
                "safety_level": "emergency",
                "red_flags": red_flags,
                "action": f"Đưa trẻ đi cấp cứu hoặc gọi cấp cứu ngay. {MEDICAL_DISCLAIMER}",
                "can_continue_rag": False,
            }

        urgent_flags = [keyword for keyword in URGENT_KEYWORDS if keyword in text]
        if urgent_flags:
            return {
                "safety_level": "urgent",
                "red_flags": urgent_flags,
                "action": (
                    "Nên liên hệ bác sĩ hoặc đưa trẻ đi khám sớm nếu triệu chứng tiếp diễn "
                    f"hoặc nặng hơn. {MEDICAL_DISCLAIMER}"
                ),
                "can_continue_rag": True,
            }

        return {
            "safety_level": "normal",
            "red_flags": [],
            "action": f"Có thể tham khảo thông tin chăm sóc chung. {MEDICAL_DISCLAIMER}",
            "can_continue_rag": True,
        }

    def _current_question(self, text: str) -> str:
        for marker in ("[Câu hỏi hiện tại]", "[Cau hoi hien tai]"):
            if marker in text:
                return text.rsplit(marker, 1)[1].strip()
        return text

    def _gemini_triage(self, user_question: str) -> dict[str, Any]:
        prompt = f"""
Bạn là Safety Triage Agent cho chatbot y khoa mẹ & bé.
Nhiệm vụ: kiểm tra mô tả của phụ huynh có dấu hiệu nguy hiểm không.

Không chẩn đoán bệnh. Không kê đơn thuốc. Không đưa liều thuốc cụ thể.
Luôn nhắc: "{MEDICAL_DISCLAIMER}"

Phân loại:
- emergency: khó thở, tím tái, co giật, li bì, mất ý thức, nôn ra máu, nôn xanh/vàng, dấu hiệu mất nước nặng, trẻ dưới 3 tháng sốt, bỏ bú hoàn toàn, sốt rất cao kèm bất thường.
- urgent: triệu chứng kéo dài, tái diễn nhiều lần, sốt, tiêu chảy nhiều, ăn/bú kém nhưng chưa có emergency.
- normal: câu hỏi chăm sóc thông thường.

Trả về JSON thuần:
{{
  "safety_level": "emergency",
  "red_flags": ["khó thở", "co giật"],
  "action": "Đưa trẻ đi cấp cứu hoặc gọi cấp cứu ngay",
  "can_continue_rag": false
}}

Câu hỏi: {user_question}
""".strip()
        raw = self.gemini_client.generate_text(prompt)
        parsed = parse_json_object(raw)
        level = str(parsed.get("safety_level", "normal")).strip()
        if level not in {"emergency", "urgent", "normal"}:
            level = "normal"

        action = str(parsed.get("action", MEDICAL_DISCLAIMER))
        if MEDICAL_DISCLAIMER.lower() not in action.lower():
            action += f" {MEDICAL_DISCLAIMER}"

        return {
            "safety_level": level,
            "red_flags": list(parsed.get("red_flags", [])),
            "action": action,
            "can_continue_rag": bool(parsed.get("can_continue_rag", level != "emergency")),
        }
