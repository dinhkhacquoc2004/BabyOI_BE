package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandbookPostResponse {
    private Long id;
    private String category;
    private String title;
    private String snippet;
    private String content;
    private String imageUrl;
    private String authorName;
    private String authorRole;
    private LocalDateTime publishedAt;
    private Long commentCount;
}
