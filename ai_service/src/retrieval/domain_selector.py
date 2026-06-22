from __future__ import annotations

import re
import unicodedata
from typing import Any

from src.constants import (
    DOMAIN_GENERAL,
    DOMAIN_GROWTH,
    DOMAIN_NUTRITION,
    DOMAIN_ROUTINE,
    DOMAIN_VACCINATION,
)


VALID_RETRIEVAL_DOMAINS = {
    DOMAIN_NUTRITION,
    DOMAIN_GROWTH,
    DOMAIN_VACCINATION,
    DOMAIN_ROUTINE,
    DOMAIN_GENERAL,
}

SPECIALIZED_DOMAINS = (DOMAIN_NUTRITION, DOMAIN_GROWTH, DOMAIN_VACCINATION, DOMAIN_ROUTINE)

DOMAIN_KEYWORDS = {
    DOMAIN_NUTRITION: [
        "an dam",
        "bu",
        "sua",
        "dinh duong",
        "thuc don",
        "chao",
        "bot",
        "di ung thuc an",
        "bieng an",
        "an it",
        "an gi",
        "cho an",
        "uong sua",
        "tang khau phan",
        "khau phan",
        "calo",
        "kcal",
        "nang luong",
        "chat dam",
        "protein",
        "tinh bot",
        "chat beo",
        "vitamin",
        "khoang chat",
        "tdee",
        "bmr",
        "bmi",
        "tang can",
        "giam can",
    ],
    DOMAIN_GROWTH: [
        "can nang",
        "nang",
        "chieu cao",
        "cham tang can",
        "nhe can",
        "suy dinh duong",
        "phat trien",
        "moc rang",
        "van dong",
        "biet bo",
        "biet ngoi",
    ],
    DOMAIN_VACCINATION: [
        "vaccine",
        "vac xin",
        "vacxin",
        "tiem chung",
        "lich tiem",
        "mui tiem",
        "sau tiem",
        "phan ung sau tiem",
    ],
    DOMAIN_ROUTINE: [
        "lich sinh hoat",
        "sinh hoat",
        "nep sinh hoat",
        "lich ngu",
        "gio ngu",
        "ngu ngay",
        "ngu dem",
        "giac ngu",
        "thuc giac",
        "cu bu",
        "gio an",
        "tam",
        "choi",
        "van dong",
        "man hinh",
        "routine",
        "schedule",
        "sleep",
        "nap",
        "bedtime",
    ],
}

SYMPTOM_KEYWORDS = [
    "om",
    "be om",
    "benh",
    "benh thuong gap",
    "sot",
    "ho",
    "so mui",
    "nghet mui",
    "viem phoi",
    "tho nhanh",
    "tieu chay",
    "non",
    "tro",
    "phat ban",
    "man do",
    "kho tho",
    "co giat",
    "tim tai",
    "li bi",
    "lu du",
    "bo bu",
    "bu kem",
    "mat nuoc",
    "dau bung",
    "phan mau",
    "dau tai",
    "chay mu tai",
    "do mat",
    "ham ta",
    "vang da",
    "ron do",
    "quay khoc",
]


def select_retrieval_domains(
    query_understanding_result: dict[str, Any],
    safety_result: dict[str, Any],
) -> list[str]:
    return select_retrieval_domains_with_debug(
        query_understanding_result,
        safety_result,
    )["final_selected_domains"]


def select_retrieval_domains_with_debug(
    query_understanding_result: dict[str, Any],
    safety_result: dict[str, Any],
) -> dict[str, Any]:
    original_candidates = _valid_domains(query_understanding_result.get("candidate_domains") or [])
    primary_domain = str(query_understanding_result.get("primary_domain") or "").strip()
    intent = str(query_understanding_result.get("intent") or "").strip()
    safety_level = str(safety_result.get("safety_level") or "").strip()
    text = _combined_query_text(query_understanding_result)
    evidence = _domain_evidence(text)
    symptom_terms = _symptom_terms(text, query_understanding_result)
    has_symptom = bool(symptom_terms)

    if query_understanding_result.get("out_of_scope") or intent == "out_of_scope":
        return _debug(
            original_candidates,
            [],
            "Out-of-scope query. Retrieval disabled.",
            general_added=False,
            evidence=evidence,
            symptom_terms=symptom_terms,
        )

    if safety_level == "emergency":
        return _debug(
            original_candidates,
            [],
            "Emergency safety level. Retrieval skipped.",
            general_added=False,
            evidence=evidence,
            symptom_terms=symptom_terms,
        )

    evidence_domains = [domain for domain in SPECIALIZED_DOMAINS if evidence[domain]]
    if not evidence_domains and primary_domain in SPECIALIZED_DOMAINS:
        evidence_domains = [primary_domain]
    if not evidence_domains and intent in SPECIALIZED_DOMAINS:
        evidence_domains = [intent]

    if (
        DOMAIN_NUTRITION in evidence_domains
        and len(evidence_domains) == 1
        and not has_symptom
        and safety_level != "urgent"
        and (intent == DOMAIN_NUTRITION or primary_domain == DOMAIN_NUTRITION)
    ):
        return _debug(
            original_candidates,
            [DOMAIN_NUTRITION],
            "Single-domain nutrition query without user symptom evidence.",
            general_added=False,
            evidence=evidence,
            symptom_terms=symptom_terms,
        )

    if not evidence_domains:
        if has_symptom or intent in {"symptom", "general_care"}:
            return _debug(
                original_candidates,
                [DOMAIN_GENERAL],
                "General/symptom query without specialized-domain evidence.",
                general_added=True,
                evidence=evidence,
                symptom_terms=symptom_terms,
            )
        return _debug(
            original_candidates,
            [DOMAIN_GENERAL],
            "No clear specialized-domain evidence. Falling back to general.",
            general_added=True,
            evidence=evidence,
            symptom_terms=symptom_terms,
        )

    selected = _ordered_domains(evidence_domains, primary_domain, intent, text)
    general_added = False

    if has_symptom or safety_level == "urgent":
        if DOMAIN_GENERAL not in selected:
            selected.append(DOMAIN_GENERAL)
            general_added = True
        reason = (
            f"{_domain_label(selected[0])} query with symptom/safety evidence, "
            "so general was added for safety context."
        )
    elif len(selected) == 1:
        reason = (
            f"Single-domain {selected[0]} query. No symptom detected, so general was not added."
        )
    else:
        reason = "True multi-domain query with evidence across specialized domains."

    return _debug(
        original_candidates,
        selected[:4],
        reason,
        general_added=general_added,
        evidence=evidence,
        symptom_terms=symptom_terms,
    )


