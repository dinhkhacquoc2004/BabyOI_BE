package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandbookCommentRequest {
    private Long parentId;

    @NotBlank(message = "Comment content is required")
    @Size(max = 1000, message = "Comment content must be at most 1000 characters")
    private String content;
}
