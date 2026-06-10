package com.example.babyoi_be.common;

import java.util.Map;

public interface VaccineRuleConstants {
    final class RECORD_SOURCE {
        private RECORD_SOURCE() {
        }

        public static final Long STANDARD = 1L;
        public static final Long CUSTOM = 2L;

        private static final Map<Long, String> CODES = Map.of(
                STANDARD, "STANDARD",
                CUSTOM, "CUSTOM"
        );

        public static String codeOf(Long source) {
            return CODES.getOrDefault(source, "UNKNOWN");
        }
    }

    final class DISEASE_SCHEDULE_STATUS {
        private DISEASE_SCHEDULE_STATUS() {
        }

        public static final Long ACTIVE = 2L;
        public static final Long STOPPED = -8L;
    }

    final class INTERCHANGE_RULE {
        private INTERCHANGE_RULE() {
        }

        public static final Long ALLOW_INTERCHANGE = 1L;
        public static final Long PREFER_SAME_PRODUCT = 2L;
        public static final Long MIXED_SERIES_USE_MAX_DOSE = 3L;
        public static final Long NOT_INTERCHANGEABLE = 4L;
        public static final Long SPECIAL_RESTART_REQUIRED = 5L;
        public static final Long ANNUAL_SINGLE_DOSE = 6L;

        private static final Map<Long, String> CODES = Map.of(
                ALLOW_INTERCHANGE, "ALLOW_INTERCHANGE",
                PREFER_SAME_PRODUCT, "PREFER_SAME_PRODUCT",
                MIXED_SERIES_USE_MAX_DOSE, "MIXED_SERIES_USE_MAX_DOSE",
                NOT_INTERCHANGEABLE, "NOT_INTERCHANGEABLE",
                SPECIAL_RESTART_REQUIRED, "SPECIAL_RESTART_REQUIRED",
                ANNUAL_SINGLE_DOSE, "ANNUAL_SINGLE_DOSE"
        );

        public static String codeOf(Long rule) {
            return CODES.getOrDefault(rule, "UNKNOWN");
        }
    }
}
