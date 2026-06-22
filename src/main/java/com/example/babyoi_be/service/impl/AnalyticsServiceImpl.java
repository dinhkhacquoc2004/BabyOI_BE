package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final ZoneId APP_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> TRACKED_PAGES = Set.of(NUTRITION_PAGE);

    private final JdbcTemplate jdbcTemplate;
    private final Map<Long, LocalDate> lastRecordedActivityDates = new ConcurrentHashMap<>();

    @Override
    public void recordDailyActivity(CustomUserDetails userDetails) {
        if (!isAppUser(userDetails)) {
            return;
        }

        Long userId = userDetails.getId();
        LocalDate today = LocalDate.now(APP_TIME_ZONE);
        if (today.equals(lastRecordedActivityDates.get(userId))) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO user_daily_activity (user_id, activity_date)
                VALUES (?, ?)
                ON CONFLICT (user_id, activity_date) DO NOTHING
                """,
                userId,
                Date.valueOf(today)
        );
        lastRecordedActivityDates.put(userId, today);
    }

    @Override
    public void recordPageView(CustomUserDetails userDetails, String pageKey) {
        if (!isAppUser(userDetails)) {
            return;
        }

        String normalizedPageKey = pageKey == null ? "" : pageKey.trim().toUpperCase(Locale.ROOT);
        if (!TRACKED_PAGES.contains(normalizedPageKey)) {
            throw new IllegalArgumentException("Unsupported analytics page: " + pageKey);
        }

        LocalDate today = LocalDate.now(APP_TIME_ZONE);
        jdbcTemplate.update(
                """
                INSERT INTO app_page_daily_activity (user_id, page_key, activity_date, view_count)
                VALUES (?, ?, ?, 1)
                ON CONFLICT (user_id, page_key, activity_date)
                DO UPDATE SET
                    view_count = app_page_daily_activity.view_count + 1,
                    last_viewed_at = CURRENT_TIMESTAMP
                """,
                userDetails.getId(),
                normalizedPageKey,
                Date.valueOf(today)
        );
    }

    private boolean isAppUser(CustomUserDetails userDetails) {
        return userDetails != null
                && userDetails.getId() != null
                && !"ADMIN".equalsIgnoreCase(userDetails.getRoleName())
                && !userDetails.getUsername().toLowerCase(Locale.ROOT).endsWith("@babyoi.local");
    }
}
