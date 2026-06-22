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
        goal_code = str(cleaned_input.get("goal_code") or "").strip()
        if self._asks_for_analysis(question, cleaned_input):
            missing_fields = self._missing_fields(cleaned_input)
            answer = self._food_analysis(cleaned_input)
            return {
                "agent_name": self.agent_name,
                "answer": self._ensure_disclaimer(answer),
                "retrieved_contexts": pre_retrieved_contexts or [],
                "used_collection": self._used_collection(pre_retrieved_contexts or []),
                "used_domains": self._used_domains(pre_retrieved_contexts or []) or ["nutrition"],
                "confidence": 0.82 if self._catalog_for_filtering(cleaned_input) else 0.2,
                "needs_more_info": bool(missing_fields),
                "missing_fields": missing_fields,
                "safety_notes": safety_result.get("red_flags", []),
            }
        if self._asks_for_dishes(question) or (is_mother and bool(goal_code)):
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
        is_mother = self._is_mother_profile(cleaned_input)
        if is_mother:
            age_months = 0.0
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
        catalog = self._catalog_for_filtering(cleaned_input)
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

        catalog_to_use = catalog
        goal_note = ""
        if goal_code and goal_label:
            filtered_catalog = self._filter_catalog_by_goal(catalog, goal_code)
            if filtered_catalog:
                goal_note = f" (đã lọc theo mục tiêu: {goal_label})"
                catalog_to_use = filtered_catalog
            else:
                return (
                    f"Theo hồ sơ đang chọn, chưa có món chuyên cho mục tiêu '{goal_label}' "
                    f"trong danh sách món phù hợp hiện có. Mình không dùng món của mục tiêu khác để thay thế. "
                    f"{MEDICAL_DISCLAIMER}"
                )

        if not is_mother:
            catalog_to_use = self._filter_catalog_for_child_age(catalog_to_use, age_months)

        requested_ingredients = [
            str(item).strip() for item in cleaned_input.get("requested_ingredients") or [] if str(item).strip()
        ]
        if requested_ingredients:
            ingredient_filtered = [
                dish for dish in catalog_to_use
                if self._dish_contains_all_ingredients(dish, requested_ingredients)
            ]
            if not ingredient_filtered:
                return (
                    f"Không tìm thấy món chứa đủ nguyên liệu {', '.join(requested_ingredients)} "
                    f"trong catalog phù hợp của hồ sơ. Mình không trả về các món đầu danh sách để thay thế. "
                    f"{MEDICAL_DISCLAIMER}"
                )
            catalog_to_use = ingredient_filtered

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
            profile_note = (
                "Theo hồ sơ mẹ đang chọn"
                if cleaned_input.get("uses_selected_profile_data", True)
                else "Theo nhu cầu của mẹ trong câu hỏi hiện tại"
            )
        elif profile_type == "CHILD":
            profile_note = (
                f"Theo hồ sơ bé đang chọn, bé khoảng {int(age_months)} tháng"
                if cleaned_input.get("uses_selected_profile_data", True)
                else f"Theo thông tin trong câu hỏi, bé khoảng {int(age_months)} tháng"
            )
        else:
            profile_note = "Theo hồ sơ đang chọn"

        return (
            f"{profile_note}. Các món phù hợp trong danh sách BabyOi và đã loại món hạn chế của profile{goal_note}:\n{dish_text}\n\n"
            f"{texture} Mỗi bữa nên phối hợp tinh bột, đạm, rau/quả và một lượng chất béo phù hợp. "
            f"Không lấy món từ nhóm khác. {MEDICAL_DISCLAIMER}"
        )

    def _filter_catalog_for_child_age(
        self,
        catalog: list[dict[str, str]],
        age_months: float,
    ) -> list[dict[str, str]]:
        if age_months < 6:
            return []
        expected = (
            "FOR_BABY_6_8_MONTHS" if age_months <= 8
            else "FOR_BABY_9_11_MONTHS" if age_months <= 11
            else "FOR_BABY_12_18_MONTHS" if age_months <= 18
            else "FOR_BABY_19_24_MONTHS" if age_months <= 24
            else ""
        )
        if not expected:
            return catalog
        return [
            dish for dish in catalog
            if str(dish.get("age_group") or "").upper().startswith(expected)
        ]

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
                "ingredients": parts[5] if len(parts) > 5 else "",
                "carbs": parts[6] if len(parts) > 6 else "",
                "fat": parts[7] if len(parts) > 7 else "",
                "fiber": parts[8] if len(parts) > 8 else "",
                "sugar": parts[9] if len(parts) > 9 else "",
                "sodium": parts[10] if len(parts) > 10 else "",
                "good_points": parts[11] if len(parts) > 11 else "",
                "bad_points": parts[12] if len(parts) > 12 else "",
                "advice": parts[13] if len(parts) > 13 else "",
                "cooking_way": parts[14] if len(parts) > 14 else "",
            })
        return foods

    def _catalog_for_filtering(self, cleaned_input: dict[str, Any]) -> list[dict[str, str]]:
        index_items = self._parse_food_catalog(cleaned_input.get("profile_food_catalog_index"))
        detailed_items = self._parse_food_catalog(cleaned_input.get("profile_food_catalog"))
        merged = {item["id"]: item for item in index_items}
        merged.update({item["id"]: item for item in detailed_items})
        return list(merged.values())

    def _dish_contains_all_ingredients(
        self,
        dish: dict[str, str],
        requested_ingredients: list[str],
    ) -> bool:
        dish_ingredients = {
            self._normalize(item.strip())
            for item in str(dish.get("ingredients") or "").split(",")
            if item.strip()
        }
        return all(
            any(self._ingredient_matches(self._normalize(item), actual) for actual in dish_ingredients)
            for item in requested_ingredients
        )

    def _ingredient_matches(self, requested: str, actual: str) -> bool:
        if requested == actual:
            return True
        if requested == "ga":
            return actual == "ga" or any(part in actual for part in ("thit ga", "uc ga", "dui ga", "canh ga", "gan ga"))
        if requested == "bo":
            return actual == "bo" or "thit bo" in actual
        if requested in {"heo", "lon"}:
            return actual == "heo" or "thit heo" in actual or "thit lon" in actual
        if requested == "ca":
            return actual == "ca" or actual.startswith("ca ")
        return False

    def _asks_for_analysis(self, question: str, cleaned_input: dict[str, Any]) -> bool:
        if cleaned_input.get("food_analysis_requested"):
            return True
        normalized = self._normalize(question)
        return any(
            phrase in normalized
            for phrase in (
                "phan tich", "danh gia", "diem manh", "diem yeu", "diem han che",
                "uu diem", "nhuoc diem", "luu y mon", "thong tin chi tiet",
                "chi tiet cua no", "mon nay co gi",
            )
        )

    def _food_analysis(self, cleaned_input: dict[str, Any]) -> str:
        catalog = self._catalog_for_filtering(cleaned_input)
        requested_names = {
            self._normalize(str(item))
            for item in cleaned_input.get("requested_food_names") or []
            if str(item).strip()
        }
        if requested_names:
            catalog = [dish for dish in catalog if self._normalize(dish["name"]) in requested_names]
        if not catalog:
            return (
                "Không tìm thấy món được yêu cầu trong catalog của profile hoặc món chưa có dữ liệu phân tích. "
                f"Mình không tự tạo điểm mạnh/điểm yếu khi thiếu dữ liệu. {MEDICAL_DISCLAIMER}"
            )

        sections: list[str] = []
        for dish in catalog[:3]:
            nutrition = ", ".join(
                value for value in (
                    dish.get("calories"), dish.get("protein"), dish.get("carbs"),
                    dish.get("fat"), dish.get("fiber"), dish.get("sugar"), dish.get("sodium"),
                ) if value
            ) or "chưa có số liệu định lượng"
            good = dish.get("good_points") or "Database chưa có đánh giá điểm mạnh riêng cho món này."
            bad = dish.get("bad_points") or "Database chưa có đánh giá điểm hạn chế riêng cho món này."
            advice = dish.get("advice") or "Chưa có lưu ý sử dụng riêng trong database."
            cooking = dish.get("cooking_way") or "Chưa có hướng dẫn chế biến riêng trong database."
            sections.append(
                f"{dish['name']}\n"
                f"- Nguyên liệu: {dish.get('ingredients') or 'chưa có dữ liệu'}\n"
                f"- Dinh dưỡng ghi nhận: {nutrition}\n"
                f"- Điểm mạnh: {good}\n"
                f"- Điểm hạn chế: {bad}\n"
                f"- Lưu ý: {advice}\n"
                f"- Chế biến: {cooking}"
            )
        return "Phân tích dựa trên dữ liệu món hiện có của BabyOi:\n\n" + "\n\n".join(sections)

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
- Chủ thể duy nhất là patient_type/subject_label đã được QueryUnderstanding giải quyết. selected_profile_type chỉ là profile giao diện, không được ghi đè chủ thể hiện tại.
- Chỉ dùng dữ liệu profile khi uses_selected_profile_data=true. Nếu hỏi chéo mẹ/bé, dùng thông tin được nêu trong câu và catalog chéo; không dùng số đo của profile sai chủ thể.
- Mở đầu nêu profile khi uses_selected_profile_data=true; nếu hỏi chéo thì nói "Theo nhu cầu của mẹ..." hoặc "Theo thông tin bé ... tháng trong câu hỏi...".
- Khi cleaned_input có health_record_date/profile_weight_kg/profile_height_cm, phải nói rõ số đo và ngày đo nếu dùng để cá nhân hóa. Khi có profile_illness_history, chỉ xem đó là lịch sử; không khẳng định bệnh đang tiếp diễn nếu câu hỏi hiện tại không nói.
- `allergies` là danh sách món bị hạn chế của đúng profile: không gợi ý các món này và nêu ngắn gọn rằng đã loại khỏi gợi ý khi liên quan.
- BẮT BUỘC xác định chủ thể tư vấn theo patient_type/profile_type: nếu profile_type là MOTHER (hoặc patient_type='mother') thì câu trả lời nói về MẸ, dùng đại từ "mẹ", KHÔNG nói về "bé/trẻ"; nếu profile_type là CHILD thì câu trả lời nói về BÉ và không áp công thức người lớn.
- Bám sát request_purpose và goal_code của câu hiện tại. Goal/chủ đề hiện tại được phép thay đổi hoàn toàn so với lịch sử.
- Nếu cleaned_input có goal_code/goal_label (ví dụ FOR_MOTHER_DIET = giảm cân, FOR_MOTHER_INCREASE_MILK_SUPPLY = tăng tiết sữa, FOR_MOTHER_HEALTHY_ENERGY = tăng năng lượng/tăng cân lành mạnh, FOR_MOTHER_DIGESTION_RECOVERY = hỗ trợ tiêu hóa, FOR_MOTHER_POSTPARTUM_BREASTFEEDING = sau sinh đang cho con bú, FOR_MOTHER_CHANGE_DIET = đổi món, FOR_MOTHER_SLEEP_STRESS_SUPPORT = giấc ngủ/giảm stress), phải bám sát mục tiêu đó: chỉ chọn món trong profile_food_catalog có age_group == goal_code; nếu không có món nào khớp goal_code thì nói rõ "chưa có món chuyên cho mục tiêu này trong catalog của hồ sơ" và không bịa món.
- Khi đề xuất món cụ thể, CHỈ được dùng tên món xuất hiện trong profile_food_catalog. Không tự tạo món, không lấy món từ context RAG hoặc nhóm tuổi khác. Catalog này đã loại các món profile hạn chế.
- Nếu requested_ingredients có dữ liệu, CHỈ đề xuất món có đủ các nguyên liệu đó trong trường ingredients của profile_food_catalog. Nếu không có món khớp, nói không tìm thấy; tuyệt đối không trả 4–6 món đầu mặc định.
- Nếu có goal_code, CHỈ dùng món có age_group đúng goal_code. Không có món khớp thì nói rõ catalog chưa có; không thay bằng mục tiêu khác.
- Không bao giờ hiển thị goal_code, age_group, type code hoặc chuỗi kỹ thuật `FOR_MOTHER_*`/`FOR_BABY_*` cho người dùng. Chỉ dùng nhãn tiếng Việt tự nhiên.
- Khi người dùng yêu cầu phân tích món, chỉ dùng ingredients, calories/protein/carbs/fat/fiber/sugar/sodium và good_points/bad_points/advice/cooking_way trong catalog. Trình bày Điểm mạnh, Điểm hạn chế, Lưu ý; trường nào thiếu phải nói chưa có dữ liệu, không tự suy diễn.
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
- Nếu profile context có healthRecordDate, cân nặng, chiều cao, BMI, BMR hoặc TDEE thì chỉ dùng đúng các giá trị đó để cá nhân hóa; luôn nêu ngày đo khi dùng số liệu. Nếu có previousHealthRecordDate cùng weightChangeKg/heightChangeCm, đây chỉ là thay đổi giữa hai lần đo, không được kết luận tăng trưởng bình thường, suy dinh dưỡng hay thừa cân nếu chưa có biểu đồ tăng trưởng/đánh giá chuyên môn.
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
