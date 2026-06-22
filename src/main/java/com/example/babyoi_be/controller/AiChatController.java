package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.AiChatRequest;
import com.example.babyoi_be.domain.dto.respone.AiChatConversationResponse;
import com.example.babyoi_be.domain.dto.respone.AiChatMessageResponse;
import com.example.babyoi_be.domain.dto.respone.AiChatResponse;
import com.example.babyoi_be.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-chat")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/messages")
    public AiChatResponse sendMessage(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.sendMessage(request);
    }

    @GetMapping("/conversations")
    public List<AiChatConversationResponse> getConversations(@RequestParam(required = false) Long profileId) {
        return aiChatService.getConversations(profileId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<AiChatMessageResponse> getMessages(@PathVariable String conversationId) {
        return aiChatService.getMessages(conversationId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable String conversationId) {
        aiChatService.deleteConversation(conversationId);
    }
}
