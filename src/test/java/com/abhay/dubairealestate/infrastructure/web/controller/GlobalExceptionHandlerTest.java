package com.abhay.dubairealestate.infrastructure.web.controller;

import com.abhay.dubairealestate.infrastructure.csv.CsvParseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * Instantiated directly (no Spring context needed) to verify each handler
 * returns the correct HTTP status and body structure.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ---------------------------------------------------------------
    // CsvParseException  →  422 Unprocessable Entity
    // ---------------------------------------------------------------

    @Test
    void handleCsvParseException_returns422WithErrorAndDetail() {
        CsvParseException ex = new CsvParseException("Bad CSV format", new RuntimeException("root cause"));

        ResponseEntity<Map<String, String>> response = handler.handleCsvParseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody())
                .containsEntry("error", "CSV parsing failed")
                .containsKey("detail");
    }

    @Test
    void handleCsvParseException_detailContainsExceptionMessage() {
        CsvParseException ex = new CsvParseException("Missing required column", new RuntimeException());

        ResponseEntity<Map<String, String>> response = handler.handleCsvParseException(ex);

        assertThat(response.getBody()).extractingByKey("detail")
                .asString().contains("Missing required column");
    }

    // ---------------------------------------------------------------
    // IOException  →  400 Bad Request
    // ---------------------------------------------------------------

    @Test
    void handleIoException_returns400WithErrorAndDetail() {
        IOException ex = new IOException("Stream closed unexpectedly");

        ResponseEntity<Map<String, String>> response = handler.handleIoException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("error", "Could not read uploaded file")
                .containsKey("detail");
    }

    @Test
    void handleIoException_detailContainsIoMessage() {
        IOException ex = new IOException("Connection reset by peer");

        ResponseEntity<Map<String, String>> response = handler.handleIoException(ex);

        assertThat(response.getBody()).extractingByKey("detail")
                .asString().contains("Connection reset by peer");
    }

    // ---------------------------------------------------------------
    // MaxUploadSizeExceededException  →  413 Payload Too Large
    // ---------------------------------------------------------------

    @Test
    void handleMaxUploadSize_returns413WithErrorMessage() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(100_000_000L);

        ResponseEntity<Map<String, String>> response = handler.handleMaxUploadSize(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("error", "File too large");
    }

    @Test
    void handleMaxUploadSize_bodyContainsDetail() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50_000_000L);

        ResponseEntity<Map<String, String>> response = handler.handleMaxUploadSize(ex);

        assertThat(response.getBody()).containsKey("detail");
    }

    // ---------------------------------------------------------------
    // IllegalArgumentException  →  400 Bad Request
    // ---------------------------------------------------------------

    @Test
    void handleIllegalArgument_returns400WithBadRequestAndDetail() {
        IllegalArgumentException ex = new IllegalArgumentException("limit must be positive");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("error", "Bad request")
                .containsEntry("detail", "limit must be positive");
    }

    @Test
    void handleIllegalArgument_nullMessage_returnsFallbackDetail() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("error", "Bad request")
                .containsKey("detail");
        assertThat(response.getBody().get("detail")).isNotNull();
    }
}
