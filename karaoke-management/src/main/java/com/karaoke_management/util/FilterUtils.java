package com.karaoke_management.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Helper cho các màn hình LIST có bộ lọc.
 *
 * - Parse linh hoạt nhiều định dạng ngày/giờ (VN + ISO + datetime-local).
 * - Chuẩn hoá khoảng lọc (swap from/to nếu nhập ngược).
 * - Chuẩn hoá min/max nếu nhập ngược.
 */
public final class FilterUtils {

    private FilterUtils() {}

    // VN formats
    private static final DateTimeFormatter VN_DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ISO / HTML input formats
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;               // yyyy-MM-dd
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;     // yyyy-MM-ddTHH:mm[:ss]
    private static final DateTimeFormatter HTML_DT_LOCAL_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter HTML_DT_LOCAL_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Parse linh hoạt:
     * - HH:mm dd/MM/yyyy
     * - dd/MM/yyyy
     * - yyyy-MM-ddTHH:mm (datetime-local)
     * - yyyy-MM-ddTHH:mm:ss
     * - yyyy-MM-dd
     */
    public static LocalDateTime parseFlexibleDateOrDateTimeOrNull(String s, boolean isFrom) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;

        // 1) VN datetime
        LocalDateTime dt = tryParseDateTime(v, VN_DATE_TIME);
        if (dt != null) return dt;

        // 2) ISO datetime (covers yyyy-MM-ddTHH:mm[:ss] in some cases)
        dt = tryParseDateTime(v, ISO_DATE_TIME);
        if (dt != null) return dt;

        // 3) HTML datetime-local common patterns
        dt = tryParseDateTime(v, HTML_DT_LOCAL_MIN);
        if (dt != null) return dt;
        dt = tryParseDateTime(v, HTML_DT_LOCAL_SEC);
        if (dt != null) return dt;

        // 4) date-only VN
        LocalDate d = tryParseDate(v, VN_DATE);
        if (d != null) return isFrom ? d.atStartOfDay() : d.atTime(LocalTime.MAX);

        // 5) date-only ISO
        d = tryParseDate(v, ISO_DATE);
        if (d != null) return isFrom ? d.atStartOfDay() : d.atTime(LocalTime.MAX);

        return null;
    }

    /**
     * Nếu cả from/to đều null -> dùng default.
     * Nếu from > to -> tự swap.
     */
    public static DateTimeRange normalizeDateTimeRange(
            LocalDateTime from,
            LocalDateTime to,
            LocalDateTime defaultFromIfBothNull,
            LocalDateTime defaultToIfBothNull
    ) {
        LocalDateTime f = from;
        LocalDateTime t = to;

        if (f == null && t == null) {
            f = defaultFromIfBothNull;
            t = defaultToIfBothNull;
        }

        if (f != null && t != null && f.isAfter(t)) {
            LocalDateTime tmp = f;
            f = t;
            t = tmp;
        }
        return new DateTimeRange(f, t);
    }

    /** Nếu min > max -> tự swap. */
    public static MinMaxRange normalizeMinMax(BigDecimal min, BigDecimal max) {
        BigDecimal mn = min;
        BigDecimal mx = max;
        if (mn != null && mx != null && mn.compareTo(mx) > 0) {
            BigDecimal tmp = mn;
            mn = mx;
            mx = tmp;
        }
        return new MinMaxRange(mn, mx);
    }

    private static LocalDateTime tryParseDateTime(String v, DateTimeFormatter f) {
        try {
            return LocalDateTime.parse(v, f);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    private static LocalDate tryParseDate(String v, DateTimeFormatter f) {
        try {
            return LocalDate.parse(v, f);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    public record DateTimeRange(LocalDateTime from, LocalDateTime to) {}
    public record MinMaxRange(BigDecimal min, BigDecimal max) {}
}
