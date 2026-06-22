from __future__ import annotations

import sys

from src.orchestrator import AgenticRAGOrchestrator


def _print_result(result: dict) -> None:
    intent = result["intent_result"]["intent"]
    safety_level = result["safety_result"]["safety_level"]
    selected_agent = result.get("selected_agent", "unknown")
    used_collection = result.get("used_collection")
    routing_result = result.get("routing_result", {})
    retrieval_debug = result.get("retrieval_debug", {})
    candidate_domains = routing_result.get("candidate_domains", [])
    used_domains = result.get("used_domains", [])
    missing_fields = result.get("cleaned_input", {}).get("missing_important_fields", [])
    validation_issues = result.get("validation_result", {}).get("issues", [])

    topics = []
    for item in result.get("retrieved_contexts", []):
        metadata = item.get("metadata") or {}
        topic = metadata.get("topic")
        if topic and topic not in topics:
            topics.append(topic)

    print("\n--- Kết quả ---")
    print(f"Intent: {intent}")
    print(f"Safety level: {safety_level}")
    print(f"Selected agent: {selected_agent}")
    print(f"Candidate domains: {', '.join(candidate_domains) if candidate_domains else '(không có)'}")
    print(f"Used domains: {', '.join(used_domains) if used_domains else '(không dùng retrieval)'}")
    print(f"Used collection: {used_collection or '(không dùng retrieval)'}")
    if retrieval_debug.get("selected_collections"):
        print(f"Selected collections: {retrieval_debug['selected_collections']}")
    if retrieval_debug.get("retrieved_counts"):
        print(f"Retrieved counts: {retrieval_debug['retrieved_counts']}")
    print(
        "Missing important fields: "
        + (", ".join(missing_fields) if missing_fields else "(không có)")
    )

    if validation_issues:
        print("Validation issues: " + "; ".join(validation_issues))
    else:
        print("Validation issues: (không có)")

    print("\nAnswer:")
    print(result["answer"])
    print("\nRetrieved topics:")
    print(", ".join(topics) if topics else "(không dùng retrieval)")
    print()


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")

    print("Baby Ơi Agentic RAG Demo")
    print("Gõ 'exit' để thoát")
    print()

    try:
        orchestrator = AgenticRAGOrchestrator()
    except Exception as exc:
        print(f"Lỗi khởi tạo demo: {exc}")
        return

    while True:
        user_question = input("Bạn: ").strip()
        if user_question.lower() in {"exit", "quit"}:
            print("Tạm biệt.")
            break
        if not user_question:
            continue

        try:
            result = orchestrator.run(user_question)
            _print_result(result)
        except Exception as exc:
            print(f"Lỗi xử lý câu hỏi: {exc}")


if __name__ == "__main__":
    main()
