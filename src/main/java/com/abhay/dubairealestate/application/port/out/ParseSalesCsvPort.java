package com.abhay.dubairealestate.application.port.out;

import com.abhay.dubairealestate.domain.model.SaleTransaction;

import java.io.InputStream;
import java.util.List;

/**
 * Outbound port: parse a sales transactions CSV stream into domain objects.
 * Implemented by the CSV adapter in the infrastructure layer.
 */
public interface ParseSalesCsvPort {
    List<SaleTransaction> parseSalesCsv(InputStream inputStream);
}
