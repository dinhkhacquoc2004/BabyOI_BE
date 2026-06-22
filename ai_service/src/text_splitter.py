from __future__ import annotations

from typing import Any


def _split_long_text(text: str, max_chars: int = 1000) -> list[str]:
    paragraphs = [part.strip() for part in text.split("\n\n") if part.strip()]
    chunks: list[str] = []
    current = ""

    for paragraph in paragraphs:
        if len(paragraph) > max_chars:
            if current:
                chunks.append(current.strip())
                current = ""
            for start in range(0, len(paragraph), max_chars):
                chunks.append(paragraph[start : start + max_chars].strip())
            continue

        candidate = f"{current}\n\n{paragraph}".strip() if current else paragraph
        if len(candidate) <= max_chars:
            current = candidate
        else:
            chunks.append(current.strip())
            current = paragraph

    if current:
        chunks.append(current.strip())

    return chunks


def split_text(
    text: str,
    source: str = "medical_knowledge.txt",
    id_prefix: str = "chunk",
    domain: str | None = None,
) -> list[dict[str, Any]]:
    sections: list[tuple[str, str]] = []
    current_topic = "Không rõ chủ đề"
    current_lines: list[str] = []

    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        if line.startswith("# "):
            if current_lines:
                sections.append((current_topic, "\n".join(current_lines).strip()))
            current_topic = line.removeprefix("# ").strip()
            current_lines = [line]
        else:
            current_lines.append(line)

    if current_lines:
        sections.append((current_topic, "\n".join(current_lines).strip()))

    chunks: list[dict[str, Any]] = []
    counter = 1
    for topic, section_text in sections:
        for piece in _split_long_text(section_text):
            chunks.append(
                {
                    "id": f"{id_prefix}_{counter:03d}",
                    "text": piece,
                    "metadata": {
                        "source": source,
                        "topic": topic,
                        **({"domain": domain} if domain else {}),
                    },
                }
            )
            counter += 1

    return chunks
