package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteFoodResponse {
    private Long id;
    private Long userId;
    private Long foodId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private FoodResponse food;
}
