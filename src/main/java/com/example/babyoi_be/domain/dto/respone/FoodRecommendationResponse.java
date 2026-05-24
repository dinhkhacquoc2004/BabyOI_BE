package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendationResponse {
    private Long id;
    private String goodPoints;
    private String badPoints;
    private String advice;
    private String cookingWay;
    private Long status;
}
