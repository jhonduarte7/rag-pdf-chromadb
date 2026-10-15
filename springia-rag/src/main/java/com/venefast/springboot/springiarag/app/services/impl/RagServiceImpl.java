package com.venefast.springboot.springiarag.app.services.impl;

import com.venefast.springboot.springiarag.app.mappers.DocumentRecordMapper;
import com.venefast.springboot.springiarag.app.models.dtos.ChatRequestDto;
import com.venefast.springboot.springiarag.app.models.dtos.ChatResponseDto;
import com.venefast.springboot.springiarag.app.models.dtos.DocumentRecordDto;
import com.venefast.springboot.springiarag.app.models.entities.DocumentRecord;
import com.venefast.springboot.springiarag.app.repositories.DocumentRecordRepository;
import com.venefast.springboot.springiarag.app.services.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * Production implementation of {@link RagService} orchestrating Spring AI, ChromaDB, and relational persistence.
 *
 * <p>Pipeline overview:
 * <ol>
 *   <li>PDF parsing via Apache PDFBox ({@link PagePdfDocumentReader}).</li>
 *   <li>Token-aware text splitting via {@link TokenTextSplitter}.</li>
 *   <li>Vectorization and storage in ChromaDB via {@link VectorStore}.</li>
 *   <li>Audit persistence in H2/JPA via {@link DocumentRecordRepository}.</li>
 *   <li>RAG inference via {@link ChatClient} coupled with {@link QuestionAnswerAdvisor}.</li>
 * </ol>
 * </p>
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    /**
     * Domain system prompt defining assistant persona, boundaries, and factual accuracy.
     */
    private static final String RAG_ASSISTANT_SYSTEM_PROMPT =
            "You are a helpful, precise AI assistant answering user questions based on the provided context documents. " +
            "If the context does not contain the answer, state that clearly.";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final TokenTextSplitter tokenTextSplitter;
    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentRecordMapper documentRecordMapper;

    /**
     * Dependency injection constructor for RagServiceImpl.
     *
     * @param vectorStore the Chroma vector store interface
     * @param chatClient the Spring AI chat client for model communication
     * @param tokenTextSplitter the token-based document segmenter
     * @param documentRecordRepository Spring Data repository for document audit records
     * @param documentRecordMapper entity-to-DTO conversion component
     */
    public RagServiceImpl(VectorStore vectorStore,
                          ChatClient chatClient,
                          TokenTextSplitter tokenTextSplitter,
                          DocumentRecordRepository documentRecordRepository,
                          DocumentRecordMapper documentRecordMapper) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.tokenTextSplitter = tokenTextSplitter;
        this.documentRecordRepository = documentRecordRepository;
        this.documentRecordMapper = documentRecordMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public DocumentRecordDto ingestPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded PDF file cannot be empty.");
        }

        String originalFilename = file.getOriginalFilename();
        log.info("Processing PDF upload for file: {}", originalFilename);

        try {
            // 1. Read PDF pages into Document objects
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    file.getResource(),
                    PdfDocumentReaderConfig.defaultConfig()
            );
            List<Document> rawDocuments = pdfReader.get();

            // 2. Attach source file metadata
            for (Document doc : rawDocuments) {
                doc.getMetadata().put("file_name", originalFilename);
                doc.getMetadata().put("file_size", file.getSize());
            }

            // 3. Split into tokenized chunks
            List<Document> chunks = tokenTextSplitter.apply(rawDocuments);
            log.info("Extracted {} pages and generated {} chunks for file: {}",
                    rawDocuments.size(), chunks.size(), originalFilename);

            // 4. Store embeddings and documents in Chroma VectorStore
            vectorStore.add(chunks);

            // 5. Save persistence audit record in database
            DocumentRecord entity = new DocumentRecord(
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    chunks.size(),
                    "INDEXED"
            );
            DocumentRecord saved = documentRecordRepository.save(entity);

            return documentRecordMapper.toDto(saved);

        } catch (Exception ex) {
            log.error("Failed to ingest PDF: {}", originalFilename, ex);
            DocumentRecord failedRecord = new DocumentRecord(
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    0,
                    "FAILED"
            );
            documentRecordRepository.save(failedRecord);
            throw new RuntimeException("Error ingesting PDF file into Chroma: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentRecordDto> getAllDocuments() {
        return documentRecordRepository.findAll()
                .stream()
                .map(documentRecordMapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatResponseDto askQuestion(ChatRequestDto request) {
        log.info("Asking question with RAG context: {}", request.question());

        int topK = (request.topK() != null && request.topK() > 0) ? request.topK() : 4;

        // Retrieve relevant source documents from Chroma
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.question())
                .topK(topK)
                .build();
        List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);

        // Extract unique source file names from retrieved document metadata
        List<String> sourceFiles = relevantDocs.stream()
                .map(doc -> (String) doc.getMetadata().get("file_name"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Query model through ChatClient with System Prompt and QuestionAnswerAdvisor
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .build();

        String answer = chatClient.prompt()
                .system(RAG_ASSISTANT_SYSTEM_PROMPT)
                .user(request.question())
                .advisors(qaAdvisor)
                .call()
                .content();

        return new ChatResponseDto(request.question(), answer, sourceFiles);
    }
}
