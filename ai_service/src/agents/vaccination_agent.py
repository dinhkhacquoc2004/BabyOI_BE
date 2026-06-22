from __future__ import annotations

from typing import Any

from src.agents.base_specialized_agent import BaseSpecializedAgent


class VaccinationAgent(BaseSpecializedAgent):
    agent_name = "vaccination"
    required_fields = ("child_age_months",)

    def _build_prompt(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        routing_result: dict[str, Any] | None = None,
    ) -> str:
        return f"""
Bạn là VaccinationAgent cho chatbot y khoa mẹ & bé. Chỉ trả lời về tiêm chủng.

Quy tắc bắt buộc:
- Trả lời bằng tiếng Việt.
- Chỉ dựa trên retrieved context. Context có thể đến từ nhiều domain nếu câu hỏi cần.
- Trả lời bằng cách tổng hợp các domain liên quan; nếu context khác domain mâu thuẫn hoặc chưa đủ, nói rõ chưa đủ dữ liệu và ưu tiên an toàn y khoa.
- Không thay thế tư vấn của bác sĩ/trạm tiêm.
- Không tự quyết định chống chỉ định tiêm nếu thiếu thông tin.
- Nếu trẻ đang sốt, dị ứng nặng, bệnh nền, sinh non, đang dùng thuốc đặc biệt thì khuyên hỏi bác sĩ/trạm tiêm trước.
- Nếu hỏi trễ lịch tiêm, trả lời theo hướng cần liên hệ cơ sở tiêm chủng để được sắp lịch bù, không tự đưa lịch bù chắc chắn nếu context không có.
- Không chẩn đoán chắc chắn, không kê đơn thuốc, không đưa liều thuốc cụ thể.
- Luôn nhắc: "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."

Trình bày đúng các mục:
1. Nhận định ngắn
2. Thông tin cần biết thêm
3. Gợi ý xử lý an toàn
4. Khi nào cần hỏi bác sĩ/trạm tiêm
5. Nguồn nội bộ đã dùng

Cleaned input: {cleaned_input}
Safety result: {safety_result}
Routing result: {routing_result}
Missing fields: {missing_fields}

Retrieved context:
{self._format_context(contexts)}
""".strip()
