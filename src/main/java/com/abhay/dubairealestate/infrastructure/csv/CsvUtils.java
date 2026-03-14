package com.abhay.dubairealestate.infrastructure.csv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared CSV parsing utilities: key normalisation, type conversions, multi-format date parsing.
 */
final class CsvUtils {

    private CsvUtils() {}

    /** Returns a new map with all keys converted to uppercase (trimmed). */
    static Map<String, String> normaliseKeys(Map<String, String> row) {
        Map<String, String> normalised = new HashMap<>(row.size());
        row.forEach((k, v) -> normalised.put(k == null ? "" : k.trim().toUpperCase(), v));
        return normalised;
    }

    /**
     * Returns the first non-blank value found by trying the supplied key candidates.
     * Returns {@code null} if none match.
     */
    static String getString(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String val = row.get(key);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }

    /**
     * Parses a numeric string, stripping commas used as thousand separators.
     * Returns {@code null} for blank/null input.
     */
    static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Attempts to parse a date string using each formatter in order.
     * Strips a trailing time component (e.g. "2026-01-01 00:04:09" → "2026-01-01") before parsing.
     * Returns {@code null} if none match or input is blank.
     */
    static LocalDate parseDate(String value, List<DateTimeFormatter> formatters) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        // Strip time component if present (DLD CSVs use "yyyy-MM-dd HH:mm:ss" format)
        int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx > 0) {
            cleaned = cleaned.substring(0, spaceIdx);
        }
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }
}
