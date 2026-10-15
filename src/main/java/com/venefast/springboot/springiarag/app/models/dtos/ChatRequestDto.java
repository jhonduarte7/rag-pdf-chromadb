package com.venefast.springboot.springiarag.app.models.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable DTO record representing an incoming user chat query.
 *
 * @param question the natural language question to submit to the RAG pipeline (mandatory)
 * @param topK the number of relevant document chunks to retrieve from ChromaDB (defaults to 4 if omitted or invalid)
 */
public record ChatRequestDto(
        @NotBlank(message = "Question is required")
        String question,
        Integer topK
) {
    /**
     * Compact constructor enforcing a sensible default for topK retrieval count.
     */
    public ChatRequestDto {
        if (topK == null || topK <= 0) {
            topK = 4;
        }
    }
}
