package com.abhay.dubairealestate.application.port.out;

import com.abhay.dubairealestate.application.port.in.RentsFilter;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: load rent transactions and derived aggregates from persistence.
 */
public interface LoadRentsTransactionsPort {
    Page<RentTransaction> loadRents(RentsFilter filter, Pageable pageable);
    List<AreaAveragePrice> loadAverageRentsPrices();
    Optional<AreaAveragePrice> loadAverageRentsPriceByArea(String areaName);
    List<String> loadRentsAreas();
    List<RentTransaction> loadLastRentsByArea(String areaName, int limit);
}
