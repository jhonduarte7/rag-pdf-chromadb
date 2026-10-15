package com.venefast.springboot.springiarag.app;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration and context loading test suite for {@link SpringiaRagApplication}.
 * <p>
 * Mocks the Chroma {@link VectorStore} and Ollama {@link ChatClient} beans so that the
 * application context can initialize cleanly during build verification without requiring live
 * ChromaDB or Ollama containers.
 */
@SpringBootTest
class SpringiaRagApplicationTests {

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private ChatClient chatClient;

    /**
     * Verifies that the Spring Boot application context loads successfully.
     */
    @Test
    void contextLoads() {
    }

}
