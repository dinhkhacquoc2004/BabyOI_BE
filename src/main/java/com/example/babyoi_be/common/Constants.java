package com.example.babyoi_be.common;

public interface Constants {
    final class TABLE_STATUS {
        private TABLE_STATUS() {
            throw new IllegalStateException("Utility class");
        }
        public static final Long SUCCESS = 3L;
        public static final Long PENDING = -1L;
        public static final Long INACTIVE = 0L;
        public static final Long INITIATED = 1L;
        public static final Long ACTIVE = 2L;
        public static final Long UPDATING = -3L;
        public static final Long CANCELED = -2L;
        public static final Long DELETED = -4L;
        public static final Long EXPIRED = -5L;
        public static final Long BLOCKED = -6L;
        public static final Long FAILED = -7L;
    }

    final class API_RESPONSE {
        private API_RESPONSE() {
            throw new IllegalStateException("Utility class");
        }

        public static final String RETURN_CODE_OK = "200";
        public static final String RETURN_CODE_CREATED = "201";
        public static final String RETURN_CODE_NO_CONTENT = "204";
        public static final String RETURN_CODE_BAD_REQUEST = "400";
        public static final String RETURN_CODE_UNAUTHORIZED = "401";
        public static final String RETURN_CODE_FORBIDDEN = "403";
        public static final String RETURN_CODE_CONFLICT = "409";
        public static final String RETURN_CODE_ERROR = "500";
    }

    final class STATUS_COMMON {
        private STATUS_COMMON() {
            throw new IllegalStateException("Utility class");
        }

        public static final Boolean RESPONSE_STATUS_TRUE = true;
        public static final Boolean RESPONSE_STATUS_FALSE = false;
    }
}
