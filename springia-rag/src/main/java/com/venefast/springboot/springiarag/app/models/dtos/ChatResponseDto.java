package com.venefast.springboot.springiarag.app.models.dtos;

import java.util.List;

/**
 * Data Transfer Object representing the AI chat response.
 *
 * @param question The original user question submitted for RAG querying.
 * @param answer The generated response synthesized by Ollama using context chunks retrieved from ChromaDB.
 * @param sourceFiles Distinct list of original document file names that contributed context to this response.
 */
public record ChatResponseDto(
        String question,
        String answer,
        List<String> sourceFiles
) {
}
