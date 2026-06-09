package com.example.babyoi_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryTimestampService {

    private static final Duration OFFSET_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration OFFSET_REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final long LOG_SKEW_THRESHOLD_SECONDS = 60L;

    private final WebClient.Builder webClientBuilder;
    private final Map<String, CachedOffset> cachedOffsets = new ConcurrentHashMap<>();

    public long currentEpochSecond(String cloudName) {
        long localTimestamp = Instant.now().getEpochSecond();
        return localTimestamp + resolveOffsetSeconds(cloudName, localTimestamp);
    }

    private long resolveOffsetSeconds(String cloudName, long localTimestamp) {
        if (cloudName == null || cloudName.isBlank()) {
            return 0L;
        }

        Instant now = Instant.now();
        CachedOffset cachedOffset = cachedOffsets.get(cloudName);
        if (cachedOffset != null && now.isBefore(cachedOffset.expiresAt())) {
            return cachedOffset.offsetSeconds();
        }

        Long refreshedOffset = fetchOffsetSeconds(cloudName, localTimestamp);
        if (refreshedOffset != null) {
            cachedOffsets.put(cloudName, new CachedOffset(refreshedOffset, now.plus(OFFSET_CACHE_TTL)));
            return refreshedOffset;
        }

        return cachedOffset != null ? cachedOffset.offsetSeconds() : 0L;
    }

    private Long fetchOffsetSeconds(String cloudName, long localTimestamp) {
        try {
            String dateHeader = webClientBuilder.clone()
                    .build()
                    .get()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", cloudName)
                    .exchangeToMono(response -> {
                        String header = response.headers().asHttpHeaders().getFirst(HttpHeaders.DATE);
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .thenReturn(header == null ? "" : header);
                    })
                    .timeout(OFFSET_REQUEST_TIMEOUT)
                    .block();

            if (dateHeader == null || dateHeader.isBlank()) {
                return null;
            }

            long serverTimestamp = ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant()
                    .getEpochSecond();
            long offsetSeconds = serverTimestamp - localTimestamp;
            if (Math.abs(offsetSeconds) >= LOG_SKEW_THRESHOLD_SECONDS) {
                log.warn(
                        "Cloudinary server time differs from local clock by {} seconds; using Cloudinary time for signed requests",
                        offsetSeconds
                );
            }
            return offsetSeconds;
        } catch (Exception exception) {
            log.debug("Could not resolve Cloudinary server time; using local clock for signed request", exception);
            return null;
        }
    }

    private record CachedOffset(long offsetSeconds, Instant expiresAt) {
    }
}
