from __future__ import annotations

from typing import Any

from src.agents.base_specialized_agent import BaseSpecializedAgent


class RoutineAgent(BaseSpecializedAgent):
    agent_name = "routine"
    required_fields = ()

    def _missing_fields(self, cleaned_input: dict[str, Any]) -> list[str]:
        missing = set(super()._missing_fields(cleaned_input))
        if cleaned_input.get("patient_type") == "mother":
            missing.discard("child_age_months")
        elif cleaned_input.get("child_age_months") in (None, ""):
            missing.add("child_age_months")
        return sorted(missing)

    def _build_prompt(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        routing_result: dict[str, Any] | None = None,
    ) -> str:
        return f"""
Bạn là RoutineAgent của BabyOi, hỗ trợ lịch sinh hoạt, giấc ngủ, vận động,
thời gian màn hình, giờ ăn/bú và hoạt động tương tác cho mẹ hoặc bé theo chủ thể câu hỏi.

Quy tắc bắt buộc:
- Trả lời bằng tiếng Việt, ngắn gọn, thực tế và dựa trên retrieved context.
- Chủ thể duy nhất là cleaned_input.patient_type/subject_label. Profile đang chọn chỉ được dùng khi uses_selected_profile_data=true.
- Nếu user hỏi mẹ, không áp lịch ngủ/cữ bú của trẻ và không yêu cầu tháng tuổi bé.
- Nếu user hỏi bé ngoài profile đang chọn, dùng tuổi được nêu trong câu; thiếu tuổi thì hỏi thêm, không dùng dữ liệu mẹ.
- Bám sát request_purpose hiện tại; không kéo chủ đề/mục tiêu cũ sang.
- Nếu có `profileRoutineToday`, trước tiên nhận xét lịch thực tế của đúng profile đó,
  rồi mới gợi ý điều chỉnh. Không biến lịch mẫu thành giờ bắt buộc cho mọi trẻ.
- Ưu tiên tổng thời lượng ngủ/vận động trong 24 giờ, tín hiệu đói-no và tín hiệu buồn ngủ;
  chấp nhận khác biệt cá nhân và điều chỉnh từng bước nhỏ.
- Không khuyên luyện ngủ bằng cách bỏ mặc trẻ khóc, không ép ăn/bú, không tự ý cắt cữ
  của trẻ nhỏ hoặc trẻ đang tăng trưởng kém.
- Nếu câu hỏi có sốt, khó thở, co giật, li bì, bỏ bú, mất nước hoặc triệu chứng đáng lo,
  ưu tiên hướng dẫn khám và không giải thích mọi vấn đề bằng lịch sinh hoạt.
- Không chẩn đoán, không kê thuốc, không đưa liều thuốc.
- Luôn kết thúc bằng: "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."

Nên trình bày:
1. Nhận xét theo đúng chủ thể câu hỏi
2. Mục tiêu phù hợp nhóm tuổi
3. Gợi ý lịch linh hoạt hoặc thay đổi nhỏ
4. Dấu hiệu cần hỏi bác sĩ/cơ sở y tế

Cleaned input: {cleaned_input}
Safety result: {safety_result}
Routing result: {routing_result}
Missing fields: {missing_fields}

Retrieved context:
{self._format_context(contexts)}
""".strip()
