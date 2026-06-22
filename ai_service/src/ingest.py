from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

from src.config import PROJECT_ROOT, Settings, get_settings
from src.constants import (
    DOMAIN_COLLECTION_SETTING_ATTRS,
    DOMAIN_FILE_NAMES,
    DOMAIN_GENERAL,
    DOMAIN_GROWTH,
    DOMAIN_NUTRITION,
    DOMAIN_ROUTINE,
    DOMAIN_VACCINATION,
)
from src.gemini_client import GeminiClient
from src.text_splitter import split_text
from src.vector_store import VectorStore


SUPPORTED_DOMAINS = (
    DOMAIN_NUTRITION,
    DOMAIN_GROWTH,
    DOMAIN_VACCINATION,
    DOMAIN_ROUTINE,
    DOMAIN_GENERAL,
)


def _collection_name_for_domain(settings: Settings, domain: str) -> str:
    attr_name = DOMAIN_COLLECTION_SETTING_ATTRS[domain]
    return str(getattr(settings, attr_name))


def _knowledge_path_for_domain(domain: str) -> Path:
    return PROJECT_ROOT / "data" / DOMAIN_FILE_NAMES[domain]


def ingest_domain(domain: str) -> int:
    if domain not in SUPPORTED_DOMAINS:
        raise ValueError(f"Domain không hợp lệ: {domain}")

    settings = get_settings()
    knowledge_path = _knowledge_path_for_domain(domain)
    if not knowledge_path.exists():
        raise FileNotFoundError(
            f"Không tìm thấy file {knowledge_path}. "
            "Hãy bổ sung dữ liệu y khoa nội bộ cho domain này trước khi ingest."
        )

    text = knowledge_path.read_text(encoding="utf-8")
    knowledge_fingerprint = hashlib.sha256(
        f"v2\n{settings.gemini_embedding_model}\n{text}".encode("utf-8")
    ).hexdigest()
    chunks = split_text(
        text,
        source=knowledge_path.name,
        id_prefix=f"{domain}_chunk",
        domain=domain,
    )
    if not chunks:
        raise ValueError(f"File {knowledge_path} không có nội dung để ingest.")

    collection_name = _collection_name_for_domain(settings, domain)
    vector_store = VectorStore(settings, collection_name=collection_name)

    collection_metadata = vector_store.collection.metadata or {}
    current_fingerprint = str(collection_metadata.get("knowledge_fingerprint") or "")
    if vector_store.has_documents() and current_fingerprint == knowledge_fingerprint:
        print(f"Knowledge unchanged for '{collection_name}'.")
        print(f"Total documents in collection: {vector_store.collection.count()}")
        return 0

    if vector_store.has_documents():
        print(f"Knowledge changed for '{collection_name}', rebuilding collection.")
        vector_store.client.delete_collection(collection_name)
        vector_store = VectorStore(settings, collection_name=collection_name)

    chunk_ids = [chunk["id"] for chunk in chunks]
    existing_ids = vector_store.get_existing_ids(chunk_ids)
    new_chunks = [chunk for chunk in chunks if chunk["id"] not in existing_ids]
    skipped_count = len(chunks) - len(new_chunks)

    print(f"Total chunks: {len(chunks)}")
    print(f"Skipped existing chunks: {skipped_count}")
    print(f"New chunks to embed: {len(new_chunks)}")

    if not new_chunks:
        print("No new chunks to ingest. Gemini embedding was not called.")
        print(f"Total documents in collection: {vector_store.collection.count()}")
        return 0

    gemini = GeminiClient(settings)
    embeddings = []
    for index, chunk in enumerate(new_chunks, start=1):
        print(f"Embedding {domain} chunk {index}/{len(new_chunks)}: {chunk['metadata']['topic']}")
        embeddings.append(gemini.embed_text(chunk["text"]))

    added = vector_store.add_documents(new_chunks, embeddings)
    vector_store.collection.modify(metadata={
        "domain": domain,
        "source": knowledge_path.name,
        "knowledge_fingerprint": knowledge_fingerprint,
        "embedding_model": settings.gemini_embedding_model,
    })
    print(f"Added chunks: {added}")
    print(f"Đã ingest {added} chunk mới vào collection '{collection_name}'.")
    print(f"Tổng số document trong collection: {vector_store.collection.count()}")
    return added


def ensure_domains_ingested(domains: tuple[str, ...]) -> dict[str, int]:
    results: dict[str, int] = {}
    for domain in domains:
        results[domain] = ingest_domain(domain)
    return results


def ingest() -> int:
    """Backward-compatible ingest entrypoint, now mapped to the general domain."""
    return ingest_domain(DOMAIN_GENERAL)


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Ingest Baby Ơi knowledge by domain.")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--domain", choices=SUPPORTED_DOMAINS, help="Domain cần ingest.")
    group.add_argument("--all", action="store_true", help="Ingest toàn bộ domain.")
    args = parser.parse_args()

    domains = SUPPORTED_DOMAINS if args.all else (args.domain or DOMAIN_GENERAL,)
    total_added = 0
    for domain in domains:
        print(f"\n=== Ingest domain: {domain} ===")
        try:
            total_added += ingest_domain(domain)
        except (FileNotFoundError, ValueError) as exc:
            print(f"Lỗi ingest: {exc}")
            if not args.all:
                raise SystemExit(1) from None
    print(f"\nHoàn tất. Tổng chunk mới đã ingest: {total_added}")


if __name__ == "__main__":
    main()
