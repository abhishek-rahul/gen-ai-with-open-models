# Spring AI Code Flow Explanation

Ye document recent Spring AI changes ko simple Hinglish me explain karta hai. Goal ye hai ki code padhte waqt clear rahe ki request controller se service tak kaise jaati hai, model kaise call hota hai, memory/tools/RAG/streaming kaise kaam karte hain.

## Big Picture

Is branch me project ko LangChain4j style se Spring AI style me shift kiya gaya hai.

Application ke main parts:

| Layer | File | Simple role |
|---|---|---|
| Config | `OllamaConfig.java` | Spring AI ke common beans banata hai: `ChatClient`, `ChatMemory`, `VectorStore` |
| Controller | `ChatController.java` | Chat related HTTP endpoints expose karta hai |
| Controller | `RagController.java` | Document ingest, retrieval, RAG ask, evaluation endpoints expose karta hai |
| Service | `SpringAiBasicsService.java` | Main AI logic yahin hai: chat, template, tools, structured output, streaming, RAG |
| Service | `DocumentIngestor.java` | Docs folder se text files read karke chunks banata hai aur vector store me save karta hai |
| Service | `InventoryTools.java` | Tool calling demo ke liye inventory lookup method |
| Service | `RagEvaluator.java` | RAG answer quality ko simple metrics se check karta hai |
| Config | `application.yml` | Ollama model, embedding model, RAG settings define karta hai |

## Spring AI Concepts Used

| Spring AI concept | Code me kaha use hua | Iska use kya hai |
|---|---|---|
| Spring AI | `pom.xml`, `application.yml`, services | Spring Boot app ko AI models, embeddings, tools, vector DB, memory, streaming ke saath standard way me connect karta hai |
| `ChatClient` | `OllamaConfig.java`, `SpringAiBasicsService.java` | LLM se baat karne ka fluent API. Jaise `prompt().user(...).call().content()` |
| `ChatModel` | `SpringAiBasicsService.template()` | Lower-level model object. Yahan direct `PromptTemplate` se bana hua `Prompt` model ko diya gaya hai |
| `PromptTemplate` | `SpringAiBasicsService.template()` | Prompt me placeholders fill karta hai, jaise `{topic}` aur `{audience}` |
| Advisors | `OllamaConfig.java`, `SpringAiBasicsService.askWithRag()` | Request ke beech extra behavior add karte hain. Memory, logging, RAG context yahi se attach hota hai |
| `MessageChatMemoryAdvisor` | `OllamaConfig.java` | Conversation history ko prompt ke saath attach karta hai, taaki model previous chat yaad rakh sake |
| `SimpleLoggerAdvisor` | `OllamaConfig.java` | Chat request/response debugging ke liye logging help karta hai |
| `QuestionAnswerAdvisor` | `SpringAiBasicsService.askWithRag()` | User ke question ke basis par vector store se relevant docs nikal kar model ko context deta hai |
| `ChatMemory` | `OllamaConfig.java`, `SpringAiBasicsService.chat()` | Conversation history store karta hai |
| `MessageWindowChatMemory` | `OllamaConfig.java` | Last 20 messages tak memory rakhta hai |
| `VectorStore` | `OllamaConfig.java`, `DocumentIngestor.java`, `SpringAiBasicsService.java` | Documents ke embeddings store karta hai aur similar documents search karta hai |
| `SimpleVectorStore` | `OllamaConfig.java` | In-memory vector store. Demo ke liye simple option |
| `EmbeddingModel` | `OllamaConfig.java`, `SpringAiBasicsService.basics()` | Text ko vector embeddings me convert karta hai. Is app me Ollama ka `nomic-embed-text` use hota hai |
| `SearchRequest` | `SpringAiBasicsService.retrieve()`, `askWithRag()`, `RagEvaluator.java` | Vector search ke options define karta hai: query, topK, similarity threshold |
| `TextReader` | `DocumentIngestor.java` | `.txt` file ko Spring AI `Document` object me read karta hai |
| `TokenTextSplitter` | `DocumentIngestor.java` | Bade documents ko chhote chunks me split karta hai |
| Tool calling | `InventoryTools.java`, `SpringAiBasicsService.chatWithTools()` | Model ko Java method call karne ki ability deta hai |
| `@Tool` | `InventoryTools.checkStock()` | Method ko LLM-callable tool banata hai |
| `@ToolParam` | `InventoryTools.checkStock()` | Tool ke input parameter ka meaning model ko batata hai |
| Structured output | `SpringAiBasicsService.structured()` | Model response ko Java record `AnswerSummary` me map karta hai |
| Streaming | `ChatController.stream()`, `SpringAiBasicsService.stream()` | Response ko ek saath wait karne ke bajay token/chunks me stream karta hai |
| `Flux<String>` | `ChatController.stream()` | Streaming response ke liye reactive stream type |

