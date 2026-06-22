package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AiChatResponse {
    private String conversationId;
    private Long profileId;
    private String userMessage;
    private String botReply;
    private String answer;
    private String selectedAgent;
    private String intent;
    private String safetyLevel;
    private List<String> usedDomains;
    private LocalDateTime createdAt;
}
