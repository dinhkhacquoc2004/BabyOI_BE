package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_FEATURE_KEY_LENGTH = 64;
    private static final int MAX_FEATURE_NAME_LENGTH = 120;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void recordDailyActivity(CustomUserDetails userDetails) {
        if (!isTrackableUser(userDetails)) {
            return;
        }

        LocalDate activityDate = LocalDate.now(APP_ZONE);
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update("""
                INSERT INTO user_daily_activity (
                    user_id,
                    activity_date,
                    first_seen_at,
                    last_seen_at,
                    request_count
                )
                VALUES (?, ?, ?, ?, 1)
                ON CONFLICT (user_id, activity_date)
                DO UPDATE SET
                    last_seen_at = EXCLUDED.last_seen_at,
                    request_count = user_daily_activity.request_count + 1
                """,
                userDetails.getId(),
                activityDate,
                now,
                now
        );
    }

    @Override
    public void recordFeatureView(CustomUserDetails userDetails, String featureKey, String featureName) {
        if (!isTrackableUser(userDetails)) {
            return;
        }

        String normalizedKey = normalizeFeatureKey(featureKey);
        if (normalizedKey.isBlank()) {
            return;
        }

        String normalizedName = normalizeFeatureName(featureName, normalizedKey);
        LocalDate activityDate = LocalDate.now(APP_ZONE);
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update("""
                INSERT INTO app_feature_daily_usage (
                    user_id,
                    feature_key,
                    feature_name,
                    activity_date,
                    first_viewed_at,
                    last_viewed_at,
                    view_count
                )
                VALUES (?, ?, ?, ?, ?, ?, 1)
                ON CONFLICT (user_id, feature_key, activity_date)
                DO UPDATE SET
                    feature_name = EXCLUDED.feature_name,
                    last_viewed_at = EXCLUDED.last_viewed_at,
                    view_count = app_feature_daily_usage.view_count + 1
                """,
                userDetails.getId(),
                normalizedKey,
                normalizedName,
                activityDate,
                now,
                now
        );
    }

    private boolean isTrackableUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return false;
        }

        return userDetails.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String normalizeFeatureKey(String featureKey) {
        if (featureKey == null) {
            return "";
        }

        String normalized = featureKey.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.:-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.length() > MAX_FEATURE_KEY_LENGTH) {
            return normalized.substring(0, MAX_FEATURE_KEY_LENGTH);
        }

        return normalized;
    }

    private String normalizeFeatureName(String featureName, String fallbackKey) {
        String normalized = featureName == null || featureName.isBlank()
                ? fallbackKey
                : featureName.trim();

        if (normalized.length() > MAX_FEATURE_NAME_LENGTH) {
            return normalized.substring(0, MAX_FEATURE_NAME_LENGTH);
        }

        return normalized;
    }
}
