package com.venefast.springboot.springiarag.app.models.dtos;

import java.util.List;

/**
 * Data Transfer Object representing the AI chat response with rich provenance and metadata.
 *
 * @param question The original user question submitted for RAG querying.
 * @param answer The generated response synthesized using context chunks retrieved from ChromaDB, formatted in Markdown.
 * @param sourceFiles Distinct list of original document file names that contributed context to this response.
 * @param sources Detailed list of context source references including page, start line, end line, snippet, and metadata.
 */
public record ChatResponseDto(
        String question,
        String answer,
        List<String> sourceFiles,
        List<SourceMetadataDto> sources
) {
    /**
     * Backward-compatible convenience constructor.
     */
    public ChatResponseDto(String question, String answer, List<String> sourceFiles) {
        this(question, answer, sourceFiles, List.of());
    }
}
