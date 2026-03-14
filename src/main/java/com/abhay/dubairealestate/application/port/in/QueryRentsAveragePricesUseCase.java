package com.abhay.dubairealestate.application.port.in;

import com.abhay.dubairealestate.domain.model.AreaAveragePrice;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: query average rent prices, either globally or per area.
 */
public interface QueryRentsAveragePricesUseCase {
    List<AreaAveragePrice> getAverageRentsPrices();
    Optional<AreaAveragePrice> getAverageRentsPriceByArea(String areaName);
}
