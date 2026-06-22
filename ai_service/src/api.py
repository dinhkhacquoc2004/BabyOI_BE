from __future__ import annotations

import logging
import os
import re
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI, HTTPException, Request, status
from pydantic import BaseModel, Field

from src.api_debug_formatter import format_debug_log
from src.config import get_settings
from src.constants import DOMAIN_COLLECTION_SETTING_ATTRS, MEDICAL_DISCLAIMER
from src.ingest import SUPPORTED_DOMAINS, ensure_domains_ingested
from src.orchestrator import AgenticRAGOrchestrator
from src.vector_store import VectorStore


logger = logging.getLogger(__name__)


class ChatHistoryItem(BaseModel):
    sender: str
    message: str


class ChatApiRequest(BaseModel):
    conversationId: str = Field(..., min_length=1)
    userName: str = Field(..., min_length=1)
    message: str
    profileContext: dict[str, Any] | None = None
    chatHistory: list[ChatHistoryItem] | None = None


class ChatApiResponse(BaseModel):
    conversationId: str
    userName: str
    answer: str
    botReply: str
    userMessage: str
    selectedAgent: str
    intent: str
    safetyLevel: str
    usedDomains: list[str]
    debugLog: str
    rawResult: dict[str, Any]


@asynccontextmanager
async def lifespan(app: FastAPI):
    logging.basicConfig(level=logging.INFO)
    logger.info("Starting Agentic RAG API and preparing knowledge collections.")
    app.state.knowledge_status = _prepare_knowledge()
    app.state.orchestrator = AgenticRAGOrchestrator()
    try:
        yield
    finally:
        logger.info("Stopping Agentic RAG API.")


app = FastAPI(
    title="AgenticRAG API",
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/health")
def health(request: Request) -> dict[str, Any]:
    knowledge = getattr(request.app.state, "knowledge_status", {})
    ready = bool(knowledge) and all(item.get("documents", 0) > 0 for item in knowledge.values())
    return {"status": "ok" if ready else "degraded", "knowledge": knowledge}


def _prepare_knowledge() -> dict[str, dict[str, Any]]:
    configured_domains = os.getenv(
        "AUTO_INGEST_DOMAINS",
        "nutrition,growth,vaccination,routine,general",
    )
    domains = tuple(
        domain.strip()
        for domain in configured_domains.split(",")
        if domain.strip() in SUPPORTED_DOMAINS
    )
    if not domains or os.getenv("AUTO_INGEST_ON_STARTUP", "true").lower() not in {"1", "true", "yes", "on"}:
        return {}

    for domain in domains:
        try:
            ensure_domains_ingested((domain,))
        except Exception:
            logger.exception("Automatic knowledge ingest failed for domain %s.", domain)

    status: dict[str, dict[str, Any]] = {}
    settings = get_settings(validate_api_key=False)
    for domain in domains:
        attr_name = DOMAIN_COLLECTION_SETTING_ATTRS[domain]
        collection_name = str(getattr(settings, attr_name))
        vector_store = VectorStore(settings, collection_name=collection_name)
        status[domain] = {
            "collection": collection_name,
            "documents": vector_store.collection.count(),
        }
    return status


@app.post("/api/chat", response_model=ChatApiResponse)
def chat(request_body: ChatApiRequest, request: Request) -> ChatApiResponse:
    message = request_body.message.strip()
    if not message:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Message must not be empty.",
        )

    orchestrator: AgenticRAGOrchestrator = request.app.state.orchestrator
    orchestrator_message = build_enriched_message(request_body, message)

    try:
        result = orchestrator.run(orchestrator_message)
    except Exception:
        logger.exception("Agentic RAG orchestrator failed while handling chat request.")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Agentic RAG service could not process this message. Please try again later.",
        )

    try:
        user_answer = build_user_friendly_answer(result)
        debug_log = format_debug_log(result)
    except Exception:
        logger.exception("Agentic RAG orchestrator returned an invalid result shape.")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Agentic RAG service returned an invalid result. Please try again later.",
        )

    if not user_answer:
        logger.error("Agentic RAG orchestrator returned an empty answer.")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Agentic RAG service returned an empty answer. Please try again later.",
        )

    intent_result = _dict_value(result.get("intent_result"))
    safety_result = _dict_value(result.get("safety_result"))
    logger.info("AgenticRAG debug result:\n%s", debug_log)

    return ChatApiResponse(
        conversationId=request_body.conversationId,
        userName=request_body.userName,
        answer=user_answer,
        botReply=user_answer,
        userMessage=message,
        selectedAgent=str(result.get("selected_agent") or "unknown"),
        intent=str(intent_result.get("intent") or "unknown"),
        safetyLevel=str(safety_result.get("safety_level") or "unknown"),
        usedDomains=_string_list(result.get("used_domains")),
        debugLog=debug_log,
        rawResult=result,
    )


