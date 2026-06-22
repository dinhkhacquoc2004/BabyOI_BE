from __future__ import annotations

from typing import Any

from src.gemini_client import GeminiClient
from src.utils.json_utils import parse_json_object


INTENTS = {
    "nutrition",
    "symptom",
    "vaccination",
    "growth",
    "sleep",
    "general_care",
    "emergency",
    "out_of_scope",
}

EMERGENCY_KEYWORDS = [
    "co giật",
    "khó thở",
    "tím tái",
    "li bì",
    "mất ý thức",
    "nôn ra máu",
    "nôn xanh",
    "nôn vàng",
    "bỏ bú hoàn toàn",
    "sốt cao",
]

OUT_OF_SCOPE_KEYWORDS = [
    "chứng khoán",
    "đầu tư",
    "crypto",
    "bitcoin",
    "bất động sản",
    "lập trình",
    "du lịch",
]


class IntentRouterAgent:
    def __init__(self, gemini_client: GeminiClient):
        self.gemini_client = gemini_client

    def classify(self, user_question: str) -> dict[str, Any]:
        prompt = f"""
Bạn là Intent Router Agent cho app mẹ & bé. Chỉ phân loại intent, không trả lời tư vấn y tế.

Intent hợp lệ:
- nutrition
- symptom
- vaccination
- growth
- sleep
- general_care
- emergency
- out_of_scope

Nếu có từ khóa nguy hiểm như co giật, khó thở, tím tái, li bì, nôn ra máu, bỏ bú hoàn toàn, sốt cao ở trẻ nhỏ, intent nên là emergency.

Trả về JSON thuần, không markdown:
{{
  "intent": "symptom",
  "confidence": 0.87,
  "reason": "Người dùng hỏi về trẻ bị nôn sau bú",
  "need_retrieval": true
}}

Câu hỏi: {user_question}
""".strip()
        try:
            raw = self.gemini_client.generate_text(prompt)
            parsed = parse_json_object(raw)
            return self._normalize(parsed, user_question)
        except Exception:
            return self._fallback(user_question)

    def _normalize(self, parsed: dict[str, Any], user_question: str) -> dict[str, Any]:
        intent = str(parsed.get("intent", "general_care")).strip()
        if intent not in INTENTS:
            intent = self._fallback(user_question)["intent"]

        confidence = parsed.get("confidence", 0.5)
        try:
            confidence = max(0.0, min(float(confidence), 1.0))
        except (TypeError, ValueError):
            confidence = 0.5

        return {
            "intent": intent,
            "confidence": confidence,
            "reason": str(parsed.get("reason", "Phân loại bằng Gemini.")),
            "need_retrieval": bool(parsed.get("need_retrieval", intent not in {"emergency", "out_of_scope"})),
        }

    def _fallback(self, user_question: str) -> dict[str, Any]:
        text = user_question.lower()
        if any(keyword in text for keyword in EMERGENCY_KEYWORDS):
            intent = "emergency"
        elif any(keyword in text for keyword in OUT_OF_SCOPE_KEYWORDS):
            intent = "out_of_scope"
        elif any(keyword in text for keyword in ["ăn", "dinh dưỡng", "sữa", "bú", "ăn dặm", "trứng"]):
            intent = "nutrition"
        elif any(keyword in text for keyword in ["tiêm", "vắc xin", "vacxin", "vaccine", "5 trong 1"]):
            intent = "vaccination"
        elif any(keyword in text for keyword in ["cân nặng", "nặng", "chiều cao", "tăng trưởng", "nhẹ cân"]):
            intent = "growth"
        elif any(keyword in text for keyword in ["ngủ", "thức đêm"]):
            intent = "sleep"
        elif any(keyword in text for keyword in ["sốt", "nôn", "ho", "tiêu chảy", "táo bón", "dị ứng"]):
            intent = "symptom"
        else:
            intent = "general_care"

        return {
            "intent": intent,
            "confidence": 0.55,
            "reason": "Fallback rule-based do Gemini lỗi hoặc trả về JSON không hợp lệ.",
            "need_retrieval": intent not in {"emergency", "out_of_scope"},
        }
