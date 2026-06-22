from __future__ import annotations

from typing import Any

from src.constants import MEDICAL_DISCLAIMER
from src.gemini_client import GeminiClient
from src.vector_store import VectorStore


class BaseSpecializedAgent:
    agent_name: str = "specialized"
    collection_name: str = ""
    required_fields: tuple[str, ...] = ()

    def __init__(self, gemini_client: GeminiClient, vector_store: VectorStore):
        self.gemini_client = gemini_client
        self.vector_store = vector_store
        self.collection_name = vector_store.collection_name

    def answer(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        top_k: int = 4,
        pre_retrieved_contexts: list[dict[str, Any]] | None = None,
        routing_result: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        missing_fields = self._missing_fields(cleaned_input)
        if pre_retrieved_contexts is None and not self.vector_store.has_documents():
            return self._empty_collection_result(missing_fields, safety_result)

        query = str(
            cleaned_input.get("normalized_query_for_retrieval")
            or cleaned_input.get("cleaned_question")
            or cleaned_input.get("original_question")
            or ""
        )
        contexts: list[dict[str, Any]] = pre_retrieved_contexts or []
        try:
            if pre_retrieved_contexts is None:
                query_embedding = self.gemini_client.embed_text(query)
                contexts = self.vector_store.similarity_search(query_embedding, top_k=top_k)
            prompt = self._build_prompt(
                cleaned_input,
                safety_result,
                contexts,
                missing_fields,
                routing_result=routing_result,
            )
            answer = self.gemini_client.generate_text(prompt)
        except Exception as exc:
            answer = self._fallback_answer(cleaned_input, contexts, missing_fields, exc)

        return {
            "agent_name": self.agent_name,
            "answer": self._ensure_disclaimer(answer),
            "retrieved_contexts": contexts,
            "used_collection": self._used_collection(contexts),
            "used_domains": self._used_domains(contexts),
            "confidence": self._confidence(contexts, missing_fields),
            "needs_more_info": bool(missing_fields),
            "missing_fields": missing_fields,
            "safety_notes": safety_result.get("red_flags", []),
        }

    def _build_prompt(
        self,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        routing_result: dict[str, Any] | None = None,
    ) -> str:
        raise NotImplementedError

    def _fallback_answer(
        self,
        cleaned_input: dict[str, Any],
        contexts: list[dict[str, Any]],
        missing_fields: list[str],
        error: Exception,
    ) -> str:
        return (
            "Trợ lý đang bận nên chưa thể tổng hợp câu trả lời lúc này. Bạn vui lòng thử lại sau ít phút. "
            f"Nếu tình trạng đáng lo hoặc có triệu chứng bất thường, hãy liên hệ bác sĩ/cơ sở y tế. {MEDICAL_DISCLAIMER}"
        )

    def _missing_fields(self, cleaned_input: dict[str, Any]) -> list[str]:
        missing = set(cleaned_input.get("missing_important_fields") or [])
        for field in self.required_fields:
            if cleaned_input.get(field) in (None, "", []):
                missing.add(field)
        return sorted(missing)

    def _empty_collection_result(
        self,
        missing_fields: list[str],
        safety_result: dict[str, Any],
    ) -> dict[str, Any]:
        answer = (
            "Kho kiến thức của trợ lý đang được cập nhật. Bạn vui lòng thử lại sau ít phút. "
            f"{MEDICAL_DISCLAIMER}"
        )
        return {
            "agent_name": self.agent_name,
            "answer": answer,
            "retrieved_contexts": [],
            "used_collection": self.collection_name,
            "confidence": 0.0,
            "needs_more_info": bool(missing_fields),
            "missing_fields": missing_fields,
            "safety_notes": safety_result.get("red_flags", []),
        }

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
        return "\n\n".join(lines) if lines else "(không có context)"

    def _ensure_disclaimer(self, answer: str) -> str:
        if MEDICAL_DISCLAIMER.lower() in answer.lower():
            return answer
        return f"{answer.rstrip()}\n\n{MEDICAL_DISCLAIMER}"

    def _confidence(self, contexts: list[dict[str, Any]], missing_fields: list[str]) -> float:
        if not contexts:
            return 0.0
        base = 0.72
        penalty = min(len(missing_fields) * 0.08, 0.32)
        return round(max(0.2, base - penalty), 2)

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
        return ", ".join(collections) if collections else self.collection_name
