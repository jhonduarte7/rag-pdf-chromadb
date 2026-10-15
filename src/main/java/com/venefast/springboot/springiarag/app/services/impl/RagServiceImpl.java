package com.venefast.springboot.springiarag.app.services.impl;

import com.venefast.springboot.springiarag.app.mappers.DocumentRecordMapper;
import com.venefast.springboot.springiarag.app.models.dtos.ChatRequestDto;
import com.venefast.springboot.springiarag.app.models.dtos.ChatResponseDto;
import com.venefast.springboot.springiarag.app.models.dtos.DocumentRecordDto;
import com.venefast.springboot.springiarag.app.models.dtos.SourceMetadataDto;
import com.venefast.springboot.springiarag.app.models.entities.DocumentRecord;
import com.venefast.springboot.springiarag.app.repositories.DocumentRecordRepository;
import com.venefast.springboot.springiarag.app.services.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Production implementation of {@link RagService} orchestrating Spring AI, ChromaDB, and relational persistence.
 *
 * <p>Pipeline overview:
 * <ol>
 *   <li>PDF parsing via Apache PDFBox ({@link PagePdfDocumentReader}).</li>
 *   <li>Token-aware text splitting via {@link TokenTextSplitter} preserving page and line metadata.</li>
 *   <li>Vectorization and storage in ChromaDB via {@link VectorStore}.</li>
 *   <li>Audit persistence in H2/JPA via {@link DocumentRecordRepository}.</li>
 *   <li>Single-pass RAG inference with ChromaDB context and Ollama model synthesis.</li>
 * </ol>
 * </p>
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    /**
     * Domain system prompt defining assistant persona, rich markdown output, and structured citations.
     */
    private static final String RAG_ASSISTANT_SYSTEM_PROMPT =
            """
            You are an expert, precise, and analytical AI assistant answering user questions based on PDF documents indexed with RAG and ChromaDB.

            Mandatory formatting and behavioral guidelines:
            1. Provide clear, concise, and direct answers based strictly on the provided context documents.
            2. Do NOT output verbose chain-of-thought or internal deliberation tags. Output the final markdown response directly.
            3. Always format your response using rich GitHub-flavored Markdown (hierarchical headings `#`, `##`, `###`, bold text for key concepts, bulleted or numbered lists, comparative/data tables when applicable, and syntax-highlighted code blocks).
            4. If the context does not contain enough information to answer the question, state so clearly, politely, and transparently.
            5. At the end of your response, include a reference section titled '### 📌 Cited Sources' summarizing the file name, page, and line numbers consulted.
            """;

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

            // 2. Split pages into tokenized chunks while computing page and line range metadata
            List<Document> chunks = new ArrayList<>();
            for (int i = 0; i < rawDocuments.size(); i++) {
                Document pageDoc = rawDocuments.get(i);
                Object pageNumObj = pageDoc.getMetadata().get("page_number");
                int pageNumber = (pageNumObj instanceof Number n) ? n.intValue() : (i + 1);

                String pageText = pageDoc.getText();

                // Split this single page into chunks
                List<Document> pageChunks = tokenTextSplitter.apply(List.of(pageDoc));
                for (Document chunk : pageChunks) {
                    String chunkText = chunk.getText();
                    int startLine = 1;
                    int endLine = 1;

                    if (pageText != null && chunkText != null && !chunkText.isBlank()) {
                        String anchor = chunkText.trim();
                        if (anchor.length() > 40) {
                            anchor = anchor.substring(0, 40);
                        }
                        int charIdx = pageText.indexOf(anchor);
                        if (charIdx >= 0) {
                            startLine = 1 + countNewlines(pageText.substring(0, charIdx));
                        }
                        int linesInChunk = countNewlines(chunkText);
                        endLine = Math.max(startLine, startLine + linesInChunk);
                    }

                    chunk.getMetadata().put("file_name", originalFilename);
                    chunk.getMetadata().put("file_size", file.getSize());
                    chunk.getMetadata().put("page_number", pageNumber);
                    chunk.getMetadata().put("start_line", startLine);
                    chunk.getMetadata().put("end_line", endLine);
                    chunks.add(chunk);
                }
            }

            log.info("Extracted {} pages and generated {} chunks for file: {}",
                    rawDocuments.size(), chunks.size(), originalFilename);

            // 3. Store embeddings and documents in Chroma VectorStore
            vectorStore.add(chunks);

            // 4. Save persistence audit record in database
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

        // Extract structured source metadata
        List<SourceMetadataDto> sources = relevantDocs.stream()
                .map(doc -> {
                    Map<String, Object> meta = doc.getMetadata();
                    String fileName = meta.get("file_name") != null ? meta.get("file_name").toString() : "document.pdf";
                    Integer page = meta.get("page_number") instanceof Number n ? n.intValue() : 1;
                    Integer startLine = meta.get("start_line") instanceof Number n ? n.intValue() : 1;
                    Integer endLine = meta.get("end_line") instanceof Number n ? n.intValue() : startLine;
                    String snippet = doc.getText();
                    return new SourceMetadataDto(fileName, page, startLine, endLine, snippet, meta);
                })
                .toList();

        // Extract unique source file names from retrieved document metadata
        List<String> sourceFiles = sources.stream()
                .map(SourceMetadataDto::fileName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Format context from retrieved documents directly without duplicating search query
        String context = relevantDocs.stream()
                .map(doc -> {
                    String fn = (String) doc.getMetadata().getOrDefault("file_name", "document.pdf");
                    Object page = doc.getMetadata().getOrDefault("page_number", 1);
                    return String.format("[Source: %s (Page %s)]:\n%s", fn, page, doc.getText());
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        if (context.isBlank()) {
            context = "No relevant context found in indexed documents.";
        }

        String userPromptWithContext = String.format("""
                Context from indexed documents:
                ---------------------
                %s
                ---------------------

                User Question: %s
                """, context, request.question());

        // Call model directly with single-pass context
        String answer = chatClient.prompt()
                .system(RAG_ASSISTANT_SYSTEM_PROMPT)
                .user(userPromptWithContext)
                .call()
                .content();

        return new ChatResponseDto(request.question(), answer, sourceFiles, sources);
    }

    /**
     * Helper method to count newline characters in a string.
     *
     * @param text input string
     * @return total count of '\n' characters
     */
    private int countNewlines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
