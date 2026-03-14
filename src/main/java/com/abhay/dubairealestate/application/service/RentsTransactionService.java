package com.abhay.dubairealestate.application.service;

import com.abhay.dubairealestate.application.port.in.*;
import com.abhay.dubairealestate.application.port.out.*;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RentsTransactionService
        implements UploadRentsUseCase,
                   QueryRentsUseCase,
                   QueryRentsAreasUseCase,
                   QueryRentsAveragePricesUseCase,
                   QueryLastRentsTransactionsUseCase {

    private final ParseRentsCsvPort parseRentsCsvPort;
    private final SaveRentsTransactionsPort saveRentsTransactionsPort;
    private final LoadRentsTransactionsPort loadRentsTransactionsPort;

    @Override
    @Transactional
    public int uploadRents(InputStream csvInputStream) {
        List<RentTransaction> transactions = parseRentsCsvPort.parseRentsCsv(csvInputStream);
        saveRentsTransactionsPort.saveRentsTransactions(transactions);
        return transactions.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentTransaction> queryRents(RentsFilter filter, Pageable pageable) {
        return loadRentsTransactionsPort.loadRents(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRentsAreas() {
        return loadRentsTransactionsPort.loadRentsAreas();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaAveragePrice> getAverageRentsPrices() {
        return loadRentsTransactionsPort.loadAverageRentsPrices();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AreaAveragePrice> getAverageRentsPriceByArea(String areaName) {
        return loadRentsTransactionsPort.loadAverageRentsPriceByArea(areaName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentTransaction> getLastRentsByArea(String areaName, int limit) {
        return loadRentsTransactionsPort.loadLastRentsByArea(areaName, limit);
    }
}
