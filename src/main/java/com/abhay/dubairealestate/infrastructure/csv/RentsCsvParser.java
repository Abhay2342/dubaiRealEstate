package com.abhay.dubairealestate.infrastructure.csv;

import com.abhay.dubairealestate.application.port.out.ParseRentsCsvPort;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses Dubai DLD rent transaction CSV files.
 *
 * Column name mapping (case-insensitive, common DLD variants supported):
 *   contract_id   → CONTRACT_NUM / CONTRACT_ID
 *   contract_date → INSTANCE_DATE / CONTRACT_DATE
 *   annual_amount → ANNUAL_AMOUNT
 *   start_date    → START_DATE
 *   end_date      → END_DATE
 *   area_name     → AREA_EN / AREA_NAME_EN / AREA_NAME
 *   project_name  → PROJECT_EN / PROJECT_NAME
 *   usage         → USAGE_EN / USAGE
 *   property_type → PROP_TYPE_EN / PROPERTY_TYPE
 *   rooms         → ROOMS_EN / ROOMS
 *   actual_area   → ACTUAL_AREA / AREA
 */
@Slf4j
@Component
public class RentsCsvParser implements ParseRentsCsvPort {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    @Override
    public List<RentTransaction> parseRentsCsv(InputStream inputStream) {
        List<RentTransaction> results = new ArrayList<>();
        int rowIndex = 1;

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAwareBuilder(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)).build()) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                rowIndex++;
                try {
                    results.add(mapRow(row));
                } catch (Exception e) {
                    log.warn("Skipping rents CSV row {} due to parse error: {}", rowIndex, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new CsvParseException("Failed to parse rents CSV: " + e.getMessage(), e);
        }

        log.info("Parsed {} valid rent transactions from CSV ({} rows read)", results.size(), rowIndex - 1);
        return results;
    }

    private RentTransaction mapRow(Map<String, String> row) {
        Map<String, String> r = CsvUtils.normaliseKeys(row);

        return new RentTransaction(
                null,
                CsvUtils.parseDate(CsvUtils.getString(r, "INSTANCE_DATE", "CONTRACT_DATE", "REGISTRATION_DATE"), DATE_FORMATS),
                CsvUtils.parseBigDecimal(CsvUtils.getString(r, "ANNUAL_AMOUNT")),
                CsvUtils.parseDate(CsvUtils.getString(r, "START_DATE"), DATE_FORMATS),
                CsvUtils.parseDate(CsvUtils.getString(r, "END_DATE"), DATE_FORMATS),
                CsvUtils.getString(r, "AREA_EN", "AREA_NAME_EN", "AREA_NAME"),
                CsvUtils.getString(r, "PROJECT_EN", "PROJECT_NAME"),
                CsvUtils.getString(r, "USAGE_EN", "USAGE"),
                CsvUtils.getString(r, "PROP_TYPE_EN", "PROPERTY_TYPE"),
                CsvUtils.getString(r, "ROOMS_EN", "ROOMS"),
                CsvUtils.parseBigDecimal(CsvUtils.getString(r, "ACTUAL_AREA", "AREA"))
        );
    }
}
