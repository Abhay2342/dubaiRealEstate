package com.abhay.dubairealestate.infrastructure.csv;

import com.abhay.dubairealestate.domain.model.RentTransaction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RentsCsvParser}.
 * Covers: happy-path parsing, alternative column names, blank/null fields,
 * timestamp-suffix dates, empty files, and error handling.
 */
class RentsCsvParserTest {

    private final RentsCsvParser parser = new RentsCsvParser();

    private InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------

    @Test
    void parseRentsCsv_shouldParseAllFieldsFromPrimaryColumnNames() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                C001,2025-01-15,60000,2025-02-01,2026-01-31,Downtown Dubai,Burj Khalifa,Residential,Apartment,1 B/R,75.50
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result).hasSize(1);
        RentTransaction t = result.get(0);
        assertThat(t.id()).isNull();
        assertThat(t.contractDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(t.annualAmount()).isEqualByComparingTo(new BigDecimal("60000"));
        assertThat(t.startDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(t.endDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(t.areaName()).isEqualTo("Downtown Dubai");
        assertThat(t.projectName()).isEqualTo("Burj Khalifa");
        assertThat(t.usage()).isEqualTo("Residential");
        assertThat(t.propertyType()).isEqualTo("Apartment");
        assertThat(t.rooms()).isEqualTo("1 B/R");
        assertThat(t.actualArea()).isEqualByComparingTo(new BigDecimal("75.50"));
    }

    @Test
    void parseRentsCsv_shouldSupportAlternativeColumnNames() {
        String content = """
                CONTRACT_ID,CONTRACT_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_NAME,PROJECT_NAME,USAGE,PROPERTY_TYPE,ROOMS,ACTUAL_AREA
                R002,2025-03-01,80000,2025-03-01,2026-02-28,Jumeirah,Marina Heights,Commercial,Office,Studio,120.00
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).areaName()).isEqualTo("Jumeirah");
    }

    @Test
    void parseRentsCsv_shouldParseMultipleRows() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                C001,2025-01-15,60000,2025-02-01,2026-01-31,Downtown Dubai,Burj Khalifa,Residential,Apartment,1 B/R,75.50
                C002,2025-02-20,90000,2025-03-01,2026-02-28,Marina,Marina Towers,Residential,Apartment,2 B/R,120.00
                C003,2024-12-01,45000,2025-01-01,2025-12-31,Deira,Gold Building,Residential,Studio,Studio,45.00
                """;

        assertThat(parser.parseRentsCsv(csv(content))).hasSize(3);
    }

    @Test
    void parseRentsCsv_shouldParseAmountWithThousandSeparator() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                C001,2025-01-01,"120,000",2025-01-01,2026-01-01,Area,Proj,Res,Apt,2BR,100
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).annualAmount()).isEqualByComparingTo(new BigDecimal("120000"));
    }

    // ---------------------------------------------------------------
    // Nullable / blank fields
    // ---------------------------------------------------------------

    @Test
    void parseRentsCsv_shouldHandleBlankFieldsAsNull() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                ,,,,,,,,,,
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result).hasSize(1);
        RentTransaction t = result.get(0);
        assertThat(t.contractDate()).isNull();
        assertThat(t.annualAmount()).isNull();
        assertThat(t.actualArea()).isNull();
    }

    // ---------------------------------------------------------------
    // Date parsing
    // ---------------------------------------------------------------

    @Test
    void parseRentsCsv_shouldStripTimestampSuffixFromDates() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                C001,2025-01-15 00:00:00,60000,2025-02-01 00:00:00,2026-01-31 00:00:00,Dubai,Proj,Res,Apt,1BR,50
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).contractDate()).isEqualTo(LocalDate.of(2025, 1, 15));
    }

    @Test
    void parseRentsCsv_shouldSupportDdMmYyyyDateFormat() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                C001,15/01/2025,50000,01/02/2025,31/01/2026,Dubai,Proj,Res,Apt,1BR,50
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).contractDate()).isEqualTo(LocalDate.of(2025, 1, 15));
    }

    // ---------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------

    @Test
    void parseRentsCsv_emptyFileBodyReturnsEmptyList() {
        String header = "CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA\n";

        assertThat(parser.parseRentsCsv(csv(header))).isEmpty();
    }

    @Test
    void parseRentsCsv_shouldThrowCsvParseExceptionOnNullInputStream() {
        assertThatThrownBy(() -> parser.parseRentsCsv(null))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Failed to parse rents CSV");
    }

    @Test
    void parseRentsCsv_idFieldIsAlwaysNull() {
        String content = """
                CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                C001,2025-01-01,50000,2025-01-01,2026-01-01,Area,Proj,Res,Apt,1BR,60
                """;

        List<RentTransaction> result = parser.parseRentsCsv(csv(content));

        assertThat(result.get(0).id()).isNull();
    }
}
