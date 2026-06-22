package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotNull(message = "Profile is required for AI consultation")
    private Long profileId;
    private String conversationId;

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;
}
