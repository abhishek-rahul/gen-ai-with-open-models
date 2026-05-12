# Production-Ready GenAI with Open Models for Java Teams

JavaOne 2026 demo project for local inference with Ollama, Spring AI basics, RAG, tool calling, chat memory, structured output, and streaming.

## Prerequisites

- Java 21+
- Maven 3.9+
- Ollama installed and running

## Quick Start

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
mvn clean package -DskipTests
mvn spring-boot:run
```

## Spring AI Basics

This branch demonstrates:

- Spring AI as the abstraction layer for model, prompt, tool, memory, and vector workflows
- `ChatClient`
- `PromptTemplate`
- Advisors: `MessageChatMemoryAdvisor`, `SimpleLoggerAdvisor`, `QuestionAnswerAdvisor`
- `VectorStore` with `SimpleVectorStore`
- `EmbeddingModel`
- `ChatModel`
- Tool calling with `@Tool`
- Chat memory through `ChatMemory.CONVERSATION_ID`
- Structured output with `ChatClient.call().entity(...)`
- Streaming with `ChatClient.stream().content()`

## Demo Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/spring-ai/basics` | Lists the Spring AI basics implemented in code |
| GET | `/chat?message=...&conversationId=...` | ChatClient chat with memory |
| GET | `/chat/template?topic=...&audience=...` | PromptTemplate + ChatModel call |
| GET | `/chat/tools?message=...` | Tool calling with inventory lookup |
| GET | `/chat/structured?message=...` | Structured Java record output |
| GET | `/chat/stream?message=...` | Server-sent streaming response |
| POST | `/ingest` | Read docs, split, embed, and store in VectorStore |
| GET | `/retrieve?question=...` | Raw VectorStore similarity matches |
| GET | `/ask?question=...` | RAG answer with QuestionAnswerAdvisor |
| POST | `/evaluate` | Simple RAG quality evaluation |

## Example Calls

```bash
curl "http://localhost:8080/spring-ai/basics"
curl "http://localhost:8080/chat?message=What+is+Spring+AI?"
curl "http://localhost:8080/chat/template?topic=Advisors&audience=Java+developers"
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+are+in+stock?"
curl "http://localhost:8080/chat/structured?message=Build+a+RAG+demo+for+Java+developers"
curl -N "http://localhost:8080/chat/stream?message=Explain+ChatClient+streaming"
curl -X POST "http://localhost:8080/ingest"
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

## Project Structure

```text
src/main/java/com/javaone/openmodels/
  config/OllamaConfig.java              Spring AI ChatClient, memory, VectorStore beans
  controller/ChatController.java        Spring AI basics, chat, tools, structured, streaming
  controller/RagController.java         Ingestion, retrieval, RAG, evaluation endpoints
  service/DocumentIngestor.java         TextReader, TokenTextSplitter, VectorStore ingest
  service/InventoryTools.java           Spring AI @Tool example
  service/SpringAiBasicsService.java    ChatClient, PromptTemplate, advisors, tools, memory
  service/RagEvaluator.java             Simple golden-set RAG evaluator
```
