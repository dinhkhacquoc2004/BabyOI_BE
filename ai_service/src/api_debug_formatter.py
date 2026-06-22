from __future__ import annotations

from typing import Any


def format_debug_log(result: dict) -> str:
    intent = result["intent_result"]["intent"]
    safety_level = result["safety_result"]["safety_level"]
    selected_agent = result.get("selected_agent", "unknown")
    used_collection = result.get("used_collection")
    routing_result = _dict_value(result.get("routing_result"))
    query_understanding = _dict_value(result.get("query_understanding_result"))
    retrieval_debug = _dict_value(result.get("retrieval_debug"))
    candidate_domains = _string_list(routing_result.get("candidate_domains"))
    used_domains = _string_list(result.get("used_domains"))
    selected_collections = retrieval_debug.get("selected_collections")
    retrieved_counts = retrieval_debug.get("retrieved_counts")
    final_selected_domains = _string_list(retrieval_debug.get("final_selected_domains"))
    domain_selection_reason = str(retrieval_debug.get("domain_selection_reason") or "")
    missing_fields = _string_list(
        _dict_value(result.get("cleaned_input")).get("missing_important_fields")
    )
    validation_result = _dict_value(result.get("validation_result"))
    validation_issues = _string_list(validation_result.get("issues"))
    validation_decision = str(validation_result.get("validation_decision") or "")

    topics: list[str] = []
    for item in result.get("retrieved_contexts", []):
        if not isinstance(item, dict):
            continue
        metadata = _dict_value(item.get("metadata"))
        topic = metadata.get("topic")
        if topic and topic not in topics:
            topics.append(str(topic))

    lines = [
        "--- Kết quả ---",
        f"Intent: {intent}",
        f"Query understanding: {query_understanding or '(none)'}",
        f"Safety level: {safety_level}",
        f"Selected agent: {selected_agent}",
        f"Final selected domains: {_join_or(final_selected_domains, '(none)')}",
        f"Domain selection reason: {domain_selection_reason or '(none)'}",
        f"Candidate domains: {_join_or(candidate_domains, '(không có)')}",
        f"Used domains: {_join_or(used_domains, '(không dùng retrieval)')}",
        f"Used collection: {used_collection or '(không dùng retrieval)'}",
        f"Selected collections: {selected_collections or '(không có)'}",
        f"Retrieved counts: {retrieved_counts or '(không có)'}",
        f"Missing important fields: {_join_or(missing_fields, '(không có)')}",
        f"Validation issues: {_join_or(validation_issues, '(không có)', separator='; ')}",
        f"Validation decision: {validation_decision or '(none)'}",
        "",
        "Answer:",
        str(result["answer"]),
        "",
        "Retrieved topics:",
        _join_or(topics, "(không dùng retrieval)"),
    ]

    return "\n".join(lines)


def _dict_value(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _string_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(item) for item in value if str(item).strip()]


def _join_or(values: list[str], fallback: str, separator: str = ", ") -> str:
    return separator.join(values) if values else fallback
