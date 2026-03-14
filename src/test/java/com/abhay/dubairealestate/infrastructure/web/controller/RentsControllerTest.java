package com.abhay.dubairealestate.infrastructure.web.controller;

import com.abhay.dubairealestate.application.port.in.QueryRentsAveragePricesUseCase;
import com.abhay.dubairealestate.application.port.in.QueryRentsUseCase;
import com.abhay.dubairealestate.application.port.in.UploadRentsUseCase;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.abhay.dubairealestate.infrastructure.csv.CsvParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice tests for {@link RentsController}.
 * Verifies HTTP routing, request mapping, response structure, and error handling.
 */
@WebMvcTest(RentsController.class)
@TestPropertySource(properties = "spring.ai.mcp.server.enabled=false")
class RentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UploadRentsUseCase uploadRentsUseCase;
    @MockitoBean private QueryRentsUseCase queryRentsUseCase;
    @MockitoBean private QueryRentsAveragePricesUseCase queryRentsAveragePricesUseCase;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private RentTransaction sampleRent() {
        return new RentTransaction(1L, LocalDate.of(2025, 1, 15),
                new BigDecimal("60000"), LocalDate.of(2025, 2, 1), LocalDate.of(2026, 1, 31),
                "Downtown Dubai", "Burj Khalifa", "Residential", "Apartment", "1 B/R",
                new BigDecimal("75.50"));
    }

    // ---------------------------------------------------------------
    // POST /api/rents/upload
    // ---------------------------------------------------------------

    @Test
    void uploadRents_validFile_returns200WithImportedCount() throws Exception {
        when(uploadRentsUseCase.uploadRents(any())).thenReturn(5);

        MockMultipartFile file = new MockMultipartFile("file", "rents.csv",
                "text/csv", "CONTRACT_NUM,ANNUAL_AMOUNT\nC001,60000".getBytes());

        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(5))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void uploadRents_csvParseException_returns422() throws Exception {
        when(uploadRentsUseCase.uploadRents(any()))
                .thenThrow(new CsvParseException("Bad header", new RuntimeException()));

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv",
                "text/csv", "garbage".getBytes());

        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("CSV parsing failed"));
    }

    @Test
    void uploadRents_zeroRecordsImported_stillReturns200() throws Exception {
        when(uploadRentsUseCase.uploadRents(any())).thenReturn(0);

        MockMultipartFile file = new MockMultipartFile("file", "empty.csv",
                "text/csv", "CONTRACT_NUM,ANNUAL_AMOUNT\n".getBytes());

        mockMvc.perform(multipart("/api/rents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(0));
    }

    // ---------------------------------------------------------------
    // GET /api/rents
    // ---------------------------------------------------------------

    @Test
    void getRents_noFilters_returns200WithPageContent() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRent()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/rents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$.content[0].annualAmount").value(60000));
    }

    @Test
    void getRents_withUsageFilter_passesFilterToUseCase() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/rents").param("usage", "Residential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getRents_withPriceRangeFilter_returns200() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRent()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/rents")
                        .param("minPrice", "40000")
                        .param("maxPrice", "80000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getRents_withAreaSearch_returns200() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleRent()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/rents").param("areaSearch", "Downtown"))
                .andExpect(status().isOk());
    }

    @Test
    void getRents_withPagination_returns200() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 0));

        mockMvc.perform(get("/api/rents").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getRents_emptyPage_returns200WithEmptyContent() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/rents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ---------------------------------------------------------------
    // GET /api/rents/averages
    // ---------------------------------------------------------------

    @Test
    void getRentsAverages_returns200WithList() throws Exception {
        List<AreaAveragePrice> averages = List.of(
                new AreaAveragePrice("Downtown Dubai", new BigDecimal("72000.00"), 340L),
                new AreaAveragePrice("Marina", new BigDecimal("54000.00"), 200L)
        );
        when(queryRentsAveragePricesUseCase.getAverageRentsPrices()).thenReturn(averages);

        mockMvc.perform(get("/api/rents/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$[0].averagePrice").value(72000.00))
                .andExpect(jsonPath("$[0].transactionCount").value(340))
                .andExpect(jsonPath("$[1].areaName").value("Marina"));
    }

    @Test
    void getRentsAverages_emptyList_returns200WithEmptyArray() throws Exception {
        when(queryRentsAveragePricesUseCase.getAverageRentsPrices()).thenReturn(List.of());

        mockMvc.perform(get("/api/rents/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // GET /api/rents/averages/{area}
    // ---------------------------------------------------------------

    @Test
    void getRentsAverageByArea_found_returns200WithBody() throws Exception {
        AreaAveragePrice avg = new AreaAveragePrice("Downtown Dubai", new BigDecimal("72000.00"), 340L);
        when(queryRentsAveragePricesUseCase.getAverageRentsPriceByArea("Downtown Dubai"))
                .thenReturn(Optional.of(avg));

        mockMvc.perform(get("/api/rents/averages/Downtown Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$.averagePrice").value(72000.00))
                .andExpect(jsonPath("$.transactionCount").value(340));
    }

    @Test
    void getRentsAverageByArea_notFound_returns404() throws Exception {
        when(queryRentsAveragePricesUseCase.getAverageRentsPriceByArea("Unknown Area"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rents/averages/Unknown Area"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRentsAverageByArea_urlEncodedAreaName_returns200() throws Exception {
        AreaAveragePrice avg = new AreaAveragePrice("Bur Dubai", new BigDecimal("45000.00"), 50L);
        when(queryRentsAveragePricesUseCase.getAverageRentsPriceByArea("Bur Dubai"))
                .thenReturn(Optional.of(avg));

        mockMvc.perform(get("/api/rents/averages/Bur Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaName").value("Bur Dubai"));
    }

    // ---------------------------------------------------------------
    // Content type
    // ---------------------------------------------------------------

    @Test
    void getRents_returnsJsonContentType() throws Exception {
        when(queryRentsUseCase.queryRents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/rents"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
