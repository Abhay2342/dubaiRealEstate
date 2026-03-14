package com.abhay.dubairealestate.application.service;

import com.abhay.dubairealestate.application.port.in.*;
import com.abhay.dubairealestate.application.port.out.*;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
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
public class SalesTransactionService
        implements UploadSalesUseCase,
                   QuerySalesUseCase,
                   QuerySalesAreasUseCase,
                   QuerySalesAveragePricesUseCase,
                   QueryLastSalesTransactionsUseCase {

    private final ParseSalesCsvPort parseSalesCsvPort;
    private final SaveSalesTransactionsPort saveSalesTransactionsPort;
    private final LoadSalesTransactionsPort loadSalesTransactionsPort;

    @Override
    @Transactional
    public int uploadSales(InputStream csvInputStream) {
        List<SaleTransaction> transactions = parseSalesCsvPort.parseSalesCsv(csvInputStream);
        saveSalesTransactionsPort.saveSalesTransactions(transactions);
        return transactions.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransaction> querySales(SalesFilter filter, Pageable pageable) {
        return loadSalesTransactionsPort.loadSales(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSalesAreas() {
        return loadSalesTransactionsPort.loadSalesAreas();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaAveragePrice> getAverageSalesPrices() {
        return loadSalesTransactionsPort.loadAverageSalesPrices();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AreaAveragePrice> getAverageSalesPriceByArea(String areaName) {
        return loadSalesTransactionsPort.loadAverageSalesPriceByArea(areaName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleTransaction> getLastSalesByArea(String areaName, int limit) {
        return loadSalesTransactionsPort.loadLastSalesByArea(areaName, limit);
    }
}
