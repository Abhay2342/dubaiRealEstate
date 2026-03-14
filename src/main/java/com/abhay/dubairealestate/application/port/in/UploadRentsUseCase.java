package com.abhay.dubairealestate.application.port.in;

import java.io.InputStream;

/**
 * Inbound port: upload and persist a rent transactions CSV file.
 * Returns the number of records imported.
 */
public interface UploadRentsUseCase {
    int uploadRents(InputStream csvInputStream);
}
