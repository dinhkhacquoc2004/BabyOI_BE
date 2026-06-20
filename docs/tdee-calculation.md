# Cách BabyOi tính nhu cầu năng lượng

Tài liệu này mô tả đúng hành vi backend. Các kết quả là giá trị ước tính cho người khỏe mạnh, không thay thế đánh giá của bác sĩ hoặc chuyên gia dinh dưỡng.

## 1. Phân biệt các khái niệm

- **BMR**: năng lượng chuyển hóa cơ bản của mẹ khi nghỉ ngơi.
- **TDEE của mẹ**: tổng năng lượng mẹ tiêu hao trong ngày, bằng BMR nhân hệ số vận động.
- **TEE của bé**: năng lượng bé tiêu hao cho hoạt động và chức năng cơ thể.
- **EER của bé**: tổng nhu cầu năng lượng ước tính, bằng TEE cộng năng lượng dành cho tăng trưởng.
- **Năng lượng ăn bổ sung**: phần năng lượng từ thức ăn ngoài sữa. Giá trị này nhỏ hơn EER và không được dùng thay cho EER.

API đang giữ tên field `tdee` cho cả mẹ và bé để tương thích. Với hồ sơ bé, giá trị trong field này có ý nghĩa chuyên môn là **EER**.

## 2. TDEE của mẹ

### Dữ liệu đầu vào

- `W`: cân nặng, kg.
- `H`: chiều cao, cm.
- `A`: tuổi tròn tại ngày ghi chỉ số, năm.
- `AF`: hệ số vận động bắt buộc gửi trong mỗi lần tạo hoặc cập nhật bản ghi sức khỏe của mẹ.

### Công thức

Hồ sơ mẹ được xác định là nữ, nên BabyOi dùng phương trình Mifflin-St Jeor cho nữ:

```text
BMR = 10 × W + 6.25 × H - 5 × A - 161
TDEE = BMR × AF
```

### Hệ số vận động

| Mã | Diễn giải | AF |
|---|---|---:|
| `SEDENTARY` | Không tập luyện; chủ yếu ngồi và sinh hoạt hằng ngày | 1.200 |
| `LIGHT` | Tập nhẹ 1-3 buổi/tuần, như đi bộ hoặc yoga nhẹ | 1.375 |
| `MODERATE` | Tập cường độ vừa 3-5 buổi/tuần | 1.550 |
| `ACTIVE` | Tập nặng 6-7 buổi/tuần | 1.725 |
| `VERY_ACTIVE` | Tập nặng mỗi ngày, có thể 2 buổi/ngày, hoặc lao động thể lực | 1.900 |

Mỗi lần tạo hoặc cập nhật chỉ số của mẹ, request phải gửi `activityLevel`. Backend dùng mức này để tính TDEE của bản ghi và đồng bộ lại vào hồ sơ mẹ làm mức hiện tại.

### Ví dụ

Mẹ 30 tuổi, 60 kg, 165 cm, mức `MODERATE`:

```text
BMR  = 10 × 60 + 6.25 × 165 - 5 × 30 - 161
     = 1320.25 kcal/ngày

TDEE = 1320.25 × 1.55
     = 2046.39 ≈ 2046 kcal/ngày
```

BabyOi hiện chưa cộng riêng năng lượng thai kỳ hoặc cho con bú vì hồ sơ chưa lưu giai đoạn thai kỳ/tiết sữa. Không nên tự cộng một mức cố định khi chưa có dữ liệu này.

## 3. EER của bé 0-35 tháng

### Dữ liệu đầu vào

- `W`: cân nặng thực đo, kg.
- Tuổi theo tháng tại ngày ghi chỉ số.

### Công thức IOM

```text
TEE = 89 × W - 100
EER = TEE + năng lượng tăng trưởng
```

| Tuổi | Năng lượng tăng trưởng |
|---|---:|
| 0-3 tháng | 175 kcal/ngày |
| 4-6 tháng | 56 kcal/ngày |
| 7-12 tháng | 22 kcal/ngày |
| 13-35 tháng | 20 kcal/ngày |

Ví dụ bé 8 tháng, 10 kg:

```text
EER = (89 × 10 - 100) + 22
    = 812 kcal/ngày
```

Phương trình này không có hằng số giới tính. Nghiên cứu theo dõi EER riêng cho bé trai và bé gái cho thấy mức trung bình của bé trai thường cao hơn chủ yếu do khác biệt cân nặng theo tuổi; với cùng cân nặng thực đo, công thức IOM cho cùng kết quả. Không tự thêm hệ số giới tính ngoài mô hình.

## 4. Năng lượng từ thức ăn bổ sung

Nguồn WHO người dùng cung cấp áp dụng cho trẻ **đang bú mẹ theo nhu cầu**. Sữa vẫn cung cấp phần lớn năng lượng; lịch ăn chỉ nhắm phần thiếu hụt cần được bù bằng thức ăn bổ sung.

| Tuổi | Mục tiêu từ thức ăn bổ sung | Số bữa/ngày trong BabyOi |
|---|---:|---:|
| Dưới 6 tháng | Không tạo lịch ăn bổ sung | 0 |
| 6-8 tháng | Khoảng 200 kcal/ngày | 2-3 |
| 9-11 tháng | Khoảng 300 kcal/ngày | 3-4 |
| 12-23 tháng | Khoảng 550 kcal/ngày | 3-4 |

Quy tắc backend:

```text
Mẹ: targetDailyCalories = TDEE × hệ số mục tiêu
Bé 6-23 tháng: targetDailyCalories = min(EER, mục tiêu thức ăn bổ sung theo tuổi)
```

Do đó, `targetDailyCalories` trong lịch của bé là mục tiêu cho **các món ăn bổ sung**, không phải toàn bộ EER của bé. Phần còn lại dự kiến đến từ sữa mẹ. Với trẻ không bú mẹ hoặc có bệnh lý, cần hướng dẫn riêng từ chuyên gia.

Từ 24 tháng trở lên, bảng WHO trên không còn áp dụng. Backend không gán mốc `550 kcal` cho nhóm này mà quay về mục tiêu tổng EER hiện có.

## 5. Nguồn tham khảo

1. WHO. *Infant and Young Child Feeding: Model Chapter*, phần Complementary feeding: https://www.ncbi.nlm.nih.gov/books/NBK148957/
2. Stan SV và cộng sự. *Estimated Energy Requirements of Infants and Young Children up to 24 Months of Age*. Current Developments in Nutrition, 2021. https://doi.org/10.1093/cdn/nzab122
3. Mifflin MD và cộng sự. *A new predictive equation for resting energy expenditure in healthy individuals*. American Journal of Clinical Nutrition, 1990. https://pubmed.ncbi.nlm.nih.gov/2305711/
