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

## Demo 3: LangChain Concepts

Why LangChain, productivity, and abstraction risk:
```bash
curl "http://localhost:8080/langchain/why"
```

Raw model call vs LangChain4j AI Service:
```bash
curl "http://localhost:8080/langchain/raw-vs-framework?message=Explain+LangChain+in+Java"
```

Prompt template plus chain-style prompt -> model -> parser flow:
```bash
curl "http://localhost:8080/langchain/prompt-chain?question=Why+use+prompt+templates?"
```

Tool integration:
```bash
curl "http://localhost:8080/langchain/tools?sku=JDK-21"
```

Memory integration:
```bash
curl -X POST "http://localhost:8080/langchain/memory?conversationId=demo&message=My+name+is+Amit"
curl -X POST "http://localhost:8080/langchain/memory?conversationId=demo&message=What+is+my+name?"
```

RAG chain internals:
```bash
curl "http://localhost:8080/langchain/rag-chain?question=What+are+virtual+threads?"
curl "http://localhost:8080/langchain/internals"
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
| POST | `/evaluate` | Run eval test set (Demo 2) |
| GET | `/langchain/why` | Why LangChain, risks, productivity |
| GET | `/langchain/raw-vs-framework?message=...` | Raw ChatModel call vs AI Service |
| GET | `/langchain/prompt-chain?question=...` | Prompt template, chain, output parser |
| GET | `/langchain/tools?sku=...` | Tool integration demo |
| POST | `/langchain/memory?conversationId=...&message=...` | Memory/chat history demo |
| GET | `/langchain/rag-chain?question=...` | Explicit RAG chain demo |
| GET | `/langchain/internals` | LangChain internals map |

## Demo Script References

- [Demo 1: Local Inference with Ollama + LangChain4j](JavaOne_Demo1_Local_Inference_Ollama_LangChain4j.md)
- [Demo 2: RAG Pipeline with Quality Evaluation](JavaOne_Demo2_RAG_Pipeline_Evaluation.md)
