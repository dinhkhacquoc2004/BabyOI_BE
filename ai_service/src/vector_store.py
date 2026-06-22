from __future__ import annotations

from typing import Any

import chromadb

from src.config import Settings, get_settings


class VectorStore:
    def __init__(self, settings: Settings | None = None, collection_name: str | None = None):
        self.settings = settings or get_settings(validate_api_key=False)
        self.collection_name = collection_name or self.settings.collection_name
        self.client = chromadb.PersistentClient(path=self.settings.chroma_path)
        self.collection = self.client.get_or_create_collection(name=self.collection_name)

    def has_documents(self) -> bool:
        return self.collection.count() > 0

    def get_existing_ids(self, ids: list[str]) -> set[str]:
        if not ids:
            return set()

        result = self.collection.get(ids=ids)
        return set(result.get("ids", []))

    def add_documents(self, chunks: list[dict[str, Any]], embeddings: list[list[float]]) -> int:
        if len(chunks) != len(embeddings):
            raise ValueError("Số chunk và số embedding không khớp.")

        ids = [chunk["id"] for chunk in chunks]
        existing_ids = self.get_existing_ids(ids)

        new_chunks = []
        new_embeddings = []
        for chunk, embedding in zip(chunks, embeddings):
            if chunk["id"] not in existing_ids:
                new_chunks.append(chunk)
                new_embeddings.append(embedding)

        if not new_chunks:
            return 0

        self.collection.add(
            ids=[chunk["id"] for chunk in new_chunks],
            documents=[chunk["text"] for chunk in new_chunks],
            metadatas=[chunk["metadata"] for chunk in new_chunks],
            embeddings=new_embeddings,
        )
        return len(new_chunks)

    def similarity_search(self, query_embedding: list[float], top_k: int = 4) -> list[dict[str, Any]]:
        if not self.has_documents():
            raise RuntimeError(
                f"Collection '{self.collection_name}' chưa có dữ liệu. "
                "Bạn cần chạy ingest cho domain tương ứng."
            )

        result = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k,
            include=["documents", "metadatas", "distances"],
        )

        documents = result.get("documents", [[]])[0]
        metadatas = result.get("metadatas", [[]])[0]
        distances = result.get("distances", [[]])[0]
        ids = result.get("ids", [[]])[0]

        return [
            {
                "id": doc_id,
                "document": document,
                "metadata": metadata,
                "distance": distance,
            }
            for doc_id, document, metadata, distance in zip(ids, documents, metadatas, distances)
        ]
