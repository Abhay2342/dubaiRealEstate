package com.abhay.dubairealestate.infrastructure.web.controller;

import com.abhay.dubairealestate.application.port.in.QuerySalesAveragePricesUseCase;
import com.abhay.dubairealestate.application.port.in.QuerySalesUseCase;
import com.abhay.dubairealestate.application.port.in.UploadSalesUseCase;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
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
 * Web slice tests for {@link SalesController}.
 * Verifies HTTP routing, request mapping, response structure, and error handling.
 */
@WebMvcTest(SalesController.class)
@TestPropertySource(properties = "spring.ai.mcp.server.enabled=false")
class SalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UploadSalesUseCase uploadSalesUseCase;
    @MockitoBean private QuerySalesUseCase querySalesUseCase;
    @MockitoBean private QuerySalesAveragePricesUseCase querySalesAveragePricesUseCase;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private SaleTransaction sampleSale() {
        return new SaleTransaction(1L, "T001", LocalDate.of(2025, 3, 10),
                new BigDecimal("2500000"), "Downtown Dubai", "Emaar Tower",
                "Residential", "Sales", "Apartment", "2 B/R",
                new BigDecimal("120.50"), new BigDecimal("20746.89"));
    }

    // ---------------------------------------------------------------
    // POST /api/sales/upload
    // ---------------------------------------------------------------

    @Test
    void uploadSales_validFile_returns200WithImportedCount() throws Exception {
        when(uploadSalesUseCase.uploadSales(any())).thenReturn(10);

        MockMultipartFile file = new MockMultipartFile("file", "sales.csv",
                "text/csv", "TRANS_ID,TRANS_VALUE\nT001,2500000".getBytes());

        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(10))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void uploadSales_csvParseException_returns422() throws Exception {
        when(uploadSalesUseCase.uploadSales(any()))
                .thenThrow(new CsvParseException("Invalid column", new RuntimeException()));

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv",
                "text/csv", "garbage".getBytes());

        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("CSV parsing failed"));
    }

    @Test
    void uploadSales_zeroRecordsImported_stillReturns200() throws Exception {
        when(uploadSalesUseCase.uploadSales(any())).thenReturn(0);

        MockMultipartFile file = new MockMultipartFile("file", "empty.csv",
                "text/csv", "TRANS_ID,TRANS_VALUE\n".getBytes());

        mockMvc.perform(multipart("/api/sales/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsImported").value(0));
    }

    // ---------------------------------------------------------------
    // GET /api/sales
    // ---------------------------------------------------------------

    @Test
    void getSales_noFilters_returns200WithPageContent() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleSale()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].transId").value("T001"))
                .andExpect(jsonPath("$.content[0].areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$.content[0].transValue").value(2500000));
    }

    @Test
    void getSales_withUsageFilter_returns200() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/sales").param("usage", "Residential"))
                .andExpect(status().isOk());
    }

    @Test
    void getSales_withPriceRangeFilter_returns200() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleSale()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/sales")
                        .param("minPrice", "1000000")
                        .param("maxPrice", "5000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getSales_withProjectSearch_returns200() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleSale()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/sales").param("projectSearch", "Emaar"))
                .andExpect(status().isOk());
    }

    @Test
    void getSales_withPagination_returns200() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/sales").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void getSales_emptyPage_returns200WithEmptyContent() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getSales_responseIncludesMeterSalePrice() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleSale()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].meterSalePrice").value(20746.89));
    }

    // ---------------------------------------------------------------
    // GET /api/sales/averages
    // ---------------------------------------------------------------

    @Test
    void getSalesAverages_returns200WithList() throws Exception {
        List<AreaAveragePrice> averages = List.of(
                new AreaAveragePrice("Downtown Dubai", new BigDecimal("2500000.00"), 120L),
                new AreaAveragePrice("Marina", new BigDecimal("1800000.00"), 75L)
        );
        when(querySalesAveragePricesUseCase.getAverageSalesPrices()).thenReturn(averages);

        mockMvc.perform(get("/api/sales/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$[0].averagePrice").value(2500000.00))
                .andExpect(jsonPath("$[0].transactionCount").value(120))
                .andExpect(jsonPath("$[1].areaName").value("Marina"));
    }

    @Test
    void getSalesAverages_emptyList_returns200WithEmptyArray() throws Exception {
        when(querySalesAveragePricesUseCase.getAverageSalesPrices()).thenReturn(List.of());

        mockMvc.perform(get("/api/sales/averages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // GET /api/sales/averages/{area}
    // ---------------------------------------------------------------

    @Test
    void getSalesAverageByArea_found_returns200WithBody() throws Exception {
        AreaAveragePrice avg = new AreaAveragePrice("Downtown Dubai", new BigDecimal("2500000.00"), 120L);
        when(querySalesAveragePricesUseCase.getAverageSalesPriceByArea("Downtown Dubai"))
                .thenReturn(Optional.of(avg));

        mockMvc.perform(get("/api/sales/averages/Downtown Dubai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaName").value("Downtown Dubai"))
                .andExpect(jsonPath("$.averagePrice").value(2500000.00))
                .andExpect(jsonPath("$.transactionCount").value(120));
    }

    @Test
    void getSalesAverageByArea_notFound_returns404() throws Exception {
        when(querySalesAveragePricesUseCase.getAverageSalesPriceByArea("Unknown"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sales/averages/Unknown"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // Content type
    // ---------------------------------------------------------------

    @Test
    void getSales_returnsJsonContentType() throws Exception {
        when(querySalesUseCase.querySales(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/sales"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
