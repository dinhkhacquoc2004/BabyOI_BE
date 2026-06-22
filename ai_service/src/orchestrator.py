from __future__ import annotations

import logging
from typing import Any

from src.agents.answer_validation_agent import AnswerValidationAgent
from src.agents.growth_agent import GrowthAgent
from src.agents.medical_knowledge_retrieval_agent import MedicalKnowledgeRetrievalAgent
from src.agents.nutrition_agent import NutritionAgent
from src.agents.query_understanding_agent import QueryUnderstandingAgent
from src.agents.routine_agent import RoutineAgent
from src.agents.safety_triage_agent import SafetyTriageAgent
from src.agents.vaccination_agent import VaccinationAgent
from src.config import get_settings
from src.constants import (
    DOMAIN_GENERAL,
    DOMAIN_GROWTH,
    DOMAIN_NUTRITION,
    DOMAIN_ROUTINE,
    DOMAIN_VACCINATION,
    MEDICAL_DISCLAIMER,
    NUTRITION_KEYWORDS,
)
from src.gemini_client import GeminiClient
from src.retrieval.domain_selector import select_retrieval_domains_with_debug
from src.retrieval.multi_domain_retriever import MultiDomainRetriever
from src.vector_store import VectorStore


logger = logging.getLogger(__name__)


