package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodIngredientResponse {
    private Long id;
    private Long foodIngredientId;
    private String nameIngredients;
    private String imageUrl;
    private Double amountPerServing;
    private String unit;
    private Long status;
    private String description;
    private IngredientNutritionResponse nutrition;
}
