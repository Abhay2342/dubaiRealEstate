package com.abhay.dubairealestate.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RentTransactionResponse(
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
