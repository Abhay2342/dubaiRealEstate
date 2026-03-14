package com.abhay.dubairealestate.application.port.out;

import com.abhay.dubairealestate.domain.model.SaleTransaction;

import java.util.List;

/**
 * Outbound port: persist a batch of sales transactions.
 */
public interface SaveSalesTransactionsPort {
    void saveSalesTransactions(List<SaleTransaction> transactions);
}
