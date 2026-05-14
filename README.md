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

## Demo 3: Agents + Manual ReAct Loop

Review the agent concepts covered by the demo:
```bash
curl "http://localhost:8080/agents/concepts"
```

Run a manual Reason -> Act -> Observe agent loop:
```bash
curl -X POST "http://localhost:8080/agents/run?goal=Check+stock+for+JDK-21+and+explain+what+the+agent+did"
```

The response includes the trace for each step: the model thought, selected tool, tool input, observation, final answer, and stopping condition. The loop is protected by `agent.max-steps` and repeated-action detection to avoid infinite loops.

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
| GET | `/agents/concepts` | Explain agent concepts (Demo 3) |
| POST | `/agents/run?goal=...` | Run manual ReAct agent loop with trace (Demo 3) |

## Demo Script References

- [Demo 1: Local Inference with Ollama + LangChain4j](JavaOne_Demo1_Local_Inference_Ollama_LangChain4j.md)
- [Demo 2: RAG Pipeline with Quality Evaluation](JavaOne_Demo2_RAG_Pipeline_Evaluation.md)
