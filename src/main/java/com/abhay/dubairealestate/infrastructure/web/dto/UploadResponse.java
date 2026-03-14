package com.abhay.dubairealestate.infrastructure.web.dto;

public record UploadResponse(
        int recordsImported,
        String message
) {}
