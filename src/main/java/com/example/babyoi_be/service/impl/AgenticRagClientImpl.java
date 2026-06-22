package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.domain.dto.request.AgenticRagChatHistoryItem;
import com.example.babyoi_be.domain.dto.request.AgenticRagChatRequest;
import com.example.babyoi_be.domain.dto.respone.AgenticRagChatResponse;
import com.example.babyoi_be.service.AgenticRagClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticRagClientImpl implements AgenticRagClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.agentic-rag.base-url}")
    private String baseUrl;

    @Value("${app.agentic-rag.chat-path}")
    private String chatPath;

    @Value("${app.agentic-rag.timeout-seconds:60}")
    private long timeoutSeconds;

    @Override
    public AgenticRagChatResponse chat(
            String conversationId,
            String userName,
            String message,
            Map<String, Object> profileContext,
            List<AgenticRagChatHistoryItem> chatHistory
    ) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        AgenticRagChatRequest request = AgenticRagChatRequest.builder()
                .conversationId(conversationId)
                .userName(userName)
                .message(message)
                .profileContext(profileContext)
                .chatHistory(chatHistory)
                .build();

        try {
            return webClient.post()
                    .uri(chatPath)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        log.warn("AgenticRAG returned status {} for conversation {}: {}",
                                                response.statusCode(), conversationId, body);
                                        return Mono.error(new ResponseStatusException(
                                                HttpStatus.BAD_GATEWAY,
                                                "AgenticRAG trả về lỗi, vui lòng thử lại sau"
                                        ));
                                    }))
                    .bodyToMono(AgenticRagChatResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("AgenticRAG HTTP error for conversation {}: status={}", conversationId, ex.getStatusCode(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AgenticRAG trả về lỗi, vui lòng thử lại sau");
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                log.error("AgenticRAG timeout for conversation {}", conversationId, ex);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AgenticRAG phản hồi quá lâu");
            }
            log.error("AgenticRAG call failed for conversation {}", conversationId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối AgenticRAG");
        }
    }

    private boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
