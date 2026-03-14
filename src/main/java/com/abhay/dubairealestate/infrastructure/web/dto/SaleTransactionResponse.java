package com.abhay.dubairealestate.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SaleTransactionResponse(
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
