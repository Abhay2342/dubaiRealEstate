package com.abhay.dubairealestate.application.port.out;

import com.abhay.dubairealestate.application.port.in.SalesFilter;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: load sales transactions and derived aggregates from persistence.
 */
public interface LoadSalesTransactionsPort {
    Page<SaleTransaction> loadSales(SalesFilter filter, Pageable pageable);
    List<AreaAveragePrice> loadAverageSalesPrices();
    Optional<AreaAveragePrice> loadAverageSalesPriceByArea(String areaName);
    List<String> loadSalesAreas();
    List<SaleTransaction> loadLastSalesByArea(String areaName, int limit);
}
