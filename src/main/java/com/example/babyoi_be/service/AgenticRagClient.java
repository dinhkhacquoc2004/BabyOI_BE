package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.AgenticRagChatHistoryItem;
import com.example.babyoi_be.domain.dto.respone.AgenticRagChatResponse;

import java.util.List;
import java.util.Map;

public interface AgenticRagClient {
    AgenticRagChatResponse chat(
            String conversationId,
            String userName,
            String message,
            Map<String, Object> profileContext,
            List<AgenticRagChatHistoryItem> chatHistory
    );
}
