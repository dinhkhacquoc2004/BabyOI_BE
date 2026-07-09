package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.FeatureViewRequest;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/feature-views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackFeatureView(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody FeatureViewRequest request
    ) {
        try {
            analyticsService.recordFeatureView(currentUser, request.getFeatureKey(), request.getFeatureName());
        } catch (Exception exception) {
            log.warn("Could not record feature view for user {}", currentUser != null ? currentUser.getId() : null, exception);
        }
    }

    @PostMapping("/page-views/nutrition")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackNutritionPageView(@AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            analyticsService.recordFeatureView(currentUser, "nutrition", "Dinh dưỡng");
        } catch (Exception exception) {
            log.warn("Could not record nutrition page view for user {}", currentUser != null ? currentUser.getId() : null, exception);
        }
    }
}
