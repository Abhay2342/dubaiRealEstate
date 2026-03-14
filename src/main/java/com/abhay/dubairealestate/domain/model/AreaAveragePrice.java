package com.abhay.dubairealestate.domain.model;

import java.math.BigDecimal;

/**
 * Aggregated average price value object for a given area.
 */
public record AreaAveragePrice(
        String areaName,
        BigDecimal averagePrice,
        long transactionCount
) {}
