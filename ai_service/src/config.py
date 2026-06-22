from dataclasses import dataclass
from pathlib import Path
import os

from dotenv import load_dotenv


PROJECT_ROOT = Path(__file__).resolve().parents[1]
load_dotenv(PROJECT_ROOT / ".env")


@dataclass(frozen=True)
class Settings:
    gemini_api_key: str
    gemini_chat_model: str
    gemini_embedding_model: str
    chroma_path: str
    collection_name: str
    nutrition_collection_name: str
    growth_collection_name: str
    vaccination_collection_name: str
    routine_collection_name: str
    general_collection_name: str


def get_settings(validate_api_key: bool = True) -> Settings:
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if validate_api_key and not api_key:
        raise ValueError(
            "Thieu GEMINI_API_KEY. Hay copy .env.example thanh .env va dien API key Gemini."
        )

    chroma_path = os.getenv("CHROMA_PATH", "./chroma_db").strip()
    chroma_path_abs = str((PROJECT_ROOT / chroma_path).resolve()) if chroma_path.startswith(".") else chroma_path

    return Settings(
        gemini_api_key=api_key,
        gemini_chat_model=os.getenv("GEMINI_CHAT_MODEL", "gemini-2.5-flash").strip(),
        gemini_embedding_model=os.getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001").strip(),
        chroma_path=chroma_path_abs,
        collection_name=os.getenv("COLLECTION_NAME", "baby_oi_medical_knowledge").strip(),
        nutrition_collection_name=os.getenv(
            "NUTRITION_COLLECTION_NAME", "baby_oi_nutrition_knowledge"
        ).strip(),
        growth_collection_name=os.getenv(
            "GROWTH_COLLECTION_NAME", "baby_oi_growth_knowledge"
        ).strip(),
        vaccination_collection_name=os.getenv(
            "VACCINATION_COLLECTION_NAME", "baby_oi_vaccination_knowledge"
        ).strip(),
        routine_collection_name=os.getenv(
            "ROUTINE_COLLECTION_NAME", "baby_oi_routine_knowledge"
        ).strip(),
        general_collection_name=os.getenv(
            "GENERAL_COLLECTION_NAME", "baby_oi_general_medical_knowledge"
        ).strip(),
    )
