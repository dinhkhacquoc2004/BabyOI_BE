package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IngredientAdjustmentSuggestRequest {
    @NotNull
    private Long profileId;

    @NotNull
    private Long foodId;

    private String currentGoal;
    private String userNotes;
}
