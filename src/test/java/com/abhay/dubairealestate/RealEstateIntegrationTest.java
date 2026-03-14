package com.abhay.dubairealestate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests using a real PostgreSQL 15 instance managed by
 * Testcontainers. Verifies the complete pipeline from HTTP request through the
 * application layers and Flyway-migrated schema.
 *
 * <p>Requires Docker to be available on the host machine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RealEstateIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------------
    // Test CSV payloads
    // ---------------------------------------------------------------

    private static final String SALES_CSV = """
            TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA,METER_SALE_PRICE
            T001,2025-03-10,2500000,Downtown Dubai,Emaar Tower,Residential,Sales,Apartment,2 B/R,120.50,20746.89
            T002,2025-02-15,1800000,Marina,Marina Vista,Residential,Sales,Apartment,1 B/R,90.00,20000.00
            T003,2025-01-20,5000000,Palm Jumeirah,Atlantis Residences,Residential,Sales,Villa,4 B/R,350.00,14285.71
            T001,2025-03-10,2500000,Downtown Dubai,Emaar Tower,Residential,Sales,Apartment,2 B/R,120.50,20746.89
            """;

    private static final String RENTS_CSV = """
            CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA
            C001,2025-01-15,60000,2025-02-01,2026-01-31,Downtown Dubai,Burj Khalifa,Residential,Apartment,1 B/R,75.50
            C002,2025-02-20,90000,2025-03-01,2026-02-28,Marina,Marina Towers,Residential,Apartment,2 B/R,120.00
            C003,2024-12-01,45000,2025-01-01,2025-12-31,Deira,Gold Building,Residential,Studio,Studio,45.00
            """;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM sale_transactions");
        jdbcTemplate.execute("DELETE FROM rent_transactions");
    }

    // ---------------------------------------------------------------
    // Sales: upload
    // ---------------------------------------------------------------

    @Test
    void uploadSales_validCsv_returns200AndImportsUniqueRows() throws Exception {
        // Row T001 appears twice — the duplicate should be silently skipped.
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv",
                "text/csv", SALES_CSV.getBytes());

        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk())
                // Parser sees 4 data rows (including duplicate) → returns 4 parsed
                .andExpect(jsonPath("$.recordsImported").value(4))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void uploadSales_idempotent_duplicateUploadDoesNotFail() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv",
                "text/csv", SALES_CSV.getBytes());

        // First upload
        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk());

        // Second upload of the same file — ON CONFLICT DO NOTHING in effect
        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Sales: query
    // ---------------------------------------------------------------

    @Test
    void querySales_afterUpload_returnsPageWithTransactions() throws Exception {
        uploadSalesCsv();

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].transId").exists())
                .andExpect(jsonPath("$.content[0].areaName").exists());
    }

    @Test
    void querySales_withUsageFilter_returnsOnlyMatchingRows() throws Exception {
        uploadSalesCsv();

        mockMvc.perform(get("/api/sales").param("usage", "Residential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void querySales_withAreaSearch_returnsMatchingRows() throws Exception {
        uploadSalesCsv();

        mockMvc.perform(get("/api/sales").param("areaSearch", "Downtown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void querySales_withMinPriceFilter_returnsOnlyExpensiveTransactions() throws Exception {
        uploadSalesCsv();

        // Only the villa (5_000_000) should be above 3_000_000
        mockMvc.perform(get("/api/sales").param("minPrice", "3000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ---------------------------------------------------------------
    // Sales: averages
    // ---------------------------------------------------------------

    @Test
    void getSalesAverages_afterUpload_returnsListWithAreaAverages() throws Exception {
        uploadSalesCsv();

        mockMvc.perform(get("/api/sales/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].areaName").exists())
                .andExpect(jsonPath("$[0].averagePrice").exists())
                .andExpect(jsonPath("$[0].transactionCount").exists());
    }

    @Test
    void getSalesAverageByArea_existingArea_returns200() throws Exception {
        uploadSalesCsv();

        mockMvc.perform(get("/api/sales/averages/Downtown Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$.averagePrice").exists())
                .andExpect(jsonPath("$.transactionCount").value(1));
    }

    @Test
    void getSalesAverageByArea_nonExistentArea_returns404() throws Exception {
        uploadSalesCsv();

        mockMvc.perform(get("/api/sales/averages/NonExistentArea"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // Rents: upload
    // ---------------------------------------------------------------

    @Test
    void uploadRents_validCsv_returns200AndImportsRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rents.csv",
                "text/csv", RENTS_CSV.getBytes());

        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(3))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void uploadRents_idempotent_duplicateUploadDoesNotFail() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rents.csv",
                "text/csv", RENTS_CSV.getBytes());

        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // Rents: query
    // ---------------------------------------------------------------

    @Test
    void queryRents_afterUpload_returnsPageWithTransactions() throws Exception {
        uploadRentsCsv();

        mockMvc.perform(get("/api/rents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].areaName").exists());
    }

    @Test
    void queryRents_withUsageFilter_returnsOnlyMatchingRows() throws Exception {
        uploadRentsCsv();

        mockMvc.perform(get("/api/rents").param("usage", "Residential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void queryRents_withMaxPriceFilter_returnsAffordableTransactions() throws Exception {
        uploadRentsCsv();

        // Only Studio at 45_000 should fall below 50_000
        mockMvc.perform(get("/api/rents").param("maxPrice", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ---------------------------------------------------------------
    // Rents: averages
    // ---------------------------------------------------------------

    @Test
    void getRentsAverages_afterUpload_returnsListWithAreaAverages() throws Exception {
        uploadRentsCsv();

        mockMvc.perform(get("/api/rents/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].areaName").exists())
                .andExpect(jsonPath("$[0].averagePrice").exists())
                .andExpect(jsonPath("$[0].transactionCount").exists());
    }

    @Test
    void getRentsAverageByArea_existingArea_returns200() throws Exception {
        uploadRentsCsv();

        mockMvc.perform(get("/api/rents/averages/Downtown Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$.transactionCount").value(1));
    }

    @Test
    void getRentsAverageByArea_nonExistentArea_returns404() throws Exception {
        uploadRentsCsv();

        mockMvc.perform(get("/api/rents/averages/NonExistentArea"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // Error handling
    // ---------------------------------------------------------------

    @Test
    void uploadSales_emptyFile_returns200WithZeroCount() throws Exception {
        String headerOnly = "TRANS_ID,INSTANCE_DATE,TRANS_VALUE,AREA_EN,PROJECT_EN,USAGE_EN,REG_TYPE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA\n";
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv",
                "text/csv", headerOnly.getBytes());

        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(0));
    }

    @Test
    void uploadRents_emptyFile_returns200WithZeroCount() throws Exception {
        String headerOnly = "CONTRACT_NUM,INSTANCE_DATE,ANNUAL_AMOUNT,START_DATE,END_DATE,AREA_EN,PROJECT_EN,USAGE_EN,PROP_TYPE_EN,ROOMS_EN,ACTUAL_AREA\n";
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv",
                "text/csv", headerOnly.getBytes());

        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(0));
    }

    @Test
    void querySales_emptyDatabase_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void queryRents_emptyDatabase_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/rents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getSalesAverages_emptyDatabase_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/sales/averages"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getRentsAverages_emptyDatabase_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/rents/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private void uploadSalesCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sales.csv",
                "text/csv", SALES_CSV.getBytes());
        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk());
    }

    private void uploadRentsCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rents.csv",
                "text/csv", RENTS_CSV.getBytes());
        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk());
    }
}
