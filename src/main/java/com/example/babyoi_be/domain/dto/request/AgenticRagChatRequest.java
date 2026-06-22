package com.example.babyoi_be.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticRagChatRequest {
    private String conversationId;
    private String userName;
    private String message;
    private Map<String, Object> profileContext;
    private List<AgenticRagChatHistoryItem> chatHistory;
}
