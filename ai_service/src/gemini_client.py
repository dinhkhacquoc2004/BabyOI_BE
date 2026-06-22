from google import genai

from src.config import Settings, get_settings


class GeminiClient:
    def __init__(self, settings: Settings | None = None):
        self.settings = settings or get_settings()
        self.client = genai.Client(api_key=self.settings.gemini_api_key)

    def generate_text(self, prompt: str) -> str:
        try:
            response = self.client.models.generate_content(
                model=self.settings.gemini_chat_model,
                contents=prompt,
            )
            text = getattr(response, "text", None)
            if not text:
                raise RuntimeError("Gemini khong tra ve text.")
            return text.strip()
        except Exception as exc:
            raise RuntimeError(
                f"Loi goi Gemini chat model '{self.settings.gemini_chat_model}': {exc}"
            ) from exc

    def embed_text(self, text: str) -> list[float]:
        try:
            response = self.client.models.embed_content(
                model=self.settings.gemini_embedding_model,
                contents=text,
            )
            if hasattr(response, "embeddings") and response.embeddings:
                values = response.embeddings[0].values
            elif hasattr(response, "embedding"):
                values = response.embedding.values
            else:
                raise RuntimeError("Gemini embedding response khong co vector.")
            return [float(value) for value in values]
        except Exception as exc:
            raise RuntimeError(
                f"Loi goi Gemini embedding model '{self.settings.gemini_embedding_model}': {exc}"
            ) from exc