def _combined_query_text(query_understanding_result: dict[str, Any]) -> str:
    parts = [
        _current_question_text(str(query_understanding_result.get("original_input") or "")),
        query_understanding_result.get("cleaned_input"),
        " ".join(str(item) for item in query_understanding_result.get("mentioned_symptoms") or []),
        " ".join(str(item) for item in query_understanding_result.get("mentioned_conditions") or []),
    ]
    return _normalize_search(" ".join(str(part or "") for part in parts))


def _current_question_text(text: str) -> str:
    for marker in ("[Câu hỏi hiện tại]", "[Cau hoi hien tai]"):
        if marker in text:
            return text.rsplit(marker, 1)[1].strip()
    return text


def _domain_evidence(text: str) -> dict[str, list[str]]:
    evidence = {
        domain: [keyword for keyword in keywords if _contains_keyword(text, keyword)]
        for domain, keywords in DOMAIN_KEYWORDS.items()
    }
    if "bo bu" in text:
        evidence[DOMAIN_NUTRITION] = [
            keyword for keyword in evidence[DOMAIN_NUTRITION] if keyword != "bu"
        ]
    return evidence


def _symptom_terms(text: str, query_understanding_result: dict[str, Any]) -> list[str]:
    terms = [keyword for keyword in SYMPTOM_KEYWORDS if _contains_keyword(text, keyword)]
    for item in query_understanding_result.get("mentioned_symptoms") or []:
        normalized = _normalize_search(str(item))
        if normalized and normalized not in terms:
            terms.append(normalized)
    return terms


def _ordered_domains(domains: list[str], primary_domain: str, intent: str, text: str) -> list[str]:
    positions = {
        domain: _first_keyword_position(text, domain)
        for domain in domains
    }
    if any(position is not None for position in positions.values()):
        return sorted(
            domains,
            key=lambda domain: (
                positions[domain] if positions[domain] is not None else 10_000,
                domains.index(domain),
            ),
        )

    ordered: list[str] = []
    for domain in [
        primary_domain,
        intent,
        DOMAIN_GROWTH,
        DOMAIN_NUTRITION,
        DOMAIN_VACCINATION,
        DOMAIN_ROUTINE,
    ]:
        if domain in domains and domain not in ordered:
            ordered.append(domain)
    for domain in domains:
        if domain not in ordered:
            ordered.append(domain)
    return ordered


def _first_keyword_position(text: str, domain: str) -> int | None:
    positions = [
        text.find(keyword)
        for keyword in DOMAIN_KEYWORDS.get(domain, [])
        if keyword in text
    ]
    positions = [position for position in positions if position >= 0]
    return min(positions) if positions else None


def _valid_domains(domains: list[Any]) -> list[str]:
    result: list[str] = []
    for domain in domains:
        normalized = str(domain).strip()
        if normalized in VALID_RETRIEVAL_DOMAINS and normalized not in result:
            result.append(normalized)
    return result


def _contains_keyword(text: str, keyword: str) -> bool:
    pattern = r"(?<![a-z0-9])" + re.escape(keyword) + r"(?![a-z0-9])"
    return re.search(pattern, text) is not None


def _normalize_search(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text.lower())
    without_marks = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return " ".join(without_marks.replace("đ", "d").split())


def _domain_label(domain: str) -> str:
    return {
        DOMAIN_NUTRITION: "Nutrition",
        DOMAIN_GROWTH: "Growth",
        DOMAIN_VACCINATION: "Vaccination",
        DOMAIN_ROUTINE: "Routine",
        DOMAIN_GENERAL: "General",
    }.get(domain, domain)


def _debug(
    original_candidates: list[str],
    final_domains: list[str],
    reason: str,
    general_added: bool,
    evidence: dict[str, list[str]],
    symptom_terms: list[str],
) -> dict[str, Any]:
    return {
        "original_candidate_domains": original_candidates,
        "final_selected_domains": _valid_domains(final_domains),
        "domain_selection_reason": reason,
        "general_added": general_added,
        "domain_keyword_evidence": evidence,
        "symptom_terms": symptom_terms,
    }
