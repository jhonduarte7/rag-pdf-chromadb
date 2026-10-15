package com.venefast.springboot.springiarag.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for RAG infrastructure beans.
 *
 * <p>Exposes the infrastructure beans required for Spring AI operations:
 * <ul>
 *   <li>{@link ChatClient} for interacting with the Ollama language model (qwen3:1.7b).</li>
 *   <li>{@link TokenTextSplitter} for segmenting parsed PDF documents into uniform tokenized chunks.</li>
 * </ul>
 * </p>
 */
@Configuration
public class RagConfig {

    /**
     * Builds and exposes the primary ChatClient bean.
     *
     * @param chatClientBuilder auto-configured ChatClient.Builder supplied by Spring Boot
     * @return an immutable, ready-to-use ChatClient instance
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    /**
     * Configures the TokenTextSplitter bean used to fragment documents into semantic token windows.
     *
     * <p>Configured with a target chunk size of 800 tokens and minimum characters per chunk to prevent
     * truncation at inappropriate boundaries.</p>
     *
     * @return configured TokenTextSplitter instance
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }
}
