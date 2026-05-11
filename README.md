# Production-Ready GenAI with Open Models for Java Teams

JavaOne 2026 Demo Project — Local inference with Ollama + LangChain4j, RAG pipeline with evaluation.

## Prerequisites

- **Java 21+** (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- **Ollama** installed and running ([ollama.com/download](https://ollama.com/download))

## Quick Start

### 1. Pull the required models

```bash
ollama pull qwen2.5:0.5b
ollama pull mistral:7b
ollama pull nomic-embed-text
```

### 2. Verify Ollama is running

```bash
ollama list
```

### 3. Build and run

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

## Demo 1: Local Inference + Tool Calling

Chat with the model:
```bash
curl "http://localhost:8080/chat?message=What+is+the+Java+record+keyword?"
```

Tool calling (inventory lookup — uses `/chat/tools` endpoint):
```bash
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+do+we+have+in+stock?"
```

To swap models, change `ollama.chat-model` in `src/main/resources/application.yml` to `mistral:7b` and restart.

## Demo 2: RAG Pipeline + Evaluation

### Ingest documents into the vector store:
```bash
curl -X POST "http://localhost:8080/ingest"
```

### Ask questions grounded in your documents:
```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

### Run quality evaluation:
```bash
curl -X POST "http://localhost:8080/evaluate"
```

## Demo 3: Embeddings

See the embedding concepts covered by the demo:
```bash
curl "http://localhost:8080/embeddings"
```

Convert text into a vector and inspect its dimensions:
```bash
curl "http://localhost:8080/embeddings/text?text=Java+virtual+threads+are+lightweight"
```

Compare a query embedding with a document embedding:
```bash
curl "http://localhost:8080/embeddings/compare?query=What+are+virtual+threads%3F&document=Virtual+threads+are+lightweight+JVM+threads+introduced+in+Java+21"
```

Run semantic similarity search across ad-hoc documents:
```bash
curl -X POST "http://localhost:8080/embeddings/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How does Spring Boot configuration work?",
    "documents": [
      "Spring Boot supports application.yml and profile-specific configuration files.",
      "Java virtual threads are lightweight threads managed by the JVM.",
      "Kubernetes can deploy Java services with containers and health checks."
    ],
    "maxResults": 3,
    "minScore": 0.0
  }'
```

## Project Structure

```
├── pom.xml
├── docs/                              # Sample documents for RAG
│   ├── java21-features.txt
│   ├── spring-boot-config.txt
│   └── kubernetes-java-deploy.txt
└── src/main/java/com/javaone/openmodels/
    ├── OpenModelsDemoApplication.java  # Spring Boot entry point
    ├── config/
    │   ├── OllamaConfig.java          # Chat model + chat assistant bean
    │   └── RagConfig.java             # Embedding model, store, RAG assistant
    ├── controller/
    │   ├── ChatController.java        # GET /chat, GET /chat/tools
    │   ├── EmbeddingController.java   # Embedding concepts, vectors, compare, search
    │   └── RagController.java         # POST /ingest, GET /ask, POST /evaluate
    └── service/
        ├── Assistant.java             # AI Service interface
        ├── DocumentIngestor.java      # Document loading, splitting, embedding
        ├── EmbeddingLearningService.java # Embedding demo logic
        ├── InventoryTools.java        # @Tool example for Demo 1
        └── RagEvaluator.java          # Golden test set evaluation
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/chat?message=...` | Chat with LLM (Demo 1) |
| GET | `/chat/tools?message=...` | Chat with tool calling (Demo 1) |
| POST | `/ingest` | Ingest docs into vector store (Demo 2) |
| GET | `/ask?question=...` | RAG query (Demo 2) |
| POST | `/evaluate` | Run eval test set (Demo 2) |
| GET | `/embeddings` | Explain embedding concepts (Demo 3) |
| GET | `/embeddings/text?text=...` | Convert text to vector and show dimensions |
| GET | `/embeddings/compare?query=...&document=...` | Compare query and document embeddings |
| POST | `/embeddings/search` | Run similarity search over provided documents |

## Demo Script References

- [Demo 1: Local Inference with Ollama + LangChain4j](JavaOne_Demo1_Local_Inference_Ollama_LangChain4j.md)
- [Demo 2: RAG Pipeline with Quality Evaluation](JavaOne_Demo2_RAG_Pipeline_Evaluation.md)
