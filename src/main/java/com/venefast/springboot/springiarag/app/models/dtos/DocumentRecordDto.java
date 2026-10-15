package com.venefast.springboot.springiarag.app.models.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object representing audit and tracking metadata for an ingested document.
 *
 * @param id The unique identifier of the document record.
 * @param fileName The original file name of the uploaded document.
 * @param contentType The MIME type of the uploaded file.
 * @param fileSize The file size in bytes.
 * @param totalChunks The total number of token chunks generated and stored in ChromaDB.
 * @param status Processing status (e.g., "INDEXED", "FAILED").
 */
public record DocumentRecordDto(
        Long id,
        @NotBlank(message = "File name is required")
        String fileName,
        String contentType,
        Long fileSize,
        Integer totalChunks,
        String status
) {
}
