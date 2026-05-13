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

Normal chat does not have access to business tools:
```bash
curl "http://localhost:8080/chat?message=What+is+the+status+of+order+ORD-1001?"
```

Tool calling uses `/chat/tools`. The LLM decides when to call a Java method, LangChain4j executes it, and the tool result is sent back to the LLM for the final answer:
```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

Refund policy tool:
```bash
curl "http://localhost:8080/chat/tools?message=Can+I+refund+a+workshop+pass?"
```

Weather/API-style tool:
```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+weather+in+Bangalore+today?"
```

Tool error handling:
```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-9999?"
```

Tool schema design:
```bash
curl "http://localhost:8080/tools/schema"
```

Tool call audit log:
```bash
curl "http://localhost:8080/tools/audit"
curl -X DELETE "http://localhost:8080/tools/audit"
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
        ├── CommerceSupportTools.java  # @Tool examples for Demo 1
        ├── DocumentIngestor.java      # Document loading, splitting, embedding
        └── RagEvaluator.java          # Golden test set evaluation
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/chat?message=...` | Chat with LLM (Demo 1) |
| GET | `/chat/tools?message=...` | Chat with tool calling (Demo 1) |
| GET | `/tools/schema` | Inspect tool names, descriptions, and parameters |
| GET | `/tools/audit` | Inspect executed tool calls and errors |
| DELETE | `/tools/audit` | Clear the in-memory tool call audit log |
| POST | `/ingest` | Ingest docs into vector store (Demo 2) |
| GET | `/ask?question=...` | RAG query (Demo 2) |
| POST | `/evaluate` | Run eval test set (Demo 2) |

## Demo Script References

- [Demo 1: Local Inference with Ollama + LangChain4j](JavaOne_Demo1_Local_Inference_Ollama_LangChain4j.md)
- [Demo 2: RAG Pipeline with Quality Evaluation](JavaOne_Demo2_RAG_Pipeline_Evaluation.md)
- [Demo 3: Tool Calling / Function Calling](JavaOne_Demo3_Tool_Calling_Function_Calling.md)
