package com.abhay.dubairealestate.application.port.in;

import com.abhay.dubairealestate.domain.model.SaleTransaction;

import java.util.List;

/**
 * Inbound port: retrieve the most recent sales transactions for a given area.
 * Used by MCP tooling.
 */
public interface QueryLastSalesTransactionsUseCase {
    List<SaleTransaction> getLastSalesByArea(String areaName, int limit);
}
