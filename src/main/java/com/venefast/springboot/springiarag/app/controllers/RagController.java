package com.venefast.springboot.springiarag.app.controllers;

import com.venefast.springboot.springiarag.app.models.dtos.ChatRequestDto;
import com.venefast.springboot.springiarag.app.models.dtos.ChatResponseDto;
import com.venefast.springboot.springiarag.app.models.dtos.DocumentRecordDto;
import com.venefast.springboot.springiarag.app.services.RagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller exposing endpoints for RAG document operations and contextual AI chat.
 *
 * <p>Base path: <code>/api/rag</code></p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    /**
     * Constructs the RagController with required business service dependencies.
     *
     * @param ragService the RAG application service
     */
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * Uploads and indexes a PDF document into ChromaDB.
     *
     * @param file the binary PDF multipart upload
     * @return 201 CREATED with the metadata of the indexed document record
     */
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentRecordDto> uploadDocument(@RequestParam("file") MultipartFile file) {
        DocumentRecordDto recordDto = ragService.ingestPdf(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(recordDto);
    }

    /**
     * Retrieves the audit history of all ingested documents.
     *
     * @return 200 OK with a list of all stored document records
     */
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentRecordDto>> getDocuments() {
        List<DocumentRecordDto> documents = ragService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * Executes a contextual RAG chat query against indexed documents in ChromaDB.
     *
     * @param request the validated chat request containing the user's question and optional topK count
     * @return 200 OK with the generated answer and associated source file names
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> askQuestion(@Valid @RequestBody ChatRequestDto request) {
        ChatResponseDto response = ragService.askQuestion(request);
        return ResponseEntity.ok(response);
    }
}