## Configuration Flow

### `application.yml`

Yahan model settings hain:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:0.5b
          temperature: 0.7
      embedding:
        options:
          model: nomic-embed-text
```

Simple meaning:

- Chat model: `qwen2.5:0.5b`
- Embedding model: `nomic-embed-text`
- Ollama URL: `http://localhost:11434`
- RAG docs folder: `docs`
- Vector search max results: `5`
- Minimum similarity score: `0.7`

## Bean Setup Flow

### `OllamaConfig.java`

Ye class Spring container me reusable objects register karti hai.

1. `ChatMemory` bean banta hai:
   - `MessageWindowChatMemory`
   - Last 20 messages remember karta hai

2. `ChatClient` bean banta hai:
   - Default system message set hota hai: Java team ke liye helpful assistant
   - Default advisors attach hote hain:
     - `MessageChatMemoryAdvisor`
     - `SimpleLoggerAdvisor`

3. `VectorStore` bean banta hai:
   - `SimpleVectorStore`
   - Embedding model use karke in-memory vector DB banata hai

## Chat Flow

Endpoint:

```text
GET /chat?message=What+is+Spring+AI?&conversationId=user1
```

Flow:

1. Request `ChatController.chat()` me aati hai.
2. Controller `SpringAiBasicsService.chat()` call karta hai.
3. Service `ChatClient` se prompt banata hai.
4. `conversationId` advisor param me pass hota hai.
5. Memory advisor same conversation ki old messages add karta hai.
6. User message model ko bheja jata hai.
7. Model response string me return hota hai.

Important code:

```java
chatClient.prompt()
    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
    .user(message)
    .call()
    .content();
```

## PromptTemplate Flow

Endpoint:

```text
GET /chat/template?topic=Advisors&audience=Java+developers
```

Flow:

1. Controller `promptTemplate()` call karta hai.
2. Service ek template banata hai:
   - `{topic}`
   - `{audience}`
3. Runtime values fill hoti hain.
4. Final `Prompt` direct `ChatModel` ko diya jata hai.
5. Response return hota hai.

Ye dikhata hai ki `ChatClient` ke alawa lower-level `ChatModel` bhi direct use kar sakte hain.

## Tool Calling Flow

Endpoint:

```text
GET /chat/tools?message=How+many+units+of+JDK-21+are+in+stock?
```

Flow:

1. Request `ChatController.chatWithTools()` me aati hai.
2. Service `chatWithTools()` run karta hai.
3. `InventoryTools` object `ChatClient` ko pass hota hai.
4. Model ko pata chalta hai ki ek tool available hai: `checkStock`.
5. Agar model ko inventory data chahiye, wo tool call karta hai.
6. Java method stock return karta hai.
7. Model final natural-language answer banata hai.

Tool method:

```java
@Tool(description = "Look up the current stock level for a product by SKU")
public String checkStock(@ToolParam(description = "Product SKU") String sku)
```

## Structured Output Flow

Endpoint:

```text
GET /chat/structured?message=Build+a+RAG+demo+for+Java+developers
```

Flow:

1. User ek normal text message bhejta hai.
2. Service model ko bolta hai ki request summarize karo.
3. `.entity(AnswerSummary.class)` use hota hai.
4. Spring AI model output ko Java record me convert karta hai.

Record:

```java
public record AnswerSummary(
    String intent,
    List<String> keyTopics,
    String suggestedNextStep
) {}
```

Simple output shape:

```json
{
  "intent": "...",
  "keyTopics": ["...", "..."],
  "suggestedNextStep": "..."
}
```

## Streaming Flow

Endpoint:

```text
GET /chat/stream?message=Explain+ChatClient+streaming
```

Flow:

1. Controller endpoint `TEXT_EVENT_STREAM` produce karta hai.
2. Service `chatClient.prompt().stream().content()` call karta hai.
3. Full answer wait karne ke bajay response chunks me client ko milta hai.
4. Return type `Flux<String>` hai.

