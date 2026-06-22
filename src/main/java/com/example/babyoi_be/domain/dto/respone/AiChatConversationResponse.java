package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiChatConversationResponse {
    private String conversationId;
    private Long profileId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
