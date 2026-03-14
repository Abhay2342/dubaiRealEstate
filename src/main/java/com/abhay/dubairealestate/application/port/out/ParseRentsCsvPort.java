package com.abhay.dubairealestate.application.port.out;

import com.abhay.dubairealestate.domain.model.RentTransaction;

import java.io.InputStream;
import java.util.List;

/**
 * Outbound port: parse a rent transactions CSV stream into domain objects.
 * Implemented by the CSV adapter in the infrastructure layer.
 */
public interface ParseRentsCsvPort {
    List<RentTransaction> parseRentsCsv(InputStream inputStream);
}
