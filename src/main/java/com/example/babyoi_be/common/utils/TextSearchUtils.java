package com.example.babyoi_be.common.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextSearchUtils {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private TextSearchUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    public static boolean contains(String source, String keyword) {
        String normalizedKeyword = normalizeSearchText(keyword);
        if (normalizedKeyword.isBlank()) {
            return true;
        }
        return normalizeSearchText(source).contains(normalizedKeyword);
    }
}
