# BabyOi Agentic RAG knowledge base

Các file `*_knowledge.txt` là nguồn kiến thức nội bộ được chia đoạn và đưa vào
ChromaDB khi AI service khởi động.

## Domain

- `nutrition`: dinh dưỡng, ăn bổ sung, gợi ý món theo catalog của profile.
- `growth`: đo và theo dõi tăng trưởng, phát triển.
- `vaccination`: nguyên tắc tiêm chủng và theo dõi sau tiêm.
- `routine`: ngủ, vận động, màn hình và lịch sinh hoạt.
- `general`: triệu chứng, bệnh thường gặp, dấu hiệu nguy hiểm, theo dõi tại nhà và chăm sóc hỗ trợ an toàn.

Mỗi câu hỏi chỉ truy xuất collection cần thiết. Gemini embedding được tạo một lần
cho câu hỏi rồi dùng lại giữa các domain. Fingerprint giúp chỉ rebuild collection
có file nguồn thay đổi.

## Quy tắc cập nhật

- Chỉ dùng nguồn chính thống hoặc tài liệu nội bộ đã duyệt.
- Ghi URL và ngày rà soát; danh mục tập trung nằm trong `SOURCES.md`.
- Diễn giải ngắn bằng tiếng Việt, không sao chép nguyên văn dài.
- Không thêm chẩn đoán xác định, đơn thuốc hoặc liều thuốc.
- Khuyến cáo điều trị và lịch tiêm phải đối chiếu hướng dẫn hiện hành tại Việt Nam.

```env
AUTO_INGEST_ON_STARTUP=true
AUTO_INGEST_DOMAINS=nutrition,growth,vaccination,routine,general
```
