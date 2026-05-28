package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandbookCommentResponse {
    private Long id;
    private Long postId;
    private Long parentId;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String content;
    private Long likeCount;
    private Boolean adminReply;
    private LocalDateTime createdAt;
    private List<HandbookCommentResponse> replies;
}
