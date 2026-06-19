package com.example.babyoi_be.domain.dto.respone;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class IngredientAdjustmentSuggestionResponse {
    private String status;
    private Long foodId;
    private Long profileId;
    private String storageKey;
    private Map<String, String> amounts;
    private String summary;
    private String reason;
    private List<String> warnings;
    private Integer expiresInDays;
}
