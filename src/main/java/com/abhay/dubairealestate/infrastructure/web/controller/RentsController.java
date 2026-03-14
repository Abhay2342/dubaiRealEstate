package com.abhay.dubairealestate.infrastructure.web.controller;

import com.abhay.dubairealestate.application.port.in.*;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.abhay.dubairealestate.infrastructure.web.dto.AreaAveragePriceResponse;
import com.abhay.dubairealestate.infrastructure.web.dto.RentTransactionResponse;
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
@RequestMapping("/api/rents")
@RequiredArgsConstructor
public class RentsController {

    private final UploadRentsUseCase uploadRentsUseCase;
    private final QueryRentsUseCase queryRentsUseCase;
    private final QueryRentsAveragePricesUseCase queryRentsAveragePricesUseCase;

    /**
     * POST /api/rents/upload
     * Upload a Dubai DLD rent transactions CSV file.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadRents(
            @RequestParam("file") MultipartFile file) throws IOException {

        int count = uploadRentsUseCase.uploadRents(file.getInputStream());
        return ResponseEntity.ok(new UploadResponse(count, "Rents CSV processed successfully. New records saved; duplicates automatically skipped."));
    }

    /**
     * GET /api/rents
     * Query rent transactions with optional filters and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<RentTransactionResponse>> getRents(
            @RequestParam(required = false) String usage,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String areaSearch,
            @RequestParam(required = false) String projectSearch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        RentsFilter filter = new RentsFilter(usage, minPrice, maxPrice, areaSearch, projectSearch);
        Page<RentTransaction> result = queryRentsUseCase.queryRents(
                filter, PageRequest.of(page, size, Sort.by("contractDate").descending()));

        return ResponseEntity.ok(result.map(this::toResponse));
    }

    /**
     * GET /api/rents/averages
     * Get average rent price per area.
     */
    @GetMapping("/averages")
    public ResponseEntity<List<AreaAveragePriceResponse>> getRentsAverages() {
        List<AreaAveragePrice> averages = queryRentsAveragePricesUseCase.getAverageRentsPrices();
        return ResponseEntity.ok(averages.stream().map(this::toAvgResponse).toList());
    }

    /**
     * GET /api/rents/averages/{area}
     * Get average rent price for a specific area.
     */
    @GetMapping("/averages/{area}")
    public ResponseEntity<AreaAveragePriceResponse> getRentsAverageByArea(
            @PathVariable String area) {
        return queryRentsAveragePricesUseCase.getAverageRentsPriceByArea(area)
                .map(this::toAvgResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private RentTransactionResponse toResponse(RentTransaction d) {
        return new RentTransactionResponse(
                d.id(), d.contractDate(), d.annualAmount(),
                d.startDate(), d.endDate(), d.areaName(), d.projectName(),
                d.usage(), d.propertyType(), d.rooms(), d.actualArea());
    }

    private AreaAveragePriceResponse toAvgResponse(AreaAveragePrice d) {
        return new AreaAveragePriceResponse(d.areaName(), d.averagePrice(), d.transactionCount());
    }
}
