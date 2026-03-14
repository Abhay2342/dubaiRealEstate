package com.abhay.dubairealestate.application.port.in;

import com.abhay.dubairealestate.domain.model.RentTransaction;

import java.util.List;

/**
 * Inbound port: retrieve the most recent rent transactions for a given area.
 * Used by MCP tooling.
 */
public interface QueryLastRentsTransactionsUseCase {
    List<RentTransaction> getLastRentsByArea(String areaName, int limit);
}
