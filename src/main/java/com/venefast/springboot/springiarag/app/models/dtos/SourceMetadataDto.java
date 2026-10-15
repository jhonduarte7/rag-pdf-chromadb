package com.venefast.springboot.springiarag.app.models.dtos;

import java.util.Map;

/**
 * Data Transfer Object representing detailed provenance metadata for a retrieved RAG context chunk.
 *
 * @param fileName The original source PDF file name.
 * @param pageNumber The 1-based page number where the chunk originated in the PDF.
 * @param startLine The starting line number of the chunk text within the page.
 * @param endLine The ending line number of the chunk text within the page.
 * @param snippet The verbatim text chunk retrieved from ChromaDB.
 * @param metadata Extended key-value metadata properties associated with the document chunk.
 */
public record SourceMetadataDto(
        String fileName,
        Integer pageNumber,
        Integer startLine,
        Integer endLine,
        String snippet,
        Map<String, Object> metadata
) {
}
