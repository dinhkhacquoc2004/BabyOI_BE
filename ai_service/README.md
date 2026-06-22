# AgenticRAG Baby Ơi

Demo Agentic RAG Python cho chatbot y khoa mẹ & bé. Hệ thống không chẩn đoán chắc chắn, không kê đơn thuốc, không đưa liều thuốc cụ thể và luôn nhắc: "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."

## Pipeline

1. `InputCleanerAgent` chuẩn hóa câu hỏi thô thành structured input.
2. `SafetyTriageAgent` kiểm tra emergency/urgent/normal.
3. Nếu emergency, hệ thống trả lời đi cấp cứu ngay và không gọi retrieval/specialized agents.
4. `IntentRouterAgent` phân loại intent để giữ backward compatibility.
5. `DomainPlannerAgent` chọn `primary_domain` và `candidate_domains`.
6. `MultiDomainRetriever` retrieval từ nhiều collection domain nếu câu hỏi cần.
7. Orchestrator chọn synthesis agent theo `primary_domain` và truyền merged contexts vào agent.
8. `AnswerValidationAgent` kiểm tra câu trả lời trước khi trả về user.

## Data Theo Domain

Các file dữ liệu được hỗ trợ:

- `data/nutrition_knowledge.txt`
- `data/growth_knowledge.txt`
- `data/vaccination_knowledge.txt`
- `data/general_medical_knowledge.txt`

Nếu file chưa tồn tại, ingest sẽ báo lỗi rõ ràng. Không tự thêm kiến thức y khoa mới vào các file này nếu chưa có nguồn nội bộ đáng tin cậy.

## Ingest

```bash
python -m src.ingest --domain nutrition
python -m src.ingest --domain growth
python -m src.ingest --domain vaccination
python -m src.ingest --domain general
python -m src.ingest --all
```

Collection mặc định theo domain:

- `baby_oi_nutrition_knowledge`
- `baby_oi_growth_knowledge`
- `baby_oi_vaccination_knowledge`
- `baby_oi_general_medical_knowledge`

## Multi-Domain Retrieval

Data vẫn tách theo file và collection riêng. Khi runtime, planner có thể chọn nhiều domain:

- Câu hỏi có ăn dặm + sốt + vaccine: `nutrition`, `vaccination`, `general`
- Câu hỏi có chậm tăng cân + ăn uống: `growth`, `nutrition`
- Câu hỏi sốt sau tiêm: `vaccination`, `general`

Mỗi chunk retrieved giữ metadata `domain`, `source`, `topic`, `collection`, `score`/`distance`. CLI in thêm candidate domains, used domains, selected collections và số chunk retrieved theo domain.

## Chạy Demo CLI

```bash
python -m src.main
```

## Chạy REST API Cho ChatDemo/BabyOI_BE

AgenticRAG expose API FastAPI để ChatDemo hoặc BabyOI_BE gọi qua HTTP. CLI `python -m src.main` vẫn được giữ nguyên.

```bash
python -m uvicorn src.api:app --host 0.0.0.0 --port 8000 --reload
```

Health check:

```bash
curl http://localhost:8000/health
```

Chat endpoint - request cũ vẫn được hỗ trợ:

```bash
curl -X POST http://localhost:8000/api/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"conversationId\":\"demo-1\",\"userName\":\"user\",\"message\":\"Bé 8 tháng chậm tăng cân nên ăn gì?\"}"
```

Chat endpoint - request mới có thêm `profileContext` và `chatHistory`:

```bash
curl -X POST http://localhost:8000/api/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"conversationId\":\"demo-2\",\"userName\":\"user\",\"message\":\"Bé nên ăn gì hôm nay?\",\"profileContext\":{\"name\":\"Bé Na\",\"birthDate\":\"2025-10-01\",\"gender\":\"female\",\"profileType\":\"baby\"},\"chatHistory\":[{\"sender\":\"user\",\"message\":\"Bé hơi biếng ăn.\"},{\"sender\":\"assistant\",\"message\":\"Bạn có thể chia nhỏ bữa và theo dõi dấu hiệu bất thường.\"}]}"
```

`chatHistory` chỉ lấy tối đa 6 message gần nhất để làm ngữ cảnh nội bộ cho orchestrator. `profileContext` và `chatHistory` không được đưa thẳng ra `botReply`.

Response trả về có cả `answer` và `botReply` để ChatDemo/BabyOI_BE mapping ổn định. `userMessage` luôn là message gốc user gửi lên:

```json
{
  "conversationId": "demo-1",
  "userName": "user",
  "answer": "...",
  "botReply": "...",
  "userMessage": "Bé 8 tháng chậm tăng cân nên ăn gì?",
  "selectedAgent": "nutrition",
  "intent": "nutrition",
  "usedDomains": ["nutrition"],
  "safetyLevel": "normal"
}
```

Demo cases nên thử:

- `Bé 7 tháng ăn dặm nhưng đang sốt có tiêm vaccine được không?`
- `Bé 8 tháng chậm tăng cân nên ăn gì?`
- `Bé bị sốt sau tiêm thì chăm sóc thế nào?`
- `Lịch tiêm vaccine cho bé 2 tháng?`
- `Bé 6 tháng bắt đầu ăn dặm như thế nào?`
- `Bé 5 tháng sốt cao, co giật và khó thở`

Manual planner check không cần API:

```bash
python tests/manual_domain_planner_cases.py
```

## Data Cần Bổ Sung Sau Này

- Tài liệu dinh dưỡng theo độ tuổi, ăn dặm, bú mẹ/sữa công thức, dị ứng thức ăn.
- Tài liệu tăng trưởng có nguồn chuẩn, nêu rõ khi nào cần dùng biểu đồ WHO hoặc khám bác sĩ.
- Tài liệu tiêm chủng theo lịch chính thức và hướng dẫn xử lý trễ lịch từ nguồn có thẩm quyền.
- Tài liệu general về triệu chứng/chăm sóc thường gặp, đặc biệt danh sách dấu hiệu cần đi khám/cấp cứu.

## Rủi Ro Còn Lại

- RAG phụ thuộc chất lượng và độ cập nhật của dữ liệu nội bộ.
- Multi-domain retrieval giúp tăng coverage nhưng cũng cần prompt/validator đủ chặt để tránh tổng hợp quá phạm vi context.
- Gemini có thể trả lời không đúng format; code đã có fallback nhưng vẫn cần kiểm thử thêm.
- Validator rule-based chỉ bắt được một số cụm nguy cơ phổ biến.
- Đây vẫn là chatbot tham khảo, không thay thế bác sĩ hoặc cơ sở y tế.
