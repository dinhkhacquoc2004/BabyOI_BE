from __future__ import annotations

import hashlib
from typing import Any

from src.config import Settings
from src.constants import DOMAIN_COLLECTION_SETTING_ATTRS, DOMAIN_GENERAL, SUPPORTED_DOMAINS
from src.gemini_client import GeminiClient
from src.vector_store import VectorStore


class MultiDomainRetriever:
    def __init__(self, settings: Settings, gemini_client: GeminiClient):
        self.settings = settings
        self.gemini_client = gemini_client

    def retrieve(
        self,
        question: str,
        candidate_domains: list[str],
        top_k_per_domain: int = 3,
        max_contexts: int = 8,
        fallback_to_general: bool = False,
    ) -> dict[str, Any]:
        domains = self._normalize_domains(candidate_domains)
        if not domains:
            return {
                "contexts": [],
                "debug": {
                    "candidate_domains": [],
                    "selected_collections": {},
                    "retrieved_counts": {},
                    "final_context_count": 0,
                    "errors": {},
                    "fallback_to_general": False,
                },
            }

        query_embedding = self.gemini_client.embed_text(question)

        contexts: list[dict[str, Any]] = []
        per_domain_counts: dict[str, int] = {}
        selected_collections: dict[str, str] = {}
        errors: dict[str, str] = {}

        for domain in domains:
            collection_name = self._collection_name_for_domain(domain)
            selected_collections[domain] = collection_name
            try:
                vector_store = VectorStore(self.settings, collection_name=collection_name)
                if not vector_store.has_documents():
                    per_domain_counts[domain] = 0
                    errors[domain] = f"Collection '{collection_name}' chưa có dữ liệu."
                    continue
                raw_items = vector_store.similarity_search(query_embedding, top_k=top_k_per_domain)
                normalized_items = [
                    self._normalize_context(item, domain, collection_name)
                    for item in raw_items
                ]
                contexts.extend(normalized_items)
                per_domain_counts[domain] = len(normalized_items)
            except Exception as exc:
                per_domain_counts[domain] = 0
                errors[domain] = str(exc)

        if fallback_to_general and not contexts and DOMAIN_GENERAL not in domains:
            fallback = self.retrieve(
                question=question,
                candidate_domains=[DOMAIN_GENERAL],
                top_k_per_domain=top_k_per_domain,
                max_contexts=max_contexts,
                fallback_to_general=False,
            )
            fallback["debug"]["fallback_to_general"] = True
            return fallback

        merged_contexts = self._merge_and_rerank(contexts, max_contexts=max_contexts)
        return {
            "contexts": merged_contexts,
            "debug": {
                "candidate_domains": domains,
                "selected_collections": selected_collections,
                "retrieved_counts": per_domain_counts,
                "final_context_count": len(merged_contexts),
                "errors": errors,
                "fallback_to_general": False,
            },
        }

    def _normalize_domains(self, domains: list[str]) -> list[str]:
        result: list[str] = []
        for domain in domains:
            normalized = str(domain).strip()
            if normalized in SUPPORTED_DOMAINS and normalized not in result:
                result.append(normalized)
        return result

    def _collection_name_for_domain(self, domain: str) -> str:
        attr_name = DOMAIN_COLLECTION_SETTING_ATTRS[domain]
        return str(getattr(self.settings, attr_name))

    def _normalize_context(
        self,
        item: dict[str, Any],
        domain: str,
        collection_name: str,
    ) -> dict[str, Any]:
        metadata = item.get("metadata") or {}
        text = str(item.get("document") or item.get("text") or "")
        distance = item.get("distance")
        score = self._score_from_distance(distance)
        normalized_metadata = {
            **metadata,
            "domain": metadata.get("domain") or domain,
            "source": metadata.get("source"),
            "topic": metadata.get("topic"),
            "collection": collection_name,
        }
        return {
            "id": item.get("id"),
            "text": text,
            "document": text,
            "domain": normalized_metadata["domain"],
            "source": normalized_metadata["source"],
            "topic": normalized_metadata["topic"],
            "score": score,
            "distance": distance,
            "metadata": normalized_metadata,
            "collection": collection_name,
        }

    def _score_from_distance(self, distance: Any) -> float:
        try:
            numeric_distance = float(distance)
        except (TypeError, ValueError):
            return 0.0
        return round(1.0 / (1.0 + max(numeric_distance, 0.0)), 6)

    def _merge_and_rerank(
        self,
        contexts: list[dict[str, Any]],
        max_contexts: int,
    ) -> list[dict[str, Any]]:
        seen: set[str] = set()
        deduped: list[dict[str, Any]] = []
        for context in contexts:
            key = self._dedupe_key(context)
            if key in seen:
                continue
            seen.add(key)
            deduped.append(context)

        deduped.sort(key=lambda item: item.get("score", 0.0), reverse=True)
        return deduped[:max_contexts]

    def _dedupe_key(self, context: dict[str, Any]) -> str:
        doc_id = context.get("id")
        domain = context.get("domain")
        if doc_id:
            return f"{domain}:{doc_id}"
        text = str(context.get("text") or "")
        return hashlib.sha256(text.encode("utf-8")).hexdigest()
