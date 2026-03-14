package com.abhay.dubairealestate.application.port.in;

import java.io.InputStream;

/**
 * Inbound port: upload and persist a sales transactions CSV file.
 * Returns the number of records imported.
 */
public interface UploadSalesUseCase {
    int uploadSales(InputStream csvInputStream);
}
