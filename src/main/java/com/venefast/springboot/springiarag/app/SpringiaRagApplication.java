package com.venefast.springboot.springiarag.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the springia-rag Spring Boot application.
 *
 * <p>Bootstraps the Spring context, initializes Spring AI auto-configurations
 * for Ollama models and Chroma VectorStore, and starts the embedded Tomcat server on port 8085.</p>
 */
@SpringBootApplication
public class SpringiaRagApplication {

	/**
	 * Launches the Spring Boot application.
	 *
	 * @param args command-line arguments passed during startup
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringiaRagApplication.class, args);
	}

}