class AgenticRAGOrchestrator:
    def __init__(self):
        self.settings = get_settings()
        self.gemini_client = GeminiClient(self.settings)

        self.query_understanding = QueryUnderstandingAgent(self.gemini_client)
        self.safety_triage = SafetyTriageAgent(self.gemini_client)
        self.answer_validator = AnswerValidationAgent(self.gemini_client)
        self.multi_domain_retriever = MultiDomainRetriever(self.settings, self.gemini_client)

        self.general_vector_store = VectorStore(
            self.settings,
            collection_name=self.settings.general_collection_name,
        )
        self.nutrition_agent = NutritionAgent(
            self.gemini_client,
            VectorStore(self.settings, collection_name=self.settings.nutrition_collection_name),
        )
        self.growth_agent = GrowthAgent(
            self.gemini_client,
            VectorStore(self.settings, collection_name=self.settings.growth_collection_name),
        )
        self.vaccination_agent = VaccinationAgent(
            self.gemini_client,
            VectorStore(self.settings, collection_name=self.settings.vaccination_collection_name),
        )
        self.routine_agent = RoutineAgent(
            self.gemini_client,
            VectorStore(self.settings, collection_name=self.settings.routine_collection_name),
        )
        self.retrieval_agent = MedicalKnowledgeRetrievalAgent(
            self.gemini_client,
            self.general_vector_store,
        )

    def run(self, user_question: str) -> dict[str, Any]:
        safety_result = self.safety_triage.triage_rule_based(user_question)
        if safety_result["safety_level"] == "emergency":
            query_understanding_result = self.query_understanding.fallback_understanding(user_question)
            cleaned_input = self.query_understanding.to_cleaned_input(query_understanding_result)
            return self._handle_emergency(
                user_question,
                cleaned_input,
                safety_result,
                query_understanding_result=query_understanding_result,
            )

        query_understanding_result = self.query_understanding.understand(user_question)
        cleaned_input = self.query_understanding.to_cleaned_input(query_understanding_result)
        intent_result = self.query_understanding.to_intent_result(query_understanding_result)
        routing_result = self.query_understanding.to_routing_result(query_understanding_result)
        cleaned_question = str(cleaned_input.get("cleaned_question") or user_question)
        intent_query = str(cleaned_input.get("normalized_query_for_retrieval") or cleaned_question)
        logger.info("query_understanding=%s", query_understanding_result)

        candidate_domains = routing_result["candidate_domains"]
        primary_domain = routing_result["primary_domain"]
        domain_selection = select_retrieval_domains_with_debug(
            query_understanding_result,
            safety_result,
        )
        selected_domains = domain_selection["final_selected_domains"]
        if selected_domains and primary_domain not in selected_domains:
            primary_domain = selected_domains[0]
            routing_result["primary_domain"] = primary_domain
        routing_result["selected_domains"] = selected_domains
        routing_result["domain_selection"] = domain_selection
        logger.info(
            "primary_domain=%s candidate_domains=%s selected_domains=%s reason=%s",
            primary_domain,
            candidate_domains,
            selected_domains,
            domain_selection["domain_selection_reason"],
        )

        if intent_result["intent"] == "out_of_scope" or query_understanding_result.get("out_of_scope"):
            return self._handle_out_of_scope(
                user_question,
                cleaned_input,
                intent_result,
                safety_result,
                query_understanding_result=query_understanding_result,
            )

        if intent_result.get("need_retrieval", True):
            retrieval_bundle = self.multi_domain_retriever.retrieve(
                question=intent_query,
                candidate_domains=selected_domains,
                top_k_per_domain=3,
                max_contexts=8,
            )
        else:
            retrieval_bundle = {
                "contexts": [],
                "debug": {
                    "candidate_domains": selected_domains,
                    "selected_collections": {},
                    "retrieved_counts": {},
                    "final_context_count": 0,
                    "errors": {},
                    "fallback_to_general": False,
                },
            }
        contexts = retrieval_bundle["contexts"]
        retrieval_debug = retrieval_bundle["debug"]
        retrieval_debug["domain_selection"] = domain_selection
        retrieval_debug["original_candidate_domains"] = domain_selection["original_candidate_domains"]
        retrieval_debug["final_selected_domains"] = domain_selection["final_selected_domains"]
        retrieval_debug["domain_selection_reason"] = domain_selection["domain_selection_reason"]
        retrieval_debug["general_added"] = domain_selection["general_added"]
        logger.info(
            "selected_collections=%s retrieved_counts=%s final_context_count=%s",
            retrieval_debug.get("selected_collections"),
            retrieval_debug.get("retrieved_counts"),
            retrieval_debug.get("final_context_count"),
        )

        selected_agent = self._select_agent_from_domain(primary_domain)
        if not contexts:
            answer = self._no_context_answer(retrieval_debug)
            agent_result = {
                "agent_name": selected_agent,
                "answer": answer,
                "retrieved_contexts": [],
                "used_collection": "",
                "used_domains": [],
                "confidence": 0.0,
                "needs_more_info": True,
                "missing_fields": cleaned_input.get("missing_important_fields", []),
                "safety_notes": safety_result.get("red_flags", []),
            }
        else:
            agent_result = self._run_selected_agent(
                selected_agent=selected_agent,
                cleaned_input=cleaned_input,
                safety_result=safety_result,
                pre_retrieved_contexts=contexts,
                routing_result=routing_result,
            )
            answer = agent_result["answer"]

        validation_result = self.answer_validator.validate_with_risk_gate(
            user_question=user_question,
            cleaned_input=cleaned_input,
            intent_result=intent_result,
            safety_result=safety_result,
            draft_answer=answer,
            retrieved_contexts=agent_result.get("retrieved_contexts", []),
            agent_result=agent_result,
        )
        logger.info(validation_result.get("validation_decision"))

        return self._build_result(
            cleaned_input=cleaned_input,
            query_understanding_result=query_understanding_result,
            intent_result=intent_result,
            safety_result=safety_result,
            selected_agent=selected_agent,
            draft_answer=answer,
            validation_result=validation_result,
            retrieved_contexts=agent_result.get("retrieved_contexts", []),
            used_collection=agent_result.get("used_collection"),
            agent_result=agent_result,
            routing_result=routing_result,
            retrieval_debug=retrieval_debug,
        )

    def _handle_emergency(
        self,
        user_question: str,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        query_understanding_result: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        emergency_answer = (
            "Mô tả có dấu hiệu nguy hiểm: "
            f"{', '.join(safety_result.get('red_flags', [])) or 'cần đánh giá khẩn cấp'}. "
            f"{safety_result['action']} Không nên chờ chatbot trả lời hoặc tự xử trí tại nhà."
        )
        intent_result = {
            "intent": "emergency",
            "confidence": 1.0,
            "reason": "SafetyTriageAgent phát hiện dấu hiệu emergency.",
            "need_retrieval": False,
        }
        routing_result = {
            "primary_domain": DOMAIN_GENERAL,
            "candidate_domains": [],
            "reason": "Emergency short-circuit, không retrieval.",
            "confidence": 1.0,
        }
        domain_selection = select_retrieval_domains_with_debug(
            query_understanding_result or {"candidate_domains": [], "out_of_scope": False},
            safety_result,
        )
        routing_result["selected_domains"] = []
        routing_result["domain_selection"] = domain_selection
        validation_result = self.answer_validator.validate_with_risk_gate(
            user_question=user_question,
            cleaned_input=cleaned_input,
            intent_result=intent_result,
            safety_result=safety_result,
            draft_answer=emergency_answer,
            retrieved_contexts=[],
            agent_result={},
        )
        return self._build_result(
            cleaned_input=cleaned_input,
            query_understanding_result=query_understanding_result or {},
            intent_result=intent_result,
            safety_result=safety_result,
            selected_agent="safety_triage",
            draft_answer=emergency_answer,
            validation_result=validation_result,
            retrieved_contexts=[],
            used_collection=None,
            routing_result=routing_result,
            retrieval_debug={
                "candidate_domains": [],
                "selected_collections": {},
                "retrieved_counts": {},
                "final_context_count": 0,
                "errors": {},
                "fallback_to_general": False,
                "domain_selection": domain_selection,
                "original_candidate_domains": domain_selection["original_candidate_domains"],
                "final_selected_domains": [],
                "domain_selection_reason": domain_selection["domain_selection_reason"],
                "general_added": False,
            },
        )

    def _handle_out_of_scope(
        self,
        user_question: str,
        cleaned_input: dict[str, Any],
        intent_result: dict[str, Any],
        safety_result: dict[str, Any],
        query_understanding_result: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        answer = f"Câu hỏi này nằm ngoài phạm vi chatbot y khoa mẹ & bé. {MEDICAL_DISCLAIMER}"
        routing_result = {
            "primary_domain": DOMAIN_GENERAL,
            "candidate_domains": [],
            "reason": "Intent out_of_scope, không retrieval.",
            "confidence": intent_result.get("confidence", 0.5),
        }
        domain_selection = select_retrieval_domains_with_debug(
            query_understanding_result or {"candidate_domains": [], "out_of_scope": True},
            safety_result,
        )
        routing_result["selected_domains"] = []
        routing_result["domain_selection"] = domain_selection
        validation_result = self.answer_validator.validate_with_risk_gate(
            user_question=user_question,
            cleaned_input=cleaned_input,
            intent_result=intent_result,
            safety_result=safety_result,
            draft_answer=answer,
            retrieved_contexts=[],
            agent_result={},
        )
        return self._build_result(
            cleaned_input=cleaned_input,
            query_understanding_result=query_understanding_result or {},
            intent_result=intent_result,
            safety_result=safety_result,
            selected_agent="out_of_scope",
            draft_answer=answer,
            validation_result=validation_result,
            retrieved_contexts=[],
            used_collection=None,
            routing_result=routing_result,
            retrieval_debug={
                "candidate_domains": [],
                "selected_collections": {},
                "retrieved_counts": {},
                "final_context_count": 0,
                "errors": {},
                "fallback_to_general": False,
                "domain_selection": domain_selection,
                "original_candidate_domains": domain_selection["original_candidate_domains"],
                "final_selected_domains": [],
                "domain_selection_reason": domain_selection["domain_selection_reason"],
                "general_added": False,
            },
        )

    def _select_agent(self, intent_result: dict[str, Any], cleaned_input: dict[str, Any]) -> str:
        """Backward-compatible single-domain selection."""
        intent = intent_result["intent"]
        normalized_query = str(cleaned_input.get("normalized_query_for_retrieval") or "").lower()
        if intent == "nutrition":
            return "nutrition"
        if intent == "general_care" and any(keyword in normalized_query for keyword in NUTRITION_KEYWORDS):
            return "nutrition"
        if intent == "growth":
            return "growth"
        if intent == "vaccination":
            return "vaccination"
        if intent == "routine":
            return "routine"
        return "general_medical"

    def _select_agent_from_domain(self, primary_domain: str) -> str:
        if primary_domain == DOMAIN_NUTRITION:
            return "nutrition"
        if primary_domain == DOMAIN_GROWTH:
            return "growth"
        if primary_domain == DOMAIN_VACCINATION:
            return "vaccination"
        if primary_domain == DOMAIN_ROUTINE:
            return "routine"
        return "general_medical"

    def _run_selected_agent(
        self,
        selected_agent: str,
        cleaned_input: dict[str, Any],
        safety_result: dict[str, Any],
        pre_retrieved_contexts: list[dict[str, Any]] | None = None,
        routing_result: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        if selected_agent == "nutrition":
            return self.nutrition_agent.answer(
                cleaned_input,
                safety_result,
                pre_retrieved_contexts=pre_retrieved_contexts,
                routing_result=routing_result,
            )
        if selected_agent == "growth":
            return self.growth_agent.answer(
                cleaned_input,
                safety_result,
                pre_retrieved_contexts=pre_retrieved_contexts,
                routing_result=routing_result,
            )
        if selected_agent == "vaccination":
            return self.vaccination_agent.answer(
                cleaned_input,
                safety_result,
                pre_retrieved_contexts=pre_retrieved_contexts,
                routing_result=routing_result,
            )
        if selected_agent == "routine":
            return self.routine_agent.answer(
                cleaned_input,
                safety_result,
                pre_retrieved_contexts=pre_retrieved_contexts,
                routing_result=routing_result,
            )

        if pre_retrieved_contexts is None and not self.general_vector_store.has_documents():
            return {
                "agent_name": "general_medical",
                "answer": (
                    f"Collection '{self.general_vector_store.collection_name}' chưa có tài liệu nội bộ. "
                    f"Cần chạy ingest cho domain general trước. {MEDICAL_DISCLAIMER}"
                ),
                "retrieved_contexts": [],
                "used_collection": self.general_vector_store.collection_name,
                "used_domains": [],
                "confidence": 0.0,
                "needs_more_info": False,
                "missing_fields": cleaned_input.get("missing_important_fields", []),
                "safety_notes": safety_result.get("red_flags", []),
            }

        return self.retrieval_agent.answer(
            user_question=str(
                cleaned_input.get("normalized_query_for_retrieval")
                or cleaned_input.get("cleaned_question")
                or cleaned_input.get("original_question")
            ),
            intent="general_care",
            pre_retrieved_contexts=pre_retrieved_contexts,
            routing_result=routing_result,
            cleaned_input=cleaned_input,
        )

    def _no_context_answer(self, retrieval_debug: dict[str, Any]) -> str:
        return (
            "Trợ lý dinh dưỡng đang cập nhật kho kiến thức nên chưa thể trả lời câu này ngay lúc này. "
            "Bạn vui lòng thử lại sau ít phút. Nếu có triệu chứng bất thường hoặc tình trạng đáng lo, "
            f"hãy liên hệ bác sĩ/cơ sở y tế. {MEDICAL_DISCLAIMER}"
        )

    def _build_result(
        self,
        cleaned_input: dict[str, Any],
        query_understanding_result: dict[str, Any],
        intent_result: dict[str, Any],
        safety_result: dict[str, Any],
        selected_agent: str,
        draft_answer: str,
        validation_result: dict[str, Any],
        retrieved_contexts: list[dict[str, Any]],
        used_collection: str | None,
        agent_result: dict[str, Any] | None = None,
        routing_result: dict[str, Any] | None = None,
        retrieval_debug: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        final_answer = validation_result.get("fixed_answer") or draft_answer
        return {
            "cleaned_input": cleaned_input,
            "query_understanding_result": query_understanding_result,
            "intent_result": intent_result,
            "routing_result": routing_result or {},
            "safety_result": safety_result,
            "selected_agent": selected_agent,
            "answer": final_answer,
            "validation_result": validation_result,
            "retrieved_contexts": retrieved_contexts,
            "used_collection": used_collection,
            "used_domains": self._used_domains(retrieved_contexts),
            "retrieval_debug": retrieval_debug or {},
            "agent_result": agent_result or {},
        }

    def _used_domains(self, contexts: list[dict[str, Any]]) -> list[str]:
        domains: list[str] = []
        for item in contexts:
            metadata = item.get("metadata") or {}
            domain = str(item.get("domain") or metadata.get("domain") or "").strip()
            if domain and domain not in domains:
                domains.append(domain)
        return domains

    def _short_log(self, value: str, max_chars: int = 120) -> str:
        compact = " ".join(value.split())
        return compact if len(compact) <= max_chars else compact[: max_chars - 3] + "..."