def _dict_value(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _string_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(item) for item in value if str(item).strip()]


def build_enriched_message(request_body: ChatApiRequest, message: str) -> str:
    profile_lines = _profile_context_lines(request_body.profileContext)
    history_lines = _chat_history_lines(request_body.chatHistory)

    if not profile_lines and not history_lines:
        return message

    sections: list[str] = []
    if profile_lines:
        sections.append("[Thông tin hồ sơ]\n" + "\n".join(profile_lines))
    if history_lines:
        sections.append("[Lịch sử hội thoại gần đây]\n" + "\n".join(history_lines))
    sections.append("[Câu hỏi hiện tại]\n" + message)
    return "\n\n".join(sections)


def _profile_context_lines(profile_context: dict[str, Any] | None) -> list[str]:
    if not profile_context:
        return []

    # Friendly Vietnamese labels for display, but we ALSO emit the original key
    # (e.g. "- profileType: MOTHER") so downstream agents can parse profile fields
    # reliably via the original camelCase keys regardless of label translations.
    field_labels = (
        ("name", "Tên"),
        ("fullName", "Tên"),
        ("displayName", "Tên"),
        ("birthDate", "Ngày sinh"),
        ("dateOfBirth", "Ngày sinh"),
        ("dob", "Ngày sinh"),
        ("gender", "Giới tính"),
        ("sex", "Giới tính"),
        ("profileType", "Loại hồ sơ"),
        ("type", "Loại hồ sơ"),
    )
    lines: list[str] = []
    used_labels: set[str] = set()
    emitted_keys: set[str] = set()

    for key, label in field_labels:
        value = _clean_context_value(profile_context.get(key))
        if not value:
            continue
        if label not in used_labels:
            lines.append(f"- {label}: {value}")
            used_labels.add(label)
        # Always also emit the raw key form so structured parsers can pick it up.
        lines.append(f"- {key}: {value}")
        emitted_keys.add(key)

    for key, value in profile_context.items():
        if key in emitted_keys:
            continue
        clean_value = _clean_context_value(value)
        if clean_value:
            lines.append(f"- {key}: {clean_value}")

    return lines


def _chat_history_lines(chat_history: list[ChatHistoryItem] | None) -> list[str]:
    if not chat_history:
        return []

    lines: list[str] = []
    for item in chat_history[-6:]:
        sender = item.sender.strip().upper() or "UNKNOWN"
        history_message = item.message.strip()
        if history_message:
            lines.append(f"{sender}: {history_message}")
    return lines


def _clean_context_value(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, (dict, list)):
        return ""
    return str(value).strip()


def build_user_friendly_answer(result: dict) -> str:
    raw_answer = str(result["answer"] or "").strip()
    safety_level = str(_dict_value(result.get("safety_result")).get("safety_level") or "").lower()

    answer = _extract_answer_section(raw_answer)
    answer = _remove_source_section(answer)
    answer = _remove_debug_lines(answer)
    answer = _remove_internal_context_sections(answer)
    answer = _normalize_blank_lines(answer)
    answer = _preserve_disclaimer(answer, raw_answer)

    if safety_level == "emergency":
        return answer.strip()

    if _is_too_long(answer) and str(result.get("selected_agent") or "") != "nutrition":
        answer = _summarize_without_new_medical_content(answer)
        answer = _preserve_disclaimer(answer, raw_answer)

    return answer.strip()


def _extract_answer_section(text: str) -> str:
    if "Answer:" not in text:
        return text

    after_answer = text.split("Answer:", 1)[1]
    if "Retrieved topics:" in after_answer:
        after_answer = after_answer.split("Retrieved topics:", 1)[0]
    return after_answer.strip()


