package com.venefast.springboot.springiarag.app.services;

import com.venefast.springboot.springiarag.app.models.dtos.ChatRequestDto;
import com.venefast.springboot.springiarag.app.models.dtos.ChatResponseDto;
import com.venefast.springboot.springiarag.app.models.dtos.DocumentRecordDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service contract defining RAG operations for PDF ingestion, indexing, and chat queries.
 */
public interface RagService {

    /**
     * Extracts text from an uploaded PDF file, segments content into token chunks,
     * stores embeddings in ChromaDB, and persists an audit record.
     *
     * @param file the binary PDF multipart upload
     * @return the persisted DocumentRecordDto metadata
     */
    DocumentRecordDto ingestPdf(MultipartFile file);

    /**
     * Retrieves all document audit records stored in the relational database.
     *
     * @return list of DocumentRecordDto records
     */
    List<DocumentRecordDto> getAllDocuments();

    /**
     * Queries the AI model with RAG context retrieved from ChromaDB.
     *
     * @param request the chat query request
     * @return ChatResponseDto containing the answer and matching source file names
     */
    ChatResponseDto askQuestion(ChatRequestDto request);
}
