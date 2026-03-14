package com.abhay.dubairealestate.infrastructure.csv;

import com.abhay.dubairealestate.domain.model.SaleTransaction;
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
 * Unit tests for {@link SalesCsvParser}.
 * Covers: happy-path parsing, derived meter price calculation, alternative column
 * names, blank/null fields, date format variations, and error handling.
 */
class SalesCsvParserTest {

    private final SalesCsvParser parser = new SalesCsvParser();

    private InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------

    @Test
    void parseSalesCsv_shouldParseAllFieldsFromPrimaryColumnNames() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA,METER_SALE_PRICE
                T001,2025-03-10,2500000,Downtown Dubai,Emaar Tower,Residential,Sales,Apartment,2 B/R,120.50,20746.89
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        SaleTransaction t = result.get(0);
        assertThat(t.id()).isNull();
        assertThat(t.transId()).isEqualTo("T001");
        assertThat(t.transDate()).isEqualTo(LocalDate.of(2025, 3, 10));
        assertThat(t.transValue()).isEqualByComparingTo(new BigDecimal("2500000"));
        assertThat(t.areaName()).isEqualTo("Downtown Dubai");
        assertThat(t.projectName()).isEqualTo("Emaar Tower");
        assertThat(t.usage()).isEqualTo("Residential");
        assertThat(t.registrationType()).isEqualTo("Sales");
        assertThat(t.propertyType()).isEqualTo("Apartment");
        assertThat(t.rooms()).isEqualTo("2 B/R");
        assertThat(t.actualArea()).isEqualByComparingTo(new BigDecimal("120.50"));
        assertThat(t.meterSalePrice()).isEqualByComparingTo(new BigDecimal("20746.89"));
    }

    @Test
    void parseSalesCsv_shouldSupportAlternativeColumnNames() {
        String content = """
                INSTANCE_ID,TRANS_DATE,TRANS_VALUE,AREA_NAME,PROJECT_NAME,USAGE,REGISTRATION_TYPE,PROPERTY_TYPE,ROOMS,ACTUAL_AREA
                S999,2025-06-01,3000000,Jumeirah,Palm Residence,Residential,Mortgage,Villa,4 B/R,350.00
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).transId()).isEqualTo("S999");
        assertThat(result.get(0).areaName()).isEqualTo("Jumeirah");
    }

    @Test
    void parseSalesCsv_shouldParseMultipleRows() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T001,2025-01-01,1000000,Area1,Proj1,Res,Sales,Apt,1BR,100
                T002,2025-01-02,2000000,Area2,Proj2,Res,Sales,Apt,2BR,150
                T003,2025-01-03,3000000,Area3,Proj3,Com,Sales,Office,Office,200
                """;

        assertThat(parser.parseSalesCsv(csv(content))).hasSize(3);
    }

    // ---------------------------------------------------------------
    // Derived meter price
    // ---------------------------------------------------------------

    @Test
    void parseSalesCsv_shouldDeriveMeterSalePriceWhenMissing() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T002,2025-03-10,1000000,Marina,Marina View,Residential,Sales,Apartment,1 B/R,100.00
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        // meterSalePrice = 1_000_000 / 100 = 10_000.00
        assertThat(result.get(0).meterSalePrice()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void parseSalesCsv_shouldNotDeriveMeterPriceWhenActualAreaIsZero() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T003,2025-03-10,1000000,Marina,View,Residential,Sales,Apartment,1 B/R,0.00
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).meterSalePrice()).isNull();
    }

    @Test
    void parseSalesCsv_shouldNotDeriveMeterPriceWhenTransValueIsNull() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T004,2025-03-10,,Marina,View,Res,Sales,Apt,1BR,100.00
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).meterSalePrice()).isNull();
    }

    // ---------------------------------------------------------------
    // Nullable / blank fields
    // ---------------------------------------------------------------

    @Test
    void parseSalesCsv_shouldHandleBlankFieldsAsNull() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                ,,,,,,,,,
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        SaleTransaction t = result.get(0);
        assertThat(t.transId()).isNull();
        assertThat(t.transDate()).isNull();
        assertThat(t.transValue()).isNull();
        assertThat(t.actualArea()).isNull();
    }

    // ---------------------------------------------------------------
    // Date parsing
    // ---------------------------------------------------------------

    @Test
    void parseSalesCsv_shouldStripTimestampSuffixFromDate() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T001,2025-03-10 08:00:00,500000,Area,Proj,Res,Sales,Apt,1BR,50
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).transDate()).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    @Test
    void parseSalesCsv_shouldSupportDdMmYyyyDateFormat() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T001,10/03/2025,500000,Area,Proj,Res,Sales,Apt,1BR,50
                """;

        List<SaleTransaction> result = parser.parseSalesCsv(csv(content));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).transDate()).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    // ---------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------

    @Test
    void parseSalesCsv_emptyFileBodyReturnsEmptyList() {
        String header = "TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA\n";

        assertThat(parser.parseSalesCsv(csv(header))).isEmpty();
    }

    @Test
    void parseSalesCsv_shouldThrowCsvParseExceptionOnNullInputStream() {
        assertThatThrownBy(() -> parser.parseSalesCsv(null))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Failed to parse sales CSV");
    }

    @Test
    void parseSalesCsv_idFieldIsAlwaysNull() {
        String content = """
                TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
                T001,2025-01-01,1000000,Area,Proj,Res,Sales,Apt,1BR,100
                """;

        assertThat(parser.parseSalesCsv(csv(content)).get(0).id()).isNull();
    }
}