def _remove_source_section(text: str) -> str:
    source_markers = (
        "Nguồn nội bộ đã dùng:",
        "Nguồn nội bộ đã dùng",
        "Nguon noi bo da dung:",
        "Nguon noi bo da dung",
    )

    earliest_index: int | None = None
    for marker in source_markers:
        index = text.lower().find(marker.lower())
        if index != -1 and (earliest_index is None or index < earliest_index):
            earliest_index = index

    if earliest_index is None:
        return text

    before_sources = text[:earliest_index].rstrip()
    before_sources = re.sub(r"\n\s*(?:\*\*)?\d+\.\s*(?:\*\*)?\s*$", "", before_sources).rstrip()
    after_sources = text[earliest_index:]
    disclaimer = _find_disclaimer(after_sources)
    return f"{before_sources}\n\n{disclaimer}".strip() if disclaimer else before_sources


def _remove_debug_lines(text: str) -> str:
    debug_prefixes = (
        "--- Kết quả ---",
        "Intent:",
        "Query understanding:",
        "Safety level:",
        "Selected agent:",
        "Candidate domains:",
        "Final selected domains:",
        "Domain selection reason:",
        "Used domains:",
        "Used collection:",
        "Selected collections:",
        "Retrieved counts:",
        "Missing important fields:",
        "Validation issues:",
        "Validation decision:",
        "Retrieved topics:",
        "Lỗi kỹ thuật:",
        "Loi ky thuat:",
    )

    kept_lines = []
    for line in text.splitlines():
        stripped = line.strip()
        if any(stripped.startswith(prefix) for prefix in debug_prefixes):
            continue
        kept_lines.append(line)
    return "\n".join(kept_lines)


def _remove_internal_context_sections(text: str) -> str:
    internal_headers = {
        "[Thông tin hồ sơ]",
        "[Lịch sử hội thoại gần đây]",
        "[Câu hỏi hiện tại]",
    }
    kept_lines: list[str] = []
    skipping_internal_section = False

    for line in text.splitlines():
        stripped = line.strip()
        if any(stripped.startswith(header) for header in internal_headers):
            skipping_internal_section = True
            continue
        if skipping_internal_section:
            if not stripped:
                skipping_internal_section = False
            continue
        kept_lines.append(line)

    return "\n".join(kept_lines)


def _is_too_long(text: str) -> bool:
    words = text.split()
    meaningful_lines = [line for line in text.splitlines() if line.strip()]
    return len(words) > 180 or len(meaningful_lines) > 10


def _summarize_without_new_medical_content(text: str) -> str:
    disclaimer = _find_disclaimer(text)
    body = text.replace(disclaimer, "").strip() if disclaimer else text.strip()
    sentences = _split_sentences(body)
    selected = [sentence for sentence in sentences if sentence][:6]

    if not selected:
        summary = body
    else:
        summary = "\n".join(f"- {sentence}" for sentence in selected)

    return f"{summary}\n\n{disclaimer}".strip() if disclaimer else summary.strip()


def _split_sentences(text: str) -> list[str]:
    normalized = " ".join(text.split())
    if not normalized:
        return []

    sentences: list[str] = []
    start = 0
    for index, char in enumerate(normalized):
        if char not in ".!?":
            continue
        sentence = normalized[start : index + 1].strip()
        if sentence:
            sentences.append(sentence)
        start = index + 1

    tail = normalized[start:].strip()
    if tail:
        sentences.append(tail)

    return sentences


def _find_disclaimer(text: str) -> str:
    if MEDICAL_DISCLAIMER in text:
        return MEDICAL_DISCLAIMER

    for line in reversed(text.splitlines()):
        stripped = line.strip()
        lowered = stripped.lower()
        if "tham khảo" in lowered and ("không thay thế" in lowered or "bác sĩ" in lowered):
            return stripped

    return ""


def _preserve_disclaimer(answer: str, original_answer: str) -> str:
    disclaimer = _find_disclaimer(original_answer)
    if not disclaimer or disclaimer in answer:
        return answer
    return f"{answer.rstrip()}\n\n{disclaimer}"


def _normalize_blank_lines(text: str) -> str:
    lines = text.splitlines()
    normalized: list[str] = []
    previous_blank = False

    for line in lines:
        is_blank = not line.strip()
        if is_blank and previous_blank:
            continue
        normalized.append(line.rstrip())
        previous_blank = is_blank

    return "\n".join(normalized).strip()
