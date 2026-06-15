package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String body;
    private String dataJson;
    private Long priority;
    private String sourceType;
    private Long sourceId;
    private Long status;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
}
