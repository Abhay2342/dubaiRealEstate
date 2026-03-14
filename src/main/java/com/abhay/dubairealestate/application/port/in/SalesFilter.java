package com.abhay.dubairealestate.application.port.in;

import java.math.BigDecimal;

/**
 * Query object for filtering sales transactions.
 *
 * @param usage          filter by property usage (e.g. "Residential", "Commercial")
 * @param minPrice       minimum trans_value (inclusive)
 * @param maxPrice       maximum trans_value (inclusive)
 * @param areaSearch     free-text search on area_name (case-insensitive)
 * @param projectSearch  free-text search on project_name (case-insensitive)
 */
public record SalesFilter(
        String usage,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String areaSearch,
        String projectSearch
) {}
