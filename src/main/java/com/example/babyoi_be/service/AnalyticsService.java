package com.example.babyoi_be.service;

import com.example.babyoi_be.security.CustomUserDetails;

public interface AnalyticsService {

    String NUTRITION_PAGE = "NUTRITION";

    void recordDailyActivity(CustomUserDetails userDetails);

    void recordPageView(CustomUserDetails userDetails, String pageKey);
}
