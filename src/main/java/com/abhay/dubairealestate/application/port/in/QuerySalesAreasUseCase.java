package com.abhay.dubairealestate.application.port.in;

import java.util.List;

/**
 * Inbound port: retrieve the distinct area names that have sales transactions.
 */
public interface QuerySalesAreasUseCase {
    List<String> getSalesAreas();
}
