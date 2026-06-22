package com.example.babyoi_be.domain.dto.respone;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgenticRagChatResponse {
    private String conversationId;
    private String userName;
    private String answer;
    private String botReply;
    private String userMessage;
    private String selectedAgent;
    private String intent;
    private String safetyLevel;
    private List<String> usedDomains;
    private String debugLog;
    private Map<String, Object> rawResult;
}
