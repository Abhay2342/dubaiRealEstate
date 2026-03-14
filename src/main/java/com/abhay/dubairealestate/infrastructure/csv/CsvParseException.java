package com.abhay.dubairealestate.infrastructure.csv;

/**
 * Thrown when a CSV file cannot be parsed (e.g. malformed structure, wrong encoding).
 * Individual bad rows are skipped with a warning; this exception signals a fatal parse failure.
 */
public class CsvParseException extends RuntimeException {

    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
