package com.example.babyoi_be.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureViewRequest {
    @NotBlank(message = "Feature key is required")
    @Size(max = 64, message = "Feature key must not exceed 64 characters")
    private String featureKey;

    @Size(max = 120, message = "Feature name must not exceed 120 characters")
    private String featureName;
}