Use case:

- Long answers
- Chat UI
- Better user experience because user ko response gradually dikhta hai

## RAG Ingestion Flow

Endpoint:

```text
POST /ingest
```

Flow:

1. `RagController.ingest()` call hota hai.
2. `DocumentIngestor.ingest(Path.of(docsDir))` run hota hai.
3. `docs` folder ki `.txt` files read hoti hain.
4. `TextReader` each file ko `Document` banata hai.
5. Metadata add hota hai:
   - `file_name`
   - `source`
6. `TokenTextSplitter` large documents ko chunks me split karta hai.
7. `VectorStore.add(chunks)` call hota hai.
8. Spring AI embedding model har chunk ka embedding banata hai.
9. Chunks `SimpleVectorStore` me save ho jate hain.

Simple mental model:

```text
txt files -> Document -> chunks -> embeddings -> VectorStore
```

## Retrieval Flow

Endpoint:

```text
GET /retrieve?question=What+are+virtual+threads?
```

Flow:

1. User question aata hai.
2. `SearchRequest` banta hai:
   - query = user question
   - topK = configured max results
   - similarityThreshold = configured min score
3. `vectorStore.similaritySearch(searchRequest)` run hota hai.
4. Matching document chunks return hote hain.

Ye endpoint raw retrieval dekhne ke liye useful hai.

## RAG Ask Flow

Endpoint:

```text
GET /ask?question=What+are+virtual+threads+in+Java+21?
```

Flow:

1. Request `RagController.ask()` me aati hai.
2. Controller `SpringAiBasicsService.askWithRag()` call karta hai.
3. Service `SearchRequest` banata hai.
4. `QuestionAnswerAdvisor` vector store se relevant context retrieve karta hai.
5. Advisor retrieved docs ko prompt context me add karta hai.
6. Model user question ka answer docs ke context ke basis par banata hai.

Simple mental model:

```text
question -> vector search -> relevant chunks -> prompt context -> LLM answer
```

## Evaluation Flow

Endpoint:

```text
POST /evaluate
```

Flow:

1. `RagEvaluator` ke andar small test set hai.
2. Har test case ke liye:
   - RAG answer generate hota hai
   - Same question ke liye vector documents retrieve hote hain
   - Simple score calculate hota hai
3. Metrics:
   - `faithfulness`: answer expected keywords se kitna match karta hai
   - `answer_relevance`: answer question ke words se kitna relevant hai
   - `context_precision`: retrieved docs question se kitne related hain
   - `source_accuracy`: expected file retrieve hui ya nahi
   - `average_latency_ms`: average response time

Ye production-grade evaluator nahi hai, but demo ke liye easy and understandable quality check hai.

## Endpoint Summary

| Endpoint | Main concept | Code path |
|---|---|---|
| `/spring-ai/basics` | Concepts overview | `ChatController` -> `SpringAiBasicsService.basics()` |
| `/chat` | ChatClient + memory | `ChatController` -> `SpringAiBasicsService.chat()` |
| `/chat/template` | PromptTemplate + ChatModel | `ChatController` -> `SpringAiBasicsService.template()` |
| `/chat/tools` | Tool calling | `ChatController` -> `SpringAiBasicsService.chatWithTools()` -> `InventoryTools` |
| `/chat/structured` | Structured output | `ChatController` -> `SpringAiBasicsService.structured()` |
| `/chat/stream` | Streaming | `ChatController` -> `SpringAiBasicsService.stream()` |
| `/ingest` | Document ingestion + embeddings | `RagController` -> `DocumentIngestor.ingest()` |
| `/retrieve` | Vector similarity search | `RagController` -> `SpringAiBasicsService.retrieve()` |
| `/ask` | RAG with advisor | `RagController` -> `SpringAiBasicsService.askWithRag()` |
| `/evaluate` | RAG quality check | `RagController` -> `RagEvaluator.evaluate()` |

## One-Line Summary

Is code me Spring AI ko use karke ek complete local AI demo bana hai: user chat kar sakta hai, prompt templates use ho sakte hain, Java methods tools ban sakte hain, chat memory maintain hoti hai, structured JSON-like Java output milta hai, response stream ho sakta hai, aur docs ko vector store me daal kar RAG answer generate ho sakta hai.
