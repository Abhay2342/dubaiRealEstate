package com.abhay.dubairealestate.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Core domain model for a property sales transaction.
 * Zero external dependencies – pure Java record.
 */
public record SaleTransaction(
        Long id,
        String transId,
        LocalDate transDate,
        BigDecimal transValue,
        String areaName,
        String projectName,
        String usage,
        String registrationType,
        String propertyType,
        String rooms,
        BigDecimal actualArea,
        BigDecimal meterSalePrice
) {}
