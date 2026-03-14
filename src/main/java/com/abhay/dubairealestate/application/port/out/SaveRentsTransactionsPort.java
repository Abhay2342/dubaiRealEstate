package com.abhay.dubairealestate.application.port.out;

import com.abhay.dubairealestate.domain.model.RentTransaction;

import java.util.List;

/**
 * Outbound port: persist a batch of rent transactions.
 */
public interface SaveRentsTransactionsPort {
    void saveRentsTransactions(List<RentTransaction> transactions);
}
