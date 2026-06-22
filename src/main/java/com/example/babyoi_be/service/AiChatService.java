package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.AiChatRequest;
import com.example.babyoi_be.domain.dto.respone.AiChatConversationResponse;
import com.example.babyoi_be.domain.dto.respone.AiChatMessageResponse;
import com.example.babyoi_be.domain.dto.respone.AiChatResponse;

import java.util.List;

public interface AiChatService {
    AiChatResponse sendMessage(AiChatRequest request);

    List<AiChatConversationResponse> getConversations(Long profileId);

    List<AiChatMessageResponse> getMessages(String conversationId);

    void deleteConversation(String conversationId);
}
