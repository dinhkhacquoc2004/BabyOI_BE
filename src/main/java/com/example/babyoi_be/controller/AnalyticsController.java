package com.example.babyoi_be.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Compatibility endpoints for client-side telemetry.
 *
 * Page-view tracking is intentionally fire-and-forget: the mobile client does
 * not send a payload and no analytics persistence/reporting contract exists yet.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @PostMapping("/page-views/nutrition")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackNutritionPageView() {
        // Kept as a no-op until a persisted analytics specification is defined.
    }
}
