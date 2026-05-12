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

Start Elasticsearch for the vector database:
```bash
docker compose up -d elasticsearch
```

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

### Inspect vector search directly:
```bash
curl "http://localhost:8080/vector/index"
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?&topK=3&scoreThreshold=0.7"
curl "http://localhost:8080/vector/search?query=heap+size&metadataKey=file_name&metadataValue=java21-features.txt"
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
    │   └── RagController.java         # POST /ingest, GET /ask, POST /evaluate
    └── service/
        ├── Assistant.java             # AI Service interface
        ├── DocumentIngestor.java      # Document loading, splitting, embedding
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
| GET | `/vector/index` | Show Elasticsearch vector index and retrieval settings |
| GET | `/vector/search?query=...` | Similarity search with topK, score threshold, and metadata filtering |
| POST | `/evaluate` | Run eval test set (Demo 2) |

## Demo Script References

- [Demo 1: Local Inference with Ollama + LangChain4j](JavaOne_Demo1_Local_Inference_Ollama_LangChain4j.md)
- [Demo 2: RAG Pipeline with Quality Evaluation](JavaOne_Demo2_RAG_Pipeline_Evaluation.md)
- [Demo 3: Vector Database with Elasticsearch](JavaOne_Demo3_Vector_Database_Elasticsearch.md)
