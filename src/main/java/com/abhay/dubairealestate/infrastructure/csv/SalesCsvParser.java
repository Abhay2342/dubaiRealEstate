package com.abhay.dubairealestate.infrastructure.csv;

import com.abhay.dubairealestate.application.port.out.ParseSalesCsvPort;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses Dubai DLD sales transaction CSV files.
 *
 * Column name mapping (case-insensitive, common DLD variants supported):
 *   trans_id          → TRANS_ID / INSTANCE_ID
 *   trans_date        → INSTANCE_DATE / TRANS_DATE
 *   trans_value       → TRANS_VALUE
 *   area_name         → AREA_EN / AREA_NAME_EN / AREA_NAME
 *   project_name      → PROJECT_EN / PROJECT_NAME
 *   usage             → USAGE_EN / USAGE / GROUP_EN
 *   registration_type → REG_TYPE_EN / REGISTRATION_TYPE
 *   property_type     → PROP_TYPE_EN / PROPERTY_TYPE
 *   rooms             → ROOMS_EN / ROOMS
 *   actual_area       → ACTUAL_AREA / AREA
 *   meter_sale_price  → METER_SALE_PRICE / PRICE_PER_SQM
 */
@Slf4j
@Component
public class SalesCsvParser implements ParseSalesCsvPort {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    @Override
    public List<SaleTransaction> parseSalesCsv(InputStream inputStream) {
        List<SaleTransaction> results = new ArrayList<>();
        int rowIndex = 1;

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAwareBuilder(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)).build()) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                rowIndex++;
                try {
                    results.add(mapRow(row));
                } catch (Exception e) {
                    log.warn("Skipping sales CSV row {} due to parse error: {}", rowIndex, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new CsvParseException("Failed to parse sales CSV: " + e.getMessage(), e);
        }

        log.info("Parsed {} valid sales transactions from CSV ({} rows read)", results.size(), rowIndex - 1);
        return results;
    }

    private SaleTransaction mapRow(Map<String, String> row) {
        // Normalise keys to uppercase for case-insensitive lookup
        Map<String, String> r = CsvUtils.normaliseKeys(row);

        BigDecimal transValue  = CsvUtils.parseBigDecimal(CsvUtils.getString(r, "TRANS_VALUE"));
        BigDecimal actualArea  = CsvUtils.parseBigDecimal(CsvUtils.getString(r, "ACTUAL_AREA", "AREA"));

        // meter_sale_price is not provided in the DLD CSV — compute it from trans_value / actual_area
        BigDecimal meterSalePrice = CsvUtils.parseBigDecimal(CsvUtils.getString(r, "METER_SALE_PRICE", "PRICE_PER_SQM"));
        if (meterSalePrice == null && transValue != null && actualArea != null
                && actualArea.compareTo(BigDecimal.ZERO) != 0) {
            meterSalePrice = transValue.divide(actualArea, 2, RoundingMode.HALF_UP);
        }

        return new SaleTransaction(
                null,
                CsvUtils.getString(r, "TRANS_ID", "INSTANCE_ID", "TRANSACTION_NUMBER"),
                CsvUtils.parseDate(CsvUtils.getString(r, "INSTANCE_DATE", "TRANS_DATE"), DATE_FORMATS),
                transValue,
                CsvUtils.getString(r, "AREA_EN", "AREA_NAME_EN", "AREA_NAME"),
                CsvUtils.getString(r, "PROJECT_EN", "PROJECT_NAME"),
                CsvUtils.getString(r, "USAGE_EN", "USAGE", "GROUP_EN"),
                CsvUtils.getString(r, "REG_TYPE_EN", "REGISTRATION_TYPE", "PROCEDURE_EN"),
                CsvUtils.getString(r, "PROP_TYPE_EN", "PROPERTY_TYPE"),
                CsvUtils.getString(r, "ROOMS_EN", "ROOMS"),
                actualArea,
                meterSalePrice
        );
    }
}
