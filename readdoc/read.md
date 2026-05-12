# Spring AI Feature Testing Steps

Ye file Spring AI changes ko locally test karne ke liye step-by-step runbook hai.

## 1. Prerequisites Check

Java version check karo:

```powershell
java -version
```

Maven version check karo:

```powershell
mvn -version
```

Ollama running hai ya nahi check karo:

```powershell
ollama list
```

Required models pull karo:

```powershell
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

## 2. Build Test

Code compile ho raha hai ya nahi:

```powershell
mvn test
```

Expected result:

```text
BUILD SUCCESS
```

## 3. Application Start

App start karo:

```powershell
mvn spring-boot:run
```

Expected logs me app start hona chahiye:

```text
Started OpenModelsDemoApplication
```

Default port:

```text
http://localhost:8080
```

## 4. Spring AI Basics Endpoint Test

Ye endpoint verify karta hai ki Spring AI concepts app me available hain.

```powershell
curl "http://localhost:8080/spring-ai/basics"
```

Expected:

- `chat_client`
- `prompt_template`
- `advisors`
- `vector_store`
- `embedding_model`
- `chat_model`
- `tool_calling`
- `chat_memory`
- `structured_output`
- `streaming`

## 5. Simple Chat Test

```powershell
curl "http://localhost:8080/chat?message=What+is+Spring+AI?"
```

Expected:

- Model se normal text answer milega.
- Ye `ChatClient` use karta hai.

Memory test ke liye same `conversationId` use karo:

```powershell
curl "http://localhost:8080/chat?conversationId=user1&message=My+name+is+Amit"
curl "http://localhost:8080/chat?conversationId=user1&message=What+is+my+name?"
```

Expected:

- Second response me model previous message ka context use karega.

## 6. PromptTemplate Test

```powershell
curl "http://localhost:8080/chat/template?topic=Advisors&audience=Java+developers"
```

Expected:

- Model `Advisors` topic ko Java developers ke liye explain karega.
- Ye `PromptTemplate` + `ChatModel` flow test karta hai.

## 7. Tool Calling Test

```powershell
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+are+in+stock?"
```

Expected:

- Response me `JDK-21` stock count aana chahiye.
- Inventory data `InventoryTools.checkStock()` method se aata hai.

Try another SKU:

```powershell
curl "http://localhost:8080/chat/tools?message=Check+stock+for+GRAAL-22"
```

## 8. Structured Output Test

```powershell
curl "http://localhost:8080/chat/structured?message=Build+a+RAG+demo+for+Java+developers"
```

Expected JSON-like response:

```json
{
  "intent": "...",
  "keyTopics": ["...", "..."],
  "suggestedNextStep": "..."
}
```

Ye verify karta hai ki model response Java record `AnswerSummary` me map ho raha hai.

## 9. Streaming Test

PowerShell/curl me streaming response dekhne ke liye:

```powershell
curl -N "http://localhost:8080/chat/stream?message=Explain+ChatClient+streaming+in+Spring+AI"
```

Expected:

- Response chunks me gradually print hoga.
- Ye `Flux<String>` + `ChatClient.stream()` flow test karta hai.

## 10. RAG Ingestion Test

Docs folder ke `.txt` files vector store me ingest karo:

```powershell
curl -X POST "http://localhost:8080/ingest"
```

Expected response:

```json
{
  "status": "success",
  "documents_ingested": 3,
  "chunks_ingested": "...",
  "embedding_model": "nomic-embed-text",
  "store": "simple-in-memory"
}
```

Important:

- `/ask`, `/retrieve`, `/evaluate` se pehle `/ingest` run karna zaroori hai.
- `SimpleVectorStore` in-memory hai, app restart karoge to ingest dobara karna padega.

## 11. Vector Retrieval Test

```powershell
curl "http://localhost:8080/retrieve?question=What+are+virtual+threads?"
```

Expected:

- `matches` array me relevant document chunks milenge.
- Metadata me `file_name` ya `source` dikhega.

## 12. RAG Ask Test

```powershell
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

Expected:

- Answer docs ke context se grounded hoga.
- Flow: question -> vector search -> relevant chunks -> LLM answer.

Another question:

```powershell
curl "http://localhost:8080/ask?question=How+do+you+configure+Spring+Boot+profiles?"
```

## 13. RAG Evaluation Test

```powershell
curl -X POST "http://localhost:8080/evaluate"
```

Expected:

- `faithfulness`
- `answer_relevance`
- `context_precision`
- `source_accuracy`
- `average_latency_ms`

Note:

- Evaluation se pehle `/ingest` run hona chahiye.

## 14. Full Happy Path

Fresh app start ke baad ye sequence run karo:

```powershell
mvn test
mvn spring-boot:run
```

Dusre terminal me:

```powershell
curl "http://localhost:8080/spring-ai/basics"
curl "http://localhost:8080/chat?message=What+is+Spring+AI?"
curl "http://localhost:8080/chat/template?topic=VectorStore&audience=Java+developers"
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+are+in+stock?"
curl "http://localhost:8080/chat/structured?message=Explain+RAG+for+a+Java+team"
curl -N "http://localhost:8080/chat/stream?message=Explain+streaming+support"
curl -X POST "http://localhost:8080/ingest"
curl "http://localhost:8080/retrieve?question=What+are+virtual+threads?"
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
curl -X POST "http://localhost:8080/evaluate"
```

## 15. Common Issues

| Issue | Possible reason | Fix |
|---|---|---|
| `Connection refused` | App start nahi hai ya wrong port | `mvn spring-boot:run` run karo |
| Ollama connection error | Ollama service running nahi hai | Ollama start karo, `ollama list` check karo |
| Model not found | Model pull nahi hua | `ollama pull qwen2.5:0.5b` run karo |
| Embedding error | Embedding model missing | `ollama pull nomic-embed-text` run karo |
| `/ask` weak answer deta hai | Docs ingest nahi hue | Pehle `curl -X POST /ingest` run karo |
| `/evaluate` low scores | In-memory vector store empty ya retrieval weak | `/ingest` dobara run karo, docs verify karo |
| Streaming output ek saath dikhta hai | Client buffering kar raha hai | `curl -N` use karo |

## 16. Stop Application

Terminal me:

```powershell
Ctrl + C
```

Agar process background me reh gaya ho, port check karke process stop karo.
