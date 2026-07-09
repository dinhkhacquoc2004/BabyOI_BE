package com.example.babyoi_be.service;

import com.example.babyoi_be.security.CustomUserDetails;

public interface AnalyticsService {
    void recordDailyActivity(CustomUserDetails userDetails);

    void recordFeatureView(CustomUserDetails userDetails, String featureKey, String featureName);
}
