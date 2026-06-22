from __future__ import annotations

import re
import unicodedata
from typing import Any

from src.agents.base_specialized_agent import BaseSpecializedAgent
from src.constants import MEDICAL_DISCLAIMER


class NutritionAgent(BaseSpecializedAgent):
    agent_name = "nutrition"
    required_fields = ()

    def _is_mother_profile(self, cleaned_input: dict[str, Any]) -> bool:
        return str(cleaned_input.get("profile_type") or "").strip().upper() in {
            "MOTHER", "MOM", "M", "MẸ"
        }

    def _missing_fields(self, cleaned_input: dict[str, Any]) -> list[str]:
        missing = list(super()._missing_fields(cleaned_input))
        is_mother = self._is_mother_profile(cleaned_input)
        if is_mother and "child_age_months" in missing:
            missing.remove("child_age_months")
        if not is_mother:
            has_age = (
                cleaned_input.get("child_age_months") not in (None, "")
                or cleaned_input.get("profile_age_months") not in (None, "")
            )
            if not has_age and "child_age_months" not in missing:
                missing.append("child_age_months")
        return sorted(set(missing))

    def answer(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        top_k: int = 4,
        pre_retrieved_contexts: list[dict[str, Any]] | None = None,
        routing_result: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        question = str(
            cleaned_input.get("cleaned_question")
            or cleaned_input.get("original_question")
            or ""
        )
        is_mother = self._is_mother_profile(cleaned_input)
        # For mother profiles we don't need child age; use 0 as a non-meaningful
        # value so _dish_suggestion can still run (it ignores age for mothers).
        age_months = (
            self._number_or_none(cleaned_input.get("child_age_months"))
            or self._number_or_none(cleaned_input.get("profile_age_months"))
        )
        if is_mother and age_months is None:
            age_months = 0.0
        if self._asks_for_dishes(question):
            missing_fields = self._missing_fields(cleaned_input)
            answer = self._dish_suggestion(cleaned_input, age_months) if age_months is not None else self._fallback_answer(
                cleaned_input,
                pre_retrieved_contexts or [],
                missing_fields,
                RuntimeError("missing age for dish suggestion"),
            )
            return {
                "agent_name": self.agent_name,
                "answer": self._ensure_disclaimer(answer),
                "retrieved_contexts": pre_retrieved_contexts or [],
                "used_collection": self._used_collection(pre_retrieved_contexts or []),
                "used_domains": self._used_domains(pre_retrieved_contexts or []) or ["nutrition"],
                "confidence": 0.78 if age_months is not None else 0.2,
                "needs_more_info": bool(missing_fields),
                "missing_fields": missing_fields,
                "safety_notes": safety_result.get("red_flags", []),
            }

        return super().answer(
            cleaned_input,
            safety_result,
            top_k=top_k,
            pre_retrieved_contexts=pre_retrieved_contexts,
            routing_result=routing_result,
        )

    def _fallback_answer(
        self,
        cleaned_input: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        error: Exception,
    ) -> str:
        age_months = self._number_or_none(cleaned_input.get("child_age_months"))
        question = str(
            cleaned_input.get("cleaned_question")
            or cleaned_input.get("original_question")
            or ""
        )
        normalized_question = self._normalize(question)

        if age_months is None:
            return (
                "Để gợi ý món và kết cấu an toàn, bạn cho mình biết bé hiện bao nhiêu tháng tuổi, "
                "có dị ứng thực phẩm hoặc khó nhai nuốt không nhé. Trong lúc chờ, không nên chọn món "
                f"chỉ dựa vào tên món mà chưa xét độ tuổi. {MEDICAL_DISCLAIMER}"
            )

        if self._asks_for_dishes(question):
            return self._dish_suggestion(cleaned_input, age_months)

        profile_contexts = self._filter_contexts_for_profile(contexts, age_months)
        context_bullets = self._context_bullets(profile_contexts, limit=7)
        if context_bullets:
            bullet_text = "\n".join(f"- {item}" for item in context_bullets)
            return (
                "Dựa trên kho kiến thức dinh dưỡng nội bộ, bạn có thể tham khảo:\n"
                f"{bullet_text}\n\nNếu bạn cho biết mục tiêu cụ thể, dị ứng và tình trạng ăn/bú, "
                f"mình có thể thu hẹp gợi ý hơn. {MEDICAL_DISCLAIMER}"
            )

        return super()._fallback_answer(cleaned_input, contexts, missing_fields, error)

    def _dish_suggestion(self, cleaned_input: dict[str, Any], age_months: float) -> str:
        catalog = self._parse_food_catalog(cleaned_input.get("profile_food_catalog"))
        profile_type = str(cleaned_input.get("profile_type") or "").upper()
        is_mother = profile_type in {"MOTHER", "MOM", "M", "MẸ"}
        food_age_group = str(cleaned_input.get("food_age_group") or "").strip()
        goal_code = str(cleaned_input.get("goal_code") or "").strip().upper()
        goal_label = str(cleaned_input.get("goal_label") or "").strip()

        if not is_mother and age_months < 6:
            return (
                "Bé dưới 6 tháng thường chưa nên ăn dặm; sữa mẹ là nguồn dinh dưỡng ưu tiên, "
                "hoặc dùng sữa công thức phù hợp khi cần. Không tự cho bé ăn cháo, bột, nước trái cây "
                f"hay mật ong nếu không có chỉ định chuyên môn. {MEDICAL_DISCLAIMER}"
            )

        filtered_catalog = self._filter_catalog_by_goal(catalog, goal_code)
        catalog_to_use = filtered_catalog if filtered_catalog else catalog
        goal_note = ""
        if goal_code and goal_label:
            if filtered_catalog:
                goal_note = f" (đã lọc theo mục tiêu: {goal_label})"
            elif catalog:
                goal_note = (
                    f" (chưa có món chuyên cho mục tiêu '{goal_label}' trong danh sách của hồ sơ, "
                    f"hiển thị các món phù hợp chung)"
                )

        if not catalog_to_use:
            subject_desc = (
                "hồ sơ mẹ" if is_mother
                else f"bé khoảng {int(age_months)} tháng"
            )
            return (
                f"Hồ sơ đang chọn là {subject_desc} nhưng hiện chưa có món nào trong "
                f"danh sách BabyOi đúng nhóm ({food_age_group or 'chưa xác định'}) và không nằm trong danh sách hạn chế của profile. "
                f"Mình không lấy món từ nhóm khác để thay thế. {MEDICAL_DISCLAIMER}"
            )

        dishes = catalog_to_use[:6]
        if is_mother:
            texture = (
                "Ưu tiên thực phẩm tươi, nấu chín kỹ, đa dạng nhóm chất; chia thành 3 bữa chính và 1–2 bữa phụ "
                "để duy trì năng lượng ổn định."
            )
        elif age_months < 9:
            texture = "Bắt đầu lượng nhỏ, mềm và đặc vừa; tăng dần theo khả năng nuốt của bé."
        elif age_months < 12:
            texture = "Có thể tăng độ thô và cho miếng mềm nhỏ để bé tập nhai, luôn có người lớn quan sát."
        elif age_months <= 24:
            texture = "Cho ăn thức ăn gia đình được nấu chín và cắt nhỏ phù hợp kỹ năng nhai nuốt."
        else:
            texture = "Luân phiên thực phẩm trong tuần và điều chỉnh lượng theo tín hiệu đói/no của trẻ."

        dish_text = "\n".join(
            f"{index}. {dish['name']}"
            + (f" — {dish['calories']}" if dish["calories"] else "")
            + (f", {dish['protein']}" if dish["protein"] else "")
            for index, dish in enumerate(dishes, start=1)
        )
        if is_mother:
            profile_note = "Theo hồ sơ mẹ đang chọn"
        elif profile_type == "CHILD":
            profile_note = f"Theo hồ sơ bé đang chọn, bé khoảng {int(age_months)} tháng"
        else:
            profile_note = "Theo hồ sơ đang chọn"

        return (
            f"{profile_note}. Các món phù hợp trong danh sách BabyOi và đã loại món hạn chế của profile{goal_note}:\n{dish_text}\n\n"
            f"{texture} Mỗi bữa nên phối hợp tinh bột, đạm, rau/quả và một lượng chất béo phù hợp. "
            f"Không lấy món từ nhóm khác. {MEDICAL_DISCLAIMER}"
        )

    def _filter_catalog_by_goal(
        self,
        catalog: list[dict[str, str]],
        goal_code: str,
    ) -> list[dict[str, str]]:
        if not goal_code or not catalog:
            return []
        return [
            dish for dish in catalog
            if (dish.get("age_group") or "").strip().upper() == goal_code
        ]

    def _asks_for_dishes(self, question: str) -> bool:
        normalized_question = self._normalize(question)
        return any(
            keyword in normalized_question
            for keyword in (
                "mon",
                "mon an",
                "an gi",
                "goi y mon",
                "thuc don",
                "vai mon",
                "list do an",
                "danh sach mon",
                "bua an",
            )
        )

    def _parse_food_catalog(self, raw_catalog: Any) -> list[dict[str, str]]:
        if raw_catalog in (None, ""):
            return []
        foods: list[dict[str, str]] = []
        for raw_item in str(raw_catalog).split(";;"):
            parts = [part.strip() for part in raw_item.split("|")]
            if len(parts) < 3 or not parts[1]:
                continue
            foods.append({
                "id": parts[0],
                "name": parts[1],
                "age_group": parts[2],
                "calories": parts[3] if len(parts) > 3 else "",
                "protein": parts[4] if len(parts) > 4 else "",
            })
        return foods

    def _filter_contexts_for_profile(
        self,
        contexts: list[dict[str, Any]],
        age_months: float | None,
    ) -> list[dict[str, Any]]:
        if age_months is None:
            return contexts
        allowed_range = (
            "dưới 6 tháng" if age_months < 6
            else "6–8 tháng" if age_months <= 8
            else "9–11 tháng" if age_months <= 11
            else "12–24 tháng" if age_months <= 24
            else None
        )
        explicit_age_markers = ("dưới 6 tháng", "6–8 tháng", "9–11 tháng", "12–24 tháng")
        filtered: list[dict[str, Any]] = []
        for context in contexts:
            metadata = context.get("metadata") or {}
            topic = str(context.get("topic") or metadata.get("topic") or "").lower()
            has_explicit_age = any(marker in topic for marker in explicit_age_markers)
            if not has_explicit_age or (allowed_range and allowed_range in topic):
                filtered.append(context)
        return filtered

    def _context_bullets(self, contexts: list[dict[str, Any]], limit: int) -> list[str]:
        result: list[str] = []
        for context in contexts:
            text = str(context.get("document") or context.get("text") or "")
            for raw_line in text.splitlines():
                line = raw_line.strip()
                if not line.startswith("- "):
                    continue
                value = line[2:].strip()
                if value and not value.startswith("http") and value not in result:
                    result.append(value)
                if len(result) >= limit:
                    return result
        return result

    def _number_or_none(self, value: Any) -> float | None:
        try:
            return float(value) if value not in (None, "") else None
        except (TypeError, ValueError):
            return None

    def _normalize(self, value: str) -> str:
        normalized = unicodedata.normalize("NFD", value.lower())
        without_marks = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
        return re.sub(r"\s+", " ", without_marks.replace("đ", "d")).strip()

    def _build_prompt(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        routing_result: dict[str, Any] | None = None,
    ) -> str:
        is_mother = self._is_mother_profile(cleaned_input)
        age_months = (
            None if is_mother
            else self._number_or_none(cleaned_input.get("profile_age_months"))
        )
        profile_contexts = self._filter_contexts_for_profile(contexts, age_months)
        return f"""
Bạn là NutritionAgent cho chatbot y khoa mẹ & bé. Chỉ trả lời về dinh dưỡng, bú, sữa, ăn dặm, dị ứng thức ăn.

Quy tắc bắt buộc:
- Trả lời bằng tiếng Việt.
- Trả lời trực tiếp, tối đa khoảng 300 từ. Nếu người dùng xin món ăn, ưu tiên liệt kê 4–6 món ngắn gọn thay vì giải thích dài từng nguyên liệu.
- Toàn bộ tư vấn phải dựa trên profile đang chọn trong cleaned_input. profile_type, patient_type, profile_age_months, food_age_group, profile_food_catalog, goal_code và goal_label là dữ liệu chuẩn, không được để thông tin trong hội thoại ghi đè.
- BẮT BUỘC xác định chủ thể tư vấn theo patient_type/profile_type: nếu profile_type là MOTHER (hoặc patient_type='mother') thì câu trả lời nói về MẸ, dùng đại từ "mẹ", KHÔNG nói về "bé/trẻ"; nếu profile_type là CHILD thì câu trả lời nói về BÉ và không áp công thức người lớn.
- Nếu profile_type là CHILD và người dùng nói một tuổi khác profile_age_months, nói rõ đang tư vấn theo tuổi của hồ sơ đang chọn; không đổi nhóm tuổi theo câu chat.
- Nếu cleaned_input có goal_code/goal_label (ví dụ FOR_MOTHER_DIET = giảm cân, FOR_MOTHER_INCREASE_MILK_SUPPLY = tăng tiết sữa, FOR_MOTHER_HEALTHY_ENERGY = tăng năng lượng/tăng cân lành mạnh, FOR_MOTHER_DIGESTION_RECOVERY = hỗ trợ tiêu hóa, FOR_MOTHER_POSTPARTUM_BREASTFEEDING = sau sinh đang cho con bú, FOR_MOTHER_CHANGE_DIET = đổi món, FOR_MOTHER_SLEEP_STRESS_SUPPORT = giấc ngủ/giảm stress), phải bám sát mục tiêu đó: chỉ chọn món trong profile_food_catalog có age_group == goal_code; nếu không có món nào khớp goal_code thì nói rõ "chưa có món chuyên cho mục tiêu này trong catalog của hồ sơ" và không bịa món.
- Khi đề xuất món cụ thể, CHỈ được dùng tên món xuất hiện trong profile_food_catalog. Không tự tạo món, không lấy món từ context RAG hoặc nhóm tuổi khác. Catalog này đã loại các món profile hạn chế.
- Nếu profile_food_catalog rỗng, nói chưa có món phù hợp trong catalog của profile; không lấy toàn bộ danh mục làm phương án thay thế.
- Chỉ dựa trên retrieved context. Context có thể đến từ nhiều domain nếu câu hỏi cần.
- Retrieved context chỉ là tài liệu tham khảo, không phải thông tin user cung cấp.
- Chỉ được nói "bé có triệu chứng..." nếu triệu chứng xuất hiện rõ trong cleaned_input["main_symptoms"], cleaned_input["vomit_status"], cleaned_input["feeding_status"], hoặc original_question/cleaned_question.
- Nếu main_symptoms rỗng/null thì không được nhắc ho, nôn, ói, sốt, vàng da, khó thở như thể user đã nói.
- Nếu câu hỏi chỉ hỏi dinh dưỡng theo tuổi, tập trung vào bú mẹ, sữa công thức, chưa ăn dặm, dấu hiệu bú đủ và pha sữa an toàn.
- Trả lời bằng cách tổng hợp các domain liên quan; nếu context khác domain mâu thuẫn hoặc chưa đủ, nói rõ chưa đủ dữ liệu và ưu tiên an toàn y khoa.
- Không bịa nếu context không đủ.
- Không đưa thực đơn quá chắc chắn nếu thiếu tuổi/tháng tuổi/dị ứng/tình trạng sức khỏe.
- Nếu hỏi ăn dặm, phải quan tâm child_age_months.
- Nếu profile context có cân nặng, chiều cao, BMI, BMR hoặc TDEE thì chỉ dùng đúng các giá trị đó để cá nhân hóa; không tự suy đoán chỉ số còn thiếu và không dùng một bản ghi cũ như dữ liệu hiện tại mà không nói rõ ngày đo.
- Với hồ sơ MOTHER, câu hỏi về calo/khẩu phần có thể tham chiếu TDEE gần nhất nhưng phải nêu đây là ước tính và cân nhắc mục tiêu, mức vận động, mang thai hoặc cho con bú nếu user cung cấp.
- Với hồ sơ CHILD, không áp dụng công thức giảm cân của người lớn và không khuyến nghị cắt calo. Ưu tiên độ tuổi, tăng trưởng, đa dạng thực phẩm và hướng dẫn của bác sĩ/chuyên gia dinh dưỡng.
- Khi hỏi về thực đơn hoặc khẩu phần, gợi ý theo nhóm chất (đạm, tinh bột, chất béo, rau quả/vi chất) và tránh khẳng định một thực đơn cố định phù hợp cho mọi người.
- Nếu hỏi dị ứng, nhắc theo dõi dấu hiệu dị ứng và hỏi bác sĩ nếu có triệu chứng nặng.
- Mục "Khi nào cần đi khám" chỉ nêu ngắn 2-4 dấu hiệu liên quan đến dinh dưỡng/bú nếu user không hỏi triệu chứng: bú kém, bỏ bú, tiểu ít, lừ đừ, không tăng cân, sốt ở trẻ nhỏ.
- Không mở rộng sang nôn trớ/vàng da/ho nếu user không hỏi các triệu chứng đó.
- Không chẩn đoán chắc chắn, không kê đơn thuốc, không đưa liều thuốc cụ thể.
- Luôn nhắc: "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."

Trình bày đúng các mục:
1. Nhận định ngắn
2. Thông tin cần biết thêm nếu thiếu
3. Gợi ý chăm sóc/dinh dưỡng an toàn
4. Khi nào cần đi khám
5. Nguồn nội bộ đã dùng

Cleaned input: {cleaned_input}
Safety result: {safety_result}
Routing result: {routing_result}
Missing fields: {missing_fields}

Retrieved context:
{self._format_context(profile_contexts)}
""".strip()
