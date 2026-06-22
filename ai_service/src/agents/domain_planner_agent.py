from __future__ import annotations

from typing import Any

from src.constants import (
    DOMAIN_GENERAL,
    DOMAIN_GROWTH,
    DOMAIN_NUTRITION,
    DOMAIN_ROUTINE,
    DOMAIN_VACCINATION,
    GENERAL_SAFETY_KEYWORDS,
    GROWTH_KEYWORDS,
    NUTRITION_KEYWORDS,
    ROUTINE_KEYWORDS,
    SUPPORTED_DOMAINS,
    VACCINATION_KEYWORDS,
)
from src.gemini_client import GeminiClient
from src.utils.json_utils import ensure_list, parse_json_object


class DomainPlannerAgent:
    def __init__(self, gemini_client: GeminiClient):
        self.gemini_client = gemini_client

    def plan(self, question: str, intent_result: dict[str, Any] | None = None) -> dict[str, Any]:
        try:
            raw = self.gemini_client.generate_text(self._build_prompt(question, intent_result))
            parsed = parse_json_object(raw)
            return self._normalize(parsed, question, intent_result)
        except Exception:
            return self._fallback(question, intent_result)

    def _build_prompt(self, question: str, intent_result: dict[str, Any] | None) -> str:
        return f"""
Bạn là DomainPlanner cho chatbot y khoa mẹ & bé.
Nhiệm vụ: chọn các domain knowledge cần retrieval. Không tư vấn y khoa.

Domain hợp lệ:
- nutrition
- growth
- vaccination
- general

Luật bắt buộc:
- candidate_domains là list, gồm 1 đến 4 domain hợp lệ.
- Nếu không rõ domain, dùng ["general"].
- Nếu có triệu chứng an toàn/nguy hiểm như sốt cao, khó thở, co giật, mất nước, li bì, dị ứng nặng, luôn include "general".
- Nếu nhắc vaccine/tiêm/chủng ngừa/sốt sau tiêm, include "vaccination".
- Nếu nhắc ăn, bú, sữa, ăn dặm, dinh dưỡng, include "nutrition".
- Nếu nhắc chiều cao, cân nặng, mốc phát triển, biểu đồ tăng trưởng, include "growth".

Trả JSON thuần:
{{
  "primary_domain": "nutrition",
  "candidate_domains": ["nutrition", "vaccination", "general"],
  "reason": "Câu hỏi nhắc ăn dặm, sốt và tiêm vaccine.",
  "confidence": 0.85
}}

Intent result hiện có: {intent_result}
Câu hỏi: {question}
""".strip()

    def _normalize(
        self,
        parsed: dict[str, Any],
        question: str,
        intent_result: dict[str, Any] | None,
    ) -> dict[str, Any]:
        fallback = self._fallback(question, intent_result)
        domains = [
            str(domain).strip()
            for domain in ensure_list(parsed.get("candidate_domains"))
            if str(domain).strip() in SUPPORTED_DOMAINS
        ]
        domains = self._dedupe(domains)
        rule_domains = fallback["candidate_domains"]
        for domain in rule_domains:
            if domain not in domains:
                domains.append(domain)
        if not domains:
            domains = [DOMAIN_GENERAL]

        primary_domain = str(parsed.get("primary_domain") or fallback["primary_domain"]).strip()
        if primary_domain not in domains:
            primary_domain = domains[0]

        confidence = parsed.get("confidence", fallback["confidence"])
        try:
            confidence = max(0.0, min(float(confidence), 1.0))
        except (TypeError, ValueError):
            confidence = fallback["confidence"]

        return {
            "primary_domain": primary_domain,
            "candidate_domains": domains[:4],
            "reason": str(parsed.get("reason") or fallback["reason"]),
            "confidence": confidence,
        }

    def _fallback(self, question: str, intent_result: dict[str, Any] | None = None) -> dict[str, Any]:
        text = question.lower()
        domains: list[str] = []
        has_nutrition = any(keyword in text for keyword in NUTRITION_KEYWORDS)
        has_vaccination = any(keyword in text for keyword in VACCINATION_KEYWORDS)
        has_growth = any(keyword in text for keyword in GROWTH_KEYWORDS)
        has_routine = any(keyword in text for keyword in ROUTINE_KEYWORDS)
        has_general_safety = any(keyword in text for keyword in GENERAL_SAFETY_KEYWORDS)
        has_strong_growth = any(
            keyword in text
            for keyword in [
                "chậm tăng cân",
                "nhẹ cân",
                "cân nặng",
                "chiều cao",
                "chậm phát triển",
                "biểu đồ tăng trưởng",
            ]
        )

        intent = str((intent_result or {}).get("intent") or "")
        if intent == "nutrition":
            domains.append(DOMAIN_NUTRITION)
        elif intent == "growth":
            domains.append(DOMAIN_GROWTH)
        elif intent == "vaccination":
            domains.append(DOMAIN_VACCINATION)
        elif intent in {"routine", "sleep"}:
            domains.append(DOMAIN_ROUTINE)
        elif intent in {"symptom", "general_care", "emergency"}:
            domains.append(DOMAIN_GENERAL)

        if has_strong_growth:
            domains.append(DOMAIN_GROWTH)
        if has_nutrition:
            domains.append(DOMAIN_NUTRITION)
        if has_vaccination:
            domains.append(DOMAIN_VACCINATION)
        if has_growth and not has_strong_growth:
            domains.append(DOMAIN_GROWTH)
        if has_routine:
            domains.append(DOMAIN_ROUTINE)
        if has_general_safety:
            domains.append(DOMAIN_GENERAL)

        domains = self._dedupe(domains) or [DOMAIN_GENERAL]
        if has_strong_growth and DOMAIN_GROWTH in domains:
            domains = [DOMAIN_GROWTH] + [domain for domain in domains if domain != DOMAIN_GROWTH]
        return {
            "primary_domain": domains[0],
            "candidate_domains": domains[:4],
            "reason": "Fallback rule-based domain planning.",
            "confidence": 0.6,
        }

    def _dedupe(self, domains: list[str]) -> list[str]:
        result: list[str] = []
        for domain in domains:
            if domain in SUPPORTED_DOMAINS and domain not in result:
                result.append(domain)
        return result
