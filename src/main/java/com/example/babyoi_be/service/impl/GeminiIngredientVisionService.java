package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.domain.dto.respone.DetectedIngredientResponse;
import com.example.babyoi_be.service.IngredientVisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GeminiIngredientVisionService implements IngredientVisionService {

    private static final String INGREDIENT_DETECTION_PROMPT = """
            You identify food ingredients for a mother-and-baby nutrition app.
            Analyze the image and list only clearly visible edible ingredients.
            If the image shows a whole animal, fish, or poultry, return the common edible cooking ingredient name, not the whole object.
            Example: whole chicken should be returned as "thịt gà" or another visible chicken edible part.
            Do not list kitchen tools, bowls, plates, tables, or hands.
            Return at most 12 ingredients.
            Return only valid JSON in this exact shape:
            {"ingredients":[{"name":"Vietnamese ingredient name with diacritics","confidence":0.0}]}
            confidence must be between 0 and 1. Skip uncertain ingredients.
            """;
    private static final Pattern INGREDIENT_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern INGREDIENT_CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${app.ai.gemini.vision-model:gemini-3.5-flash,gemini-2.5-flash,gemini-2.5-flash-lite}")
    private String visionModels;

    @Value("${app.ai.gemini.timeout-seconds:20}")
    private Long timeoutSeconds;

    @Value("${app.ai.gemini.thinking-level:minimal}")
    private String thinkingLevel;

    @Value("${app.ai.gemini.thinking-budget:0}")
    private Integer thinkingBudget;

    @Override
    public boolean isAvailable() {
        return !normalizedApiKey().isBlank();
    }

    @Override
    public List<DetectedIngredientResponse> detectIngredients(MultipartFile image) {
        validateImage(image);
        if (!isAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI vision chưa được cấu hình GEMINI_API_KEY");
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(image);
            JsonNode response = callGeminiWithFallbackModels(requestBody);

            return parseDetectedIngredients(extractOutputText(response));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            if (isTimeoutException(exception)) {
                log.warn("Gemini ingredient vision timed out. models={}, timeoutSeconds={}", visionModels, timeoutSeconds);
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Gemini phan hoi qua cham, vui long thu lai hoac nhap ingredientNames",
                        exception
                );
            }
            log.error("Gemini ingredient vision failed unexpectedly. models={}", visionModels, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể phân tích ảnh nguyên liệu bằng Gemini", exception);
        }
    }

    private JsonNode callGeminiWithFallbackModels(Map<String, Object> requestBody) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", normalizedApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        List<String> models = resolveVisionModels();
        WebClientResponseException lastException = null;
        RuntimeException lastRuntimeException = null;
        String lastFailureMessage = "không có model Gemini nào được cấu hình";

        for (String model : models) {
            try {
                return webClient.post()
                        .uri("/{model}:generateContent", model)
                        .bodyValue(buildRequestBodyForModel(requestBody, model))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                        .block();
            } catch (WebClientResponseException exception) {
                lastException = exception;
                lastRuntimeException = exception;
                String geminiMessage = extractGeminiErrorMessage(exception.getResponseBodyAsString());
                lastFailureMessage = geminiMessage;
                log.error(
                        "Gemini ingredient vision failed. status={}, model={}, message={}",
                        exception.getStatusCode(),
                        model,
                        geminiMessage,
                        exception
                );
                if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Gemini API key không hợp lệ hoặc chưa được cấp quyền gọi Gemini",
                            exception
                    );
                }
                if (!shouldTryNextModel(exception.getStatusCode().value())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Gemini API loi: " + geminiMessage,
                            exception
                    );
                }
            } catch (RuntimeException exception) {
                lastRuntimeException = exception;
                if (isTimeoutException(exception)) {
                    lastFailureMessage = "model Gemini phan hoi qua cham: " + model;
                    log.warn("Gemini ingredient vision timed out. model={}, timeoutSeconds={}", model, resolveTimeoutSeconds());
                    continue;
                }
                throw exception;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.GATEWAY_TIMEOUT,
                "Gemini API loi: " + lastFailureMessage,
                lastRuntimeException != null ? lastRuntimeException : lastException
        );
    }

    private Map<String, Object> buildRequestBodyForModel(Map<String, Object> requestBody, String model) {
        Map<String, Object> body = new LinkedHashMap<>(requestBody);
        Object generationConfigValue = requestBody.get("generationConfig");
        Map<String, Object> generationConfig = generationConfigValue instanceof Map<?, ?> originalConfig
                ? new LinkedHashMap<>((Map<String, Object>) originalConfig)
                : new LinkedHashMap<>();
        Map<String, Object> thinkingConfig = resolveThinkingConfig(model);
        if (!thinkingConfig.isEmpty()) {
            generationConfig.put("thinkingConfig", thinkingConfig);
        }
        body.put("generationConfig", generationConfig);
        return body;
    }

    private Map<String, Object> resolveThinkingConfig(String model) {
        String normalizedModel = model != null ? model.toLowerCase() : "";
        Map<String, Object> thinkingConfig = new LinkedHashMap<>();
        if (normalizedModel.startsWith("gemini-3") || normalizedModel.startsWith("gemini-flash")) {
            String normalizedThinkingLevel = thinkingLevel != null ? thinkingLevel.trim().toLowerCase() : "";
            if (!normalizedThinkingLevel.isBlank()) {
                thinkingConfig.put("thinkingLevel", normalizedThinkingLevel);
            }
            return thinkingConfig;
        }
        if (normalizedModel.startsWith("gemini-2.5") && thinkingBudget != null) {
            thinkingConfig.put("thinkingBudget", thinkingBudget);
        }
        return thinkingConfig;
    }

    private long resolveTimeoutSeconds() {
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 20L;
    }

    private boolean shouldTryNextModel(int statusCode) {
        return statusCode == HttpStatus.NOT_FOUND.value()
                || statusCode == HttpStatus.TOO_MANY_REQUESTS.value()
                || statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()
                || statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String normalizedApiKey() {
        return apiKey != null ? apiKey.replaceAll("\\s+", "") : "";
    }

    private List<String> resolveVisionModels() {
        if (visionModels == null || visionModels.isBlank()) {
            return List.of("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite");
        }
        return List.of(visionModels.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần gửi ảnh nguyên liệu");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tai len phai la anh");
        }
    }

    private Map<String, Object> buildRequestBody(MultipartFile image) throws Exception {
        Map<String, Object> inlineData = new LinkedHashMap<>();
        inlineData.put("mime_type", image.getContentType() != null ? image.getContentType() : MediaType.IMAGE_JPEG_VALUE);
        inlineData.put("data", Base64.getEncoder().encodeToString(image.getBytes()));

        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("inline_data", inlineData);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", INGREDIENT_DETECTION_PROMPT);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(imagePart, textPart));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 1024);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        JsonNode candidates = response.get("candidates");
        if (candidates != null && candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (parts != null && parts.isArray()) {
                    for (JsonNode part : parts) {
                        JsonNode text = part.get("text");
                        if (text != null && text.isTextual()) {
                            builder.append(text.asText());
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private List<DetectedIngredientResponse> parseDetectedIngredients(String outputText) throws Exception {
        String cleanedText = cleanJsonText(outputText);
        if (cleanedText.isBlank()) {
            return List.of();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(cleanedText);
        } catch (Exception exception) {
            List<DetectedIngredientResponse> fallbackIngredients = parseIngredientsFromPartialJson(cleanedText);
            if (!fallbackIngredients.isEmpty()) {
                log.warn("Gemini returned malformed ingredient JSON, using partial parse. text={}", compactLogText(cleanedText));
                return fallbackIngredients;
            }
            log.warn("Gemini returned malformed ingredient JSON. text={}", compactLogText(cleanedText), exception);
            return List.of();
        }
        JsonNode ingredientsNode = root.isArray() ? root : root.get("ingredients");
        if (ingredientsNode == null || !ingredientsNode.isArray()) {
            return List.of();
        }

        List<DetectedIngredientResponse> ingredients = new ArrayList<>();
        for (JsonNode ingredientNode : ingredientsNode) {
            String name = ingredientNode.path("name").asText("").trim();
            if (name.isBlank()) {
                continue;
            }
            double confidence = ingredientNode.path("confidence").asDouble(0.0);
            ingredients.add(DetectedIngredientResponse.builder()
                    .name(name)
                    .confidence(confidence)
                    .build());
        }
        return ingredients;
    }

    private List<DetectedIngredientResponse> parseIngredientsFromPartialJson(String outputText) {
        if (outputText == null || outputText.isBlank()) {
            return List.of();
        }

        List<Double> confidences = new ArrayList<>();
        Matcher confidenceMatcher = INGREDIENT_CONFIDENCE_PATTERN.matcher(outputText);
        while (confidenceMatcher.find()) {
            try {
                confidences.add(Double.parseDouble(confidenceMatcher.group(1)));
            } catch (NumberFormatException ignored) {
                confidences.add(0.0);
            }
        }

        List<DetectedIngredientResponse> ingredients = new ArrayList<>();
        Matcher nameMatcher = INGREDIENT_NAME_PATTERN.matcher(outputText);
        int index = 0;
        while (nameMatcher.find()) {
            String name = nameMatcher.group(1).trim();
            if (!name.isBlank()) {
                double confidence = index < confidences.size() ? confidences.get(index) : 0.7;
                ingredients.add(DetectedIngredientResponse.builder()
                        .name(name)
                        .confidence(Math.max(0.0, Math.min(1.0, confidence)))
                        .build());
            }
            index++;
        }
        return ingredients;
    }

    private String extractGeminiErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "không có nội dung lỗi từ Gemini";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            String status = root.path("error").path("status").asText("");
            if (!message.isBlank() && !status.isBlank()) {
                return status + " - " + message;
            }
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // Fall back to a compact raw response below.
        }
        return responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody;
    }

    private String compactLogText(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) : compact;
    }

    private String cleanJsonText(String outputText) {
        if (outputText == null) {
            return "";
        }
        String text = outputText.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }
}
