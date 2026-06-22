from __future__ import annotations


MEDICAL_DISCLAIMER = "Thông tin chỉ mang tính tham khảo, không thay thế bác sĩ."

DOMAIN_NUTRITION = "nutrition"
DOMAIN_GROWTH = "growth"
DOMAIN_VACCINATION = "vaccination"
DOMAIN_ROUTINE = "routine"
DOMAIN_GENERAL = "general"

SPECIALIZED_DOMAINS = {
    DOMAIN_NUTRITION,
    DOMAIN_GROWTH,
    DOMAIN_VACCINATION,
    DOMAIN_ROUTINE,
}

SUPPORTED_DOMAINS = (
    DOMAIN_NUTRITION,
    DOMAIN_GROWTH,
    DOMAIN_VACCINATION,
    DOMAIN_ROUTINE,
    DOMAIN_GENERAL,
)

DOMAIN_FILE_NAMES = {
    DOMAIN_NUTRITION: "nutrition_knowledge.txt",
    DOMAIN_GROWTH: "growth_knowledge.txt",
    DOMAIN_VACCINATION: "vaccination_knowledge.txt",
    DOMAIN_ROUTINE: "routine_knowledge.txt",
    DOMAIN_GENERAL: "general_medical_knowledge.txt",
}

DOMAIN_COLLECTION_SETTING_ATTRS = {
    DOMAIN_NUTRITION: "nutrition_collection_name",
    DOMAIN_GROWTH: "growth_collection_name",
    DOMAIN_VACCINATION: "vaccination_collection_name",
    DOMAIN_ROUTINE: "routine_collection_name",
    DOMAIN_GENERAL: "general_collection_name",
}

ROUTINE_KEYWORDS = [
    "lịch sinh hoạt",
    "sinh hoạt",
    "nếp sinh hoạt",
    "giấc ngủ",
    "ngủ ngày",
    "ngủ đêm",
    "giờ ngủ",
    "thức giấc",
    "cữ bú",
    "giờ ăn",
    "tắm",
    "chơi",
    "vận động",
    "màn hình",
    "routine",
    "schedule",
    "sleep",
    "nap",
    "bedtime",
]

NUTRITION_KEYWORDS = [
    "ăn",
    "dinh dưỡng",
    "sữa",
    "bú",
    "ăn dặm",
    "dị ứng thức ăn",
    "trứng",
    "cháo",
    "bột",
    "thực đơn",
    "khẩu phần",
    "calo",
    "kcal",
    "năng lượng",
    "chất đạm",
    "protein",
    "tinh bột",
    "chất béo",
    "vitamin",
    "khoáng chất",
    "tdee",
    "bmr",
    "bmi",
    "biếng ăn",
    "tăng cân",
    "giảm cân",
]

VACCINATION_KEYWORDS = [
    "tiêm",
    "tiêm chủng",
    "vắc xin",
    "vacxin",
    "vaccine",
    "mũi tiêm",
    "miễn dịch",
    "5 trong 1",
    "6 trong 1",
]

GROWTH_KEYWORDS = [
    "cân nặng",
    "nặng",
    "chiều cao",
    "cao",
    "tăng trưởng",
    "nhẹ cân",
    "chậm tăng cân",
    "chậm phát triển",
    "mốc phát triển",
    "biểu đồ tăng trưởng",
]

GENERAL_SAFETY_KEYWORDS = [
    "ốm",
    "bệnh",
    "bé ốm",
    "bệnh thường gặp",
    "sốt",
    "sốt cao",
    "ho",
    "sổ mũi",
    "nghẹt mũi",
    "viêm phổi",
    "thở nhanh",
    "khó thở",
    "co giật",
    "mất nước",
    "li bì",
    "lừ đừ",
    "dị ứng nặng",
    "phát ban",
    "mẩn đỏ",
    "nôn",
    "ói",
    "trớ",
    "tiêu chảy",
    "phân máu",
    "bỏ bú",
    "bú kém",
    "đau tai",
    "chảy mủ tai",
    "đỏ mắt",
    "hăm tã",
    "vàng da",
    "rốn đỏ",
]

RED_FLAG_KEYWORDS = [
    "co giật",
    "khó thở",
    "tím tái",
    "li bì",
    "mất ý thức",
    "nôn ra máu",
    "nôn xanh",
    "nôn vàng",
    "bỏ bú hoàn toàn",
    "sốt cao",
]
