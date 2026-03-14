package com.abhay.dubairealestate.infrastructure.csv;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the package-private {@link CsvUtils} helper.
 * Tests cover key normalisation, value extraction, BigDecimal parsing,
 * and multi-format date parsing.
 */
class CsvUtilsTest {

    // ---------------------------------------------------------------
    // normaliseKeys
    // ---------------------------------------------------------------

    @Test
    void normaliseKeys_shouldTrimAndUppercaseKeys() {
        Map<String, String> raw = new HashMap<>();
        raw.put("  area_name  ", "Dubai");
        raw.put("contract_id", "C001");

        Map<String, String> result = CsvUtils.normaliseKeys(raw);

        assertThat(result).containsKey("AREA_NAME").containsKey("CONTRACT_ID");
    }

    @Test
    void normaliseKeys_shouldReplaceNullKeyWithEmptyString() {
        Map<String, String> raw = new HashMap<>();
        raw.put(null, "orphan-value");

        Map<String, String> result = CsvUtils.normaliseKeys(raw);

        assertThat(result).containsKey("");
    }

    @Test
    void normaliseKeys_emptyMapReturnsEmptyMap() {
        assertThat(CsvUtils.normaliseKeys(new HashMap<>())).isEmpty();
    }

    // ---------------------------------------------------------------
    // getString
    // ---------------------------------------------------------------

    @Test
    void getString_shouldReturnFirstNonBlankValue() {
        Map<String, String> row = Map.of("KEY1", "  ", "KEY2", "value2");

        assertThat(CsvUtils.getString(row, "KEY1", "KEY2")).isEqualTo("value2");
    }

    @Test
    void getString_shouldReturnNullWhenNoKeyHasValue() {
        Map<String, String> row = Map.of("KEY1", "  ");

        assertThat(CsvUtils.getString(row, "KEY1", "MISSING")).isNull();
    }

    @Test
    void getString_shouldTrimReturnedValue() {
        Map<String, String> row = Map.of("KEY", "  trimmed  ");

        assertThat(CsvUtils.getString(row, "KEY")).isEqualTo("trimmed");
    }

    @Test
    void getString_shouldReturnNullForEmptyKeyList() {
        Map<String, String> row = Map.of("A", "val");

        assertThat(CsvUtils.getString(row)).isNull();
    }

    // ---------------------------------------------------------------
    // parseBigDecimal
    // ---------------------------------------------------------------

    @Test
    void parseBigDecimal_validDecimalString() {
        assertThat(CsvUtils.parseBigDecimal("1234.56")).isEqualByComparingTo("1234.56");
    }

    @Test
    void parseBigDecimal_valueWithThousandSeparatorCommas() {
        assertThat(CsvUtils.parseBigDecimal("1,234,567.89")).isEqualByComparingTo("1234567.89");
    }

    @Test
    void parseBigDecimal_integerString() {
        assertThat(CsvUtils.parseBigDecimal("500000")).isEqualByComparingTo("500000");
    }

    @Test
    void parseBigDecimal_nullInputReturnsNull() {
        assertThat(CsvUtils.parseBigDecimal(null)).isNull();
    }

    @Test
    void parseBigDecimal_blankInputReturnsNull() {
        assertThat(CsvUtils.parseBigDecimal("   ")).isNull();
    }

    @Test
    void parseBigDecimal_nonNumericInputReturnsNull() {
        assertThat(CsvUtils.parseBigDecimal("N/A")).isNull();
    }

    @Test
    void parseBigDecimal_emptyInputReturnsNull() {
        assertThat(CsvUtils.parseBigDecimal("")).isNull();
    }

    // ---------------------------------------------------------------
    // parseDate
    // ---------------------------------------------------------------

    @Test
    void parseDate_isoFormat_yyyy_MM_dd() {
        List<DateTimeFormatter> formats = List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(CsvUtils.parseDate("2025-12-31", formats)).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void parseDate_fallsBackToSecondFormatter() {
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        assertThat(CsvUtils.parseDate("25/12/2025", formats)).isEqualTo(LocalDate.of(2025, 12, 25));
    }

    @Test
    void parseDate_stripsTimestampSuffix() {
        List<DateTimeFormatter> formats = List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(CsvUtils.parseDate("2025-01-15 00:00:00", formats))
                .isEqualTo(LocalDate.of(2025, 1, 15));
    }

    @Test
    void parseDate_nullInputReturnsNull() {
        assertThat(CsvUtils.parseDate(null, List.of())).isNull();
    }

    @Test
    void parseDate_blankInputReturnsNull() {
        assertThat(CsvUtils.parseDate("  ", List.of(DateTimeFormatter.ISO_LOCAL_DATE))).isNull();
    }

    @Test
    void parseDate_noMatchingFormatReturnsNull() {
        List<DateTimeFormatter> formats = List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(CsvUtils.parseDate("not-a-date", formats)).isNull();
    }

    @Test
    void parseDate_singleDigitDayMonth_dSlashMSlashYyyy() {
        List<DateTimeFormatter> formats = List.of(DateTimeFormatter.ofPattern("d/M/yyyy"));

        assertThat(CsvUtils.parseDate("5/3/2025", formats)).isEqualTo(LocalDate.of(2025, 3, 5));
    }
}
