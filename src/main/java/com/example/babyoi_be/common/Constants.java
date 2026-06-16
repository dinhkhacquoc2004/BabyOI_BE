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

    final class NOTIFICATION_TYPE {
        private NOTIFICATION_TYPE() {
            throw new IllegalStateException("Utility class");
        }

        public static final String VACCINE_REMINDER = "VACCINE_REMINDER";
        public static final String HEALTH_REMINDER = "HEALTH_REMINDER";
        public static final String HANDBOOK_COMMENT_REPLY = "HANDBOOK_COMMENT_REPLY";
        public static final String SYSTEM = "SYSTEM";
    }

    final class NOTIFICATION_PRIORITY {
        private NOTIFICATION_PRIORITY() {
            throw new IllegalStateException("Utility class");
        }

        public static final Long LOW = 1L;
        public static final Long NORMAL = 2L;
        public static final Long HIGH = 3L;
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

    final class FOOD_FUNCTION_CODE {
        private FOOD_FUNCTION_CODE() {
            throw new IllegalStateException("Utility class");
        }

        public static final Long MOM = 1L;
        public static final String MOM_LABEL = "Mẹ";

        public static final Long BABY = 2L;
        public static final String BABY_LABEL = "Bé";

        public static boolean isMomCode(Long functionCode) {
            return MOM.equals(functionCode);
        }

        public static boolean isBabyCode(Long functionCode) {
            return BABY.equals(functionCode);
        }
    }

    final class FOOD_ADVICE_FOR {
        private FOOD_ADVICE_FOR() {
            throw new IllegalStateException("Utility class");
        }

        public static final String MOM_GET_BACK_IN_SHAPE = "FOR_MOTHER_DIET";
        public static final String MOM_CHANGE_DIET = "FOR_MOTHER_CHANGE_DIET";
        public static final String MOM_POSTPARTUM_BREASTFEEDING = "FOR_MOTHER_POSTPARTUM_BREASTFEEDING";
        public static final String MOM_HEALTHY_ENERGY = "FOR_MOTHER_HEALTHY_ENERGY";
        public static final String MOM_DIGESTION_RECOVERY = "FOR_MOTHER_DIGESTION_RECOVERY";
        public static final String MOM_INCREASE_MILK_SUPPLY = "FOR_MOTHER_INCREASE_MILK_SUPPLY";
        public static final String MOM_SLEEP_STRESS_SUPPORT = "FOR_MOTHER_SLEEP_STRESS_SUPPORT";

        public static final String BABY_6_TO_8_MONTHS = "FOR_BABY_6_8_MONTHS";
        public static final String BABY_9_TO_11_MONTHS = "FOR_BABY_9_11_MONTHS";
        public static final String BABY_12_TO_18_MONTHS = "FOR_BABY_12_18_MONTHS";
        public static final String BABY_19_TO_24_MONTHS = "FOR_BABY_19_24_MONTHS";
        public static final String BABY_6_TO_8_MONTHS_DEVELOPMENT = "FOR_BABY_6_8_MONTHS_DEVELOPMENT";
        public static final String BABY_9_TO_11_MONTHS_DEVELOPMENT = "FOR_BABY_9_11_MONTHS_DEVELOPMENT";
        public static final String BABY_12_TO_18_MONTHS_DEVELOPMENT = "FOR_BABY_12_18_MONTHS_DEVELOPMENT";
        public static final String BABY_19_TO_24_MONTHS_DEVELOPMENT = "FOR_BABY_19_24_MONTHS_DEVELOPMENT";
    }
}
