# AGENTS.md - Spring Boot Workspace & RAG Project Governance

## Active Workspace Skills

| Skill Name | Path | Description |
| :--- | :--- | :--- |
| **`spring-boot-best-practices-workspaces`** | `.agents/skills/spring-boot-best-practices-workspaces/SKILL.md` | Workspace-level governance, language policy (Code in English, User preferred communication in English), Java 25 standard, and skill orchestration. |
| **`spring-boot-best-practices`** | `.agents/skills/spring-boot-best-practices/SKILL.md` | Layered architecture standards (Controllers, Services, Repositories, Entities, DTO records, Mappers), Maven wrapper tooling, and application configuration. |
| **`skill-creator`** | `.agents/skills/skill-creator/SKILL.md` | Skill lifecycle management, evaluation, benchmarking, and maintenance. |

---

## Subprojects Overview

### 1. [`springia-rag`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/pom.xml)
Spring Boot 4.1.1 & Spring AI 2.0.1 application implementing an end-to-end Retrieval-Augmented Generation (RAG) system for PDF documents.

- **Build Tool**: Maven Wrapper (`mvnw.cmd` on Windows, `./mvnw` on Unix)
- **Java Standard**: Java 25 (LTS)
- **Base Package**: `com.venefast.springboot.springiarag.app`
- **Application Port**: `8085` (`server.port=8085` to prevent conflicts with Windows `AgentService` on `8080`)
- **Configuration Format**: `src/main/resources/application.properties` (Properties only, no YAML)

---

## Vector Database: ChromaDB Infrastructure

ChromaDB serves as the centralized vector database for storing PDF chunk embeddings and performing semantic similarity searches.

### Container Specifications
- **Image**: `chromadb/chroma:latest`
- **Container Name**: `chroma-db` (running in background as a daemon)
- **Restart Policy**: `always` (`--restart always`)
- **Port Mapping**: `8000:8000`
- **Volume**: `chroma-data` mounted to container `/data`
- **Environment**: `IS_PERSISTENT=TRUE`, `PERSIST_DIRECTORY=/data`, `ANONYMIZED_TELEMETRY=FALSE`

### Where & How ChromaDB is Used in the Application
1. **Application Configuration ([`application.properties`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/resources/application.properties))**:
   - `spring.ai.vectorstore.chroma.client.host=http://localhost`
   - `spring.ai.vectorstore.chroma.client.port=8000`
   - `spring.ai.vectorstore.chroma.collection-name=springia_rag_collection`
   - `spring.ai.vectorstore.chroma.initialize-schema=true`
2. **Service Layer ([`RagServiceImpl.java`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/services/impl/RagServiceImpl.java))**:
   - Injected as `org.springframework.ai.vectorstore.VectorStore`.
   - **Ingestion**: Calls `vectorStore.add(chunks)` to embed and store tokenized PDF fragments.
   - **RAG Retrieval**: Binds with `QuestionAnswerAdvisor.builder(vectorStore).build()` on `ChatClient` to supply retrieved context chunks to `qwen3:1.7b`.
   - **Source Attribution**: Reads chunk metadata (`file_name`) to populate `sourceFiles` in `ChatResponseDto`.

### ChromaDB Lifecycle Commands

#### Option A: Direct Docker Run Command (Recommended)
```bash
docker run -d \
  --name chroma-db \
  --restart always \
  -p 8000:8000 \
  -v chroma-data:/data \
  -e IS_PERSISTENT=TRUE \
  -e PERSIST_DIRECTORY=/data \
  -e ANONYMIZED_TELEMETRY=FALSE \
  chromadb/chroma:latest
```

#### Option B: Docker Compose Command
From [`springia-rag/docker-compose.yml`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/docker-compose.yml):
```bash
docker compose -f springia-rag/docker-compose.yml up -d
```

#### Health Check & Status Verification
- **Container status**:
  ```bash
  docker ps --filter "name=chroma-db"
  ```
- **API Heartbeat check**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8000/api/v2/heartbeat"
  ```
- **View container logs**:
  ```bash
  docker logs -f chroma-db
  ```

---

## AI Models (Ollama)
- **Chat Model**: `qwen3:1.7b` (`spring.ai.ollama.chat.options.model`)
- **Embedding Model**: `nomic-embed-text` (`spring.ai.ollama.embedding.options.model`)
- **Base URL**: `http://localhost:11434`

Download models:
```bash
ollama pull qwen3:1.7b
ollama pull nomic-embed-text
```

---

## Application Architecture & Layered Structure
- **`controllers/`**: [`RagController`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/controllers/RagController.java) exposing REST endpoints for PDF ingestion and document querying.
- **`services/`**: [`RagService`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/services/RagService.java) & [`RagServiceImpl`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/services/impl/RagServiceImpl.java) encapsulating PDF processing, chunking, vector indexing in Chroma, business logic system prompt, and RAG retrieval.
- **`repositories/`**: [`DocumentRecordRepository`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/repositories/DocumentRecordRepository.java) extending Spring Data `JpaRepository`.
- **`models/entities/`**: [`DocumentRecord`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/models/entities/DocumentRecord.java) JPA entity tracking ingested files and chunk metrics.
- **`models/dtos/`**: Immutable Java `record`s ([`ChatRequestDto`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/models/dtos/ChatRequestDto.java), [`ChatResponseDto`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/models/dtos/ChatResponseDto.java), [`DocumentRecordDto`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/models/dtos/DocumentRecordDto.java)).
- **`mappers/`**: [`DocumentRecordMapper`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/mappers/DocumentRecordMapper.java) handling bidirectional conversion between entities and DTOs.
- **`config/`**: [`RagConfig`](file:///D:/IA/SPRING-BOOT/WORKSPACE/rag-pdf-chromadb/springia-rag/src/main/java/com/venefast/springboot/springiarag/app/config/RagConfig.java) providing `ChatClient` and `TokenTextSplitter` infrastructure beans.
