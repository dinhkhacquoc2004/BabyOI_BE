package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AiChatMessageResponse {
    private Long id;
    private String sender;
    private String message;
    private String selectedAgent;
    private String intent;
    private String safetyLevel;
    private List<String> usedDomains;
    private LocalDateTime createdAt;
}
