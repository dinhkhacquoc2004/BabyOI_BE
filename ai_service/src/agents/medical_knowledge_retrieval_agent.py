from __future__ import annotations

from typing import Any

from src.gemini_client import GeminiClient
from src.vector_store import VectorStore


class MedicalKnowledgeRetrievalAgent:
    def __init__(self, gemini_client: GeminiClient, vector_store: VectorStore):
        self.gemini_client = gemini_client
        self.vector_store = vector_store

    def answer(
        self,
        user_question: str,
        intent: str,
        top_k: int = 4,
        pre_retrieved_contexts: list[dict[str, Any]] | None = None,
        routing_result: dict[str, Any] | None = None,
        cleaned_input: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        if pre_retrieved_contexts is None:
            query_embedding = self.gemini_client.embed_text(user_question)
            contexts = self.vector_store.similarity_search(query_embedding, top_k=top_k)
        else:
            contexts = pre_retrieved_contexts
        context_text = self._format_context(contexts)

        cleaned_input = cleaned_input or {}
        profile_type = str(cleaned_input.get("profile_type") or "").strip().upper()
        is_mother = profile_type in {"MOTHER", "MOM", "M", "MẸ"}
        subject_label = str(cleaned_input.get("subject_label") or ("mẹ" if is_mother else "bé"))
        symptoms = cleaned_input.get("main_symptoms") or []
        age_months = cleaned_input.get("profile_age_months") or cleaned_input.get("child_age_months")
        profile_name = cleaned_input.get("profile_name") or subject_label
        health_record_date = cleaned_input.get("health_record_date")
        weight_kg = cleaned_input.get("profile_weight_kg")
        height_cm = cleaned_input.get("profile_height_cm")
        illness_history = cleaned_input.get("profile_illness_history")
        allergies = cleaned_input.get("allergies") or []
        subject_block = (
            f"Chủ thể câu hỏi: {subject_label.upper()} {profile_name} "
            f"(profile_type={profile_type or 'UNKNOWN'}, patient_type={cleaned_input.get('patient_type') or 'baby'}, "
            f"selected_profile_type={cleaned_input.get('selected_profile_type')}, "
            f"uses_selected_profile_data={cleaned_input.get('uses_selected_profile_data')}, "
            f"request_purpose={cleaned_input.get('request_purpose')}, "
            f"age_months={age_months}, health_record_date={health_record_date}, weight_kg={weight_kg}, "
            f"height_cm={height_cm}, restricted_foods={allergies}). "
            f"Lịch sử ốm gần đây trong hồ sơ: {illness_history or 'không có dữ liệu'}. "
            f"Triệu chứng người dùng nêu: {symptoms or 'chưa nêu cụ thể'}."
        )

        prompt = f"""
Bạn là Medical Knowledge Retrieval Agent cho app Baby Ơi. Bạn trả lời câu hỏi chăm sóc mẹ & bé dựa trên tài liệu y khoa nội bộ đã được retrieval.

Quy tắc bắt buộc:
1. Trả lời bằng tiếng Việt, dễ hiểu cho người dùng.
2. BẮT BUỘC xác định chủ thể theo subject_block: nếu chủ thể là MẸ thì câu trả lời nói về mẹ và không nhầm sang bé; nếu là BÉ thì nói về bé.
2a. Mở đầu phải nêu rõ đang tư vấn theo đúng hồ sơ nào và tuổi tháng, ví dụ "Theo hồ sơ bé An, hiện 7 tháng...". Nếu dùng cân nặng/chiều cao phải kèm ngày đo; nếu hồ sơ không có dữ liệu thì nói rõ thiếu, không tự đoán.
2b. Khi uses_selected_profile_data=true, dữ liệu profile trong subject_block là nguồn chuẩn. Khi uses_selected_profile_data=false, chỉ dùng dữ liệu user nêu cho chủ thể hiện tại và hỏi thêm phần thiếu. Lịch sử ốm chỉ là bối cảnh, không được coi là triệu chứng hiện tại nếu user không nói hoặc sự kiện đã kết thúc.
2c. patient_type/subject_label là chủ thể đã được giải quyết cho CÂU HIỆN TẠI. selected_profile_type chỉ là profile giao diện. Nếu uses_selected_profile_data=false, không được suy ra dữ liệu từ profile đang chọn.
2d. Bám sát request_purpose và câu hỏi hiện tại; không mang bệnh, mục tiêu dinh dưỡng hoặc chủ thể từ câu trước sang nếu user đã đổi chủ đề rõ ràng.
3. Khi user nói chung "bị ốm/bị bệnh" mà chưa rõ triệu chứng, hãy hỏi lại 2-3 câu cụ thể (sốt bao nhiêu độ, có ho/sổ mũi/nôn/tiêu chảy không, bắt đầu khi nào, ăn/bú/ngủ ra sao) ĐỒNG THỜI đưa ra hướng dẫn theo dõi và xử trí ban đầu dựa trên context nội bộ (sốt, ho, tiêu chảy, nôn trớ, nghẹt mũi…). KHÔNG được chỉ nói chung chung "hãy đi khám bác sĩ".
4. Trả lời có cấu trúc rõ: (a) Nhận định ngắn theo dữ liệu user đã nêu; (b) Câu hỏi cần làm rõ thêm; (c) Hướng dẫn theo dõi/xử trí tại nhà dựa trên context; (d) Dấu hiệu cần đi khám ngay; (e) Nguồn nội bộ đã dùng.
5. Chỉ dựa trên context tìm được. Nếu context không đủ thì nói rõ phần nào chưa đủ dữ liệu rồi mới khuyên hỏi bác sĩ.
6. Không chẩn đoán chắc chắn, không kê đơn thuốc/liều thuốc.
7. Luôn nhắc nội dung chỉ mang tính tham khảo, không thay thế bác sĩ.
8. Context có thể đến từ nhiều domain. Nếu mâu thuẫn, ưu tiên an toàn y khoa.

{subject_block}

Intent: {intent}
Routing result: {routing_result}
Câu hỏi người dùng: {user_question}

Context nội bộ:
{context_text}
""".strip()

        try:
            answer = self.gemini_client.generate_text(prompt)
        except Exception as exc:
            answer = self._fallback_answer(user_question, contexts, exc, cleaned_input)
        return {
            "agent_name": "general_medical",
            "answer": answer,
            "retrieved_contexts": contexts,
            "used_collection": self._used_collection(contexts),
            "used_domains": self._used_domains(contexts),
            "confidence": 0.65 if contexts else 0.0,
            "needs_more_info": False,
            "missing_fields": [],
            "safety_notes": [],
        }

    def _fallback_answer(
        self,
        user_question: str,
        contexts: list[dict[str, Any]],
        error: Exception,
        cleaned_input: dict[str, Any] | None = None,
    ) -> str:
        cleaned_input = cleaned_input or {}
        profile_intro = self._profile_intro(cleaned_input)
        bullets = self._context_bullets(contexts, limit=5)
        if not bullets:
            return (
                f"{profile_intro}Mình chưa có đủ dữ liệu nội bộ để tư vấn an toàn cho câu hỏi này. "
                "Nếu bé có dấu hiệu bất thường như khó thở, tím tái, co giật, li bì, bỏ bú, nôn liên tục, "
                "tiêu chảy có máu hoặc mất nước, bạn nên đưa bé đi khám/cấp cứu ngay. "
                "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."
            )

        bullet_text = "\n".join(f"- {item}" for item in bullets)
        return (
            f"{profile_intro}Dựa trên kho kiến thức sức khỏe nội bộ theo hướng WHO/UNICEF, bạn có thể tham khảo:\n"
            f"{bullet_text}\n\n"
            "Theo dõi thêm: nhiệt độ, nhịp thở khi bé nằm yên, mức tỉnh táo, ăn/bú/uống, số lần tiểu, "
            "số lần nôn/đi ngoài và thời điểm triệu chứng bắt đầu. Nếu có khó thở, tím tái, co giật, "
            "li bì/khó đánh thức, bỏ bú hoàn toàn, nôn mọi thứ, phân có máu hoặc dấu mất nước, cần đi khám/cấp cứu ngay. "
            "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."
        )

    def _profile_intro(self, cleaned_input: dict[str, Any]) -> str:
        is_mother = str(cleaned_input.get("profile_type") or "").strip().upper() in {
            "MOTHER", "MOM", "M", "MẸ"
        }
        name = str(cleaned_input.get("profile_name") or ("mẹ" if is_mother else "bé")).strip()
        age_months = cleaned_input.get("profile_age_months")
        subject = f"mẹ {name}" if is_mother else f"bé {name}"
        age_text = ""
        if not is_mother and isinstance(age_months, (int, float)):
            age_text = f", hiện {age_months:g} tháng"

        measurement_parts: list[str] = []
        weight = cleaned_input.get("profile_weight_kg")
        height = cleaned_input.get("profile_height_cm")
        record_date = cleaned_input.get("health_record_date")
        if isinstance(weight, (int, float)):
            measurement_parts.append(f"{weight:g} kg")
        if isinstance(height, (int, float)):
            measurement_parts.append(f"{height:g} cm")
        measurement_text = ""
        if measurement_parts:
            date_text = f" ngày {record_date}" if record_date else ""
            measurement_text = f" Số đo gần nhất{date_text}: {', '.join(measurement_parts)}."

        restricted = cleaned_input.get("allergies") or []
        restricted_text = ""
        if restricted:
            restricted_text = f" Hồ sơ đang hạn chế: {', '.join(str(item) for item in restricted)}."
        return f"Theo hồ sơ {subject}{age_text}.{measurement_text}{restricted_text}\n\n"

    def _format_context(self, contexts: list[dict[str, Any]]) -> str:
        lines = []
        for index, item in enumerate(contexts, start=1):
            metadata = item.get("metadata") or {}
            text = item.get("document") or item.get("text") or ""
            domain = item.get("domain") or metadata.get("domain")
            source = item.get("source") or metadata.get("source")
            topic = item.get("topic") or metadata.get("topic")
            score = item.get("score")
            distance = item.get("distance")
            lines.append(
                f"[{index}] [Domain: {domain} | Source: {source} | Topic: {topic} | "
                f"Score: {score} | Distance: {distance}]\n{text}"
            )
        return "\n\n".join(lines)

    def _used_domains(self, contexts: list[dict[str, Any]]) -> list[str]:
        domains: list[str] = []
        for item in contexts:
            metadata = item.get("metadata") or {}
            domain = str(item.get("domain") or metadata.get("domain") or "").strip()
            if domain and domain not in domains:
                domains.append(domain)
        return domains

    def _used_collection(self, contexts: list[dict[str, Any]]) -> str:
        collections: list[str] = []
        for item in contexts:
            metadata = item.get("metadata") or {}
            collection = str(item.get("collection") or metadata.get("collection") or "").strip()
            if collection and collection not in collections:
                collections.append(collection)
        return ", ".join(collections) if collections else self.vector_store.collection_name

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
