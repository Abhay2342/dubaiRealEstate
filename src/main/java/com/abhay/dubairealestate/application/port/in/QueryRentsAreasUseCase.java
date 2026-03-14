package com.abhay.dubairealestate.application.port.in;

import java.util.List;

/**
 * Inbound port: retrieve the distinct area names that have rent transactions.
 */
public interface QueryRentsAreasUseCase {
    List<String> getRentsAreas();
}
