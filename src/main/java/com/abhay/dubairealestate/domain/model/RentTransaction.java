package com.abhay.dubairealestate.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Core domain model for a property rent transaction.
 * Zero external dependencies – pure Java record.
 */
public record RentTransaction(
        Long id,
        LocalDate contractDate,
        BigDecimal annualAmount,
        LocalDate startDate,
        LocalDate endDate,
        String areaName,
        String projectName,
        String usage,
        String propertyType,
        String rooms,
        BigDecimal actualArea
) {}
