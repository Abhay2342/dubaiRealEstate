package com.abhay.dubairealestate.infrastructure.web.controller;

import com.abhay.dubairealestate.application.port.in.*;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import com.abhay.dubairealestate.infrastructure.web.dto.AreaAveragePriceResponse;
import com.abhay.dubairealestate.infrastructure.web.dto.SaleTransactionResponse;
import com.abhay.dubairealestate.infrastructure.web.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final UploadSalesUseCase uploadSalesUseCase;
    private final QuerySalesUseCase querySalesUseCase;
    private final QuerySalesAveragePricesUseCase querySalesAveragePricesUseCase;

    /**
     * POST /api/sales/upload
     * Upload a Dubai DLD sales transactions CSV file.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadSales(
            @RequestParam("file") MultipartFile file) throws IOException {

        int count = uploadSalesUseCase.uploadSales(file.getInputStream());
        return ResponseEntity.ok(new UploadResponse(count, "Sales CSV processed successfully. New records saved; duplicates automatically skipped."));
    }

    /**
     * GET /api/sales
     * Query sales transactions with optional filters and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<SaleTransactionResponse>> getSales(
            @RequestParam(required = false) String usage,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String areaSearch,
            @RequestParam(required = false) String projectSearch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SalesFilter filter = new SalesFilter(usage, minPrice, maxPrice, areaSearch, projectSearch);
        Page<SaleTransaction> result = querySalesUseCase.querySales(
                filter, PageRequest.of(page, size, Sort.by("transDate").descending()));

        return ResponseEntity.ok(result.map(this::toResponse));
    }

    /**
     * GET /api/sales/averages
     * Get average sales price per area.
     */
    @GetMapping("/averages")
    public ResponseEntity<List<AreaAveragePriceResponse>> getSalesAverages() {
        List<AreaAveragePrice> averages = querySalesAveragePricesUseCase.getAverageSalesPrices();
        return ResponseEntity.ok(averages.stream().map(this::toAvgResponse).toList());
    }

    /**
     * GET /api/sales/averages/{area}
     * Get average sales price for a specific area.
     */
    @GetMapping("/averages/{area}")
    public ResponseEntity<AreaAveragePriceResponse> getSalesAverageByArea(
            @PathVariable String area) {
        return querySalesAveragePricesUseCase.getAverageSalesPriceByArea(area)
                .map(this::toAvgResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private SaleTransactionResponse toResponse(SaleTransaction d) {
        return new SaleTransactionResponse(
                d.id(), d.transId(), d.transDate(), d.transValue(),
                d.areaName(), d.projectName(), d.usage(), d.registrationType(),
                d.propertyType(), d.rooms(), d.actualArea(), d.meterSalePrice());
    }

    private AreaAveragePriceResponse toAvgResponse(AreaAveragePrice d) {
        return new AreaAveragePriceResponse(d.areaName(), d.averagePrice(), d.transactionCount());
    }
}
