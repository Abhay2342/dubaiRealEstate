package com.abhay.dubairealestate.infrastructure.web.dto;

import java.math.BigDecimal;

public record AreaAveragePriceResponse(
        String areaName,
        BigDecimal averagePrice,
        long transactionCount
) {}
