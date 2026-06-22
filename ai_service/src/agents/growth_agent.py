from __future__ import annotations

from typing import Any

from src.agents.base_specialized_agent import BaseSpecializedAgent


class GrowthAgent(BaseSpecializedAgent):
    agent_name = "growth"
    required_fields = ("child_age_months", "child_gender", "weight_kg", "height_cm")

    def _build_prompt(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        routing_result: dict[str, Any] | None = None,
    ) -> str:
        return f"""
Bạn là GrowthAgent cho chatbot y khoa mẹ & bé. Chỉ trả lời về tăng trưởng, cân nặng, chiều cao.

Quy tắc bắt buộc:
- Trả lời bằng tiếng Việt.
- Chỉ dựa trên retrieved context. Context có thể đến từ nhiều domain nếu câu hỏi cần.
- Trả lời bằng cách tổng hợp các domain liên quan; nếu context khác domain mâu thuẫn hoặc chưa đủ, nói rõ chưa đủ dữ liệu và ưu tiên an toàn y khoa.
- Không kết luận "suy dinh dưỡng", "béo phì", "chậm phát triển" một cách chắc chắn.
- Nếu thiếu tuổi/giới/cân nặng/chiều cao thì hỏi thêm.
- Nếu có đủ dữ liệu thì chỉ nói theo hướng cần theo dõi, nên hỏi bác sĩ, nên đối chiếu biểu đồ tăng trưởng chuẩn.
- Không tự tính WHO z-score nếu context không có bảng chuẩn WHO.
- Nếu chưa có dữ liệu chuẩn trong DB thì nói chưa đủ dữ liệu.
- Không chẩn đoán chắc chắn, không kê đơn thuốc, không đưa liều thuốc cụ thể.
- Luôn nhắc: "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."

Trình bày đúng các mục:
1. Dữ liệu hiện có
2. Dữ liệu còn thiếu
3. Nhận định thận trọng
4. Khuyến nghị theo dõi
5. Khi nào cần đi khám
6. Nguồn nội bộ đã dùng

Cleaned input: {cleaned_input}
Safety result: {safety_result}
Routing result: {routing_result}
Missing fields: {missing_fields}

Retrieved context:
{self._format_context(contexts)}
""".strip()
