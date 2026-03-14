package com.abhay.dubairealestate.application.port.in;

import com.abhay.dubairealestate.domain.model.AreaAveragePrice;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: query average sales prices, either globally or per area.
 */
public interface QuerySalesAveragePricesUseCase {
    List<AreaAveragePrice> getAverageSalesPrices();
    Optional<AreaAveragePrice> getAverageSalesPriceByArea(String areaName);
}
