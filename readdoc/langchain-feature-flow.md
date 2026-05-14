# LangChain Feature Code Flow

Ye document simple terms me explain karta hai ki `develop_langchain_feature1` branch me jo LangChain related changes kiye gaye hain, unka code flow kaise kaam karta hai.

## Big Picture

Is project me Spring Boot API hai jo LangChain4j use karke local Ollama model ko call karti hai.

High level flow:

```text
User / curl request
    -> Controller
    -> Service
    -> LangChain4j abstraction
    -> Ollama model / tool / memory / retriever
    -> Response back to API
```

LangChain4j ka kaam hai common GenAI tasks ko easy banana:

- model call karna
- prompt template banana
- chain-style flow banana
- tool call karwana
- memory maintain karna
- RAG ke liye documents retrieve karna
- output ko Java types me parse karna

## Main New Files

### 1. `LangChainController.java`

Path:

```text
src/main/java/com/javaone/openmodels/controller/LangChainController.java
```

Ye REST endpoints expose karta hai. Is file me business logic nahi rakha gaya. Controller bas request leta hai aur `LangChainConceptService` ko call karta hai.

Example:

```java
@GetMapping("/langchain/why")
public Map<String, Object> whyLangChain() {
    return conceptService.whyLangChain();
}
```

Simple meaning:

```text
Browser/curl -> /langchain/why -> Controller -> Service -> JSON response
```

### 2. `LangChainConceptService.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/LangChainConceptService.java
```

Ye main demo service hai. Isme LangChain ke concepts practical code ke through dikhaye gaye hain:

- why LangChain
- raw call vs LangChain4j
- prompt template
- chain
- output parser
- tool integration
- memory integration
- RAG chain
- internals

Service ke andar ye dependencies inject hoti hain:

```java
private final ChatLanguageModel chatModel;
private final Assistant chatAssistant;
private final Assistant toolAssistant;
private final MemoryAssistant memoryAssistant;
private final ContentRetriever contentRetriever;
```

Inka simple meaning:

- `chatModel`: direct model adapter. Ye Ollama model ko call karta hai.
- `chatAssistant`: normal LangChain4j AI service.
- `toolAssistant`: AI service jiske paas Java tools available hain.
- `memoryAssistant`: AI service jo previous conversation yaad rakhta hai.
- `contentRetriever`: RAG ke liye document chunks retrieve karta hai.

### 3. `MemoryAssistant.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/MemoryAssistant.java
```

Ye memory demo ke liye separate interface hai.

Important point:

LangChain4j me agar method me `@MemoryId` use karte hain, to us AI service ko `ChatMemoryProvider` ke saath build karna padta hai. Isliye memory method ko normal `Assistant` me nahi rakha gaya; alag `MemoryAssistant` banaya gaya.

```java
String chatWithMemory(@MemoryId String conversationId, @UserMessage String message);
```

Simple meaning:

```text
conversationId same hoga -> model previous messages yaad rakhega
conversationId different hoga -> new conversation maanega
```

## Existing Files Me Changes

### 1. `OllamaConfig.java`

Path:

```text
src/main/java/com/javaone/openmodels/config/OllamaConfig.java
```

Ye file LangChain4j beans banati hai.

#### `chatModel()`

```java
OllamaChatModel.builder()
    .baseUrl(baseUrl)
    .modelName(chatModelName)
    .temperature(0.7)
    .timeout(Duration.ofSeconds(120))
    .build();
```

Simple meaning:

Ye batata hai ki model kaha chal raha hai aur kaunsa model use karna hai.

Config values `application.yml` se aati hain:

```yaml
ollama:
  base-url: http://localhost:11434
  chat-model: qwen2.5:0.5b
```

#### `chatAssistant`

Normal AI assistant:

```text
User message -> LangChain4j AiServices -> Ollama ChatModel -> answer
```

#### `toolAssistant`

Tool-enabled assistant:

```text
User asks inventory question
    -> model sees tool available
    -> calls InventoryTools.checkStock()
    -> final answer
```

#### `memoryAssistant`

Memory-enabled assistant:

```text
User message + conversationId
    -> MessageWindowChatMemory
    -> previous messages included
    -> model answer
```

### 2. `Assistant.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/Assistant.java
```

Ye LangChain4j AI service interface hai.

Important methods:

#### Normal chat

```java
String chat(@UserMessage String message);
```

Simple meaning:

User ka message model ko bhejna aur answer lena.

#### Prompt template

```java
@SystemMessage("You are a {role}. Answer about {topic}.")
String answerAsRole(@V("role") String role, @V("topic") String topic, @UserMessage String question);
```

Simple meaning:

Prompt me dynamic values inject hoti hain.

Example:

```text
role = senior Java architect
topic = LangChain
question = Why use prompt templates?
```

Final prompt model ko role/topic ke context ke saath jata hai.

#### Output parser

```java
List<String> threeLearningPoints(@V("topic") String topic);
```

Simple meaning:

Model ka text output LangChain4j Java `List<String>` me convert karta hai.

### 3. `InventoryTools.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/InventoryTools.java
```

Isme Java method ko LLM tool banaya gaya hai:

```java
@Tool("Look up current stock level for a product by SKU")
public String checkStock(@P("Product SKU") String sku)
```

Simple meaning:

Model jab samjhega ki stock check karna hai, to ye Java method call kar sakta hai.

Example:

```text
SKU JDK-21 ka stock kitna hai?
    -> checkStock("JDK-21")
    -> "SKU JDK-21: 150 units in stock"
```

### 4. `RagConfig.java`

Path:

```text
src/main/java/com/javaone/openmodels/config/RagConfig.java
```

Ye RAG ke core components banata hai:

- `EmbeddingModel`
- `EmbeddingStore`
- `ContentRetriever`
- `ragAssistant`

Simple RAG flow:

```text
documents
    -> split into chunks
    -> convert chunks to embeddings
    -> store vectors
    -> retrieve matching chunks
    -> send context to LLM
    -> answer
```

### 5. `DocumentIngestor.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/DocumentIngestor.java
```

Ye documents ko vector store me load karta hai.

Flow:

```text
docs folder
    -> FileSystemDocumentLoader
    -> DocumentSplitters.recursive()
    -> EmbeddingModel
    -> EmbeddingStore
```

Endpoint:

```bash
curl -X POST "http://localhost:8080/ingest"
```

### 6. `RagEvaluator.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/RagEvaluator.java
```

Ye RAG quality check karta hai.

Isme test questions hain:

- What are virtual threads?
- What is the minimum heap for microservices?
- How do you configure Spring Boot profiles?

Evaluation me ye check hota hai:

- answer expected terms se match kar raha hai ya nahi
- answer question se relevant hai ya nahi
- retrieved chunks useful hain ya nahi
- correct source file retrieve hui ya nahi

## Endpoint Wise Flow

### 1. `/langchain/why`

Purpose:

LangChain kyun use hota hai, productivity kya hai, abstraction risk kya hai.

Flow:

```text
GET /langchain/why
    -> LangChainController.whyLangChain()
    -> LangChainConceptService.whyLangChain()
    -> static explanation as JSON
```

### 2. `/langchain/raw-vs-framework`

Purpose:

Raw model call aur LangChain4j AI Service call ka difference dikhana.

Flow:

```text
GET /langchain/raw-vs-framework?message=...
    -> raw: chatModel.chat(message)
    -> framework: chatAssistant.chat(message)
    -> dono answers JSON me return
```

Simple difference:

- Raw call me model ko direct prompt bhejte hain.
- LangChain4j call me typed Java interface use hota hai.

### 3. `/langchain/prompt-chain`

Purpose:

Prompt template + chain + parser demo.

Flow:

```text
GET /langchain/prompt-chain?question=...
    -> PromptTemplate variables fill karta hai
    -> rendered prompt model ko jata hai
    -> answer return hota hai
    -> List<String> output parser demo bhi run hota hai
```

Chain idea:

```text
prompt -> model -> parser
```

Code me ye Java style me hai:

```text
PromptTemplate -> ChatLanguageModel -> String/List response
```

### 4. `/langchain/tools`

Purpose:

LLM ko Java function call karwana.

Flow:

```text
GET /langchain/tools?sku=JDK-21
    -> toolAssistant.chat("Check inventory for SKU JDK-21")
    -> model InventoryTools.checkStock() use kar sakta hai
    -> answer return
```

### 5. `/langchain/memory`

Purpose:

Conversation history maintain karna.

Flow:

```text
POST /langchain/memory?conversationId=demo&message=My name is Amit
    -> memoryAssistant.chatWithMemory("demo", message)
    -> message memory me save

POST /langchain/memory?conversationId=demo&message=What is my name?
    -> same conversationId
    -> previous message available
    -> model answer can use "Amit"
```

### 6. `/langchain/rag-chain`

Purpose:

Explicit RAG chain dikhana.

Flow:

```text
GET /langchain/rag-chain?question=...
    -> contentRetriever.retrieve(question)
    -> matching document chunks milte hain
    -> chunks + question model ko bheje jate hain
    -> grounded answer return hota hai
```

Before this, documents ingest karna useful hai:

```bash
curl -X POST "http://localhost:8080/ingest"
```

### 7. `/langchain/internals`

Purpose:

LangChain ke internal building blocks ko simple map me explain karna.

Includes:

- LLM / ChatModel
- PromptTemplate
- Chains / LCEL
- Output Parsers
- Tools
- Agents
- Memory / Chat History
- RAG
- Document Loaders
- Vector Stores
- Embeddings
- Callbacks / Streaming

## Why Comments Added In Code

Code me comments is style me add kiye gaye hain:

```java
// [ RAG chain: question -> retriever -> context augmented prompt -> LLM answer. ]
```

Purpose:

Jab koi code padhe, to us line/section par kaunsa LangChain concept use ho raha hai, wo instantly samajh aaye.

## Complete Request Flow Example

Example endpoint:

```bash
curl "http://localhost:8080/langchain/tools?sku=JDK-21"
```

Flow:

```text
1. Request LangChainController.tools() me aati hai.
2. Controller LangChainConceptService.toolIntegration("JDK-21") call karta hai.
3. Service toolAssistant.chat(...) call karta hai.
4. toolAssistant LangChain4j AiServices se bana hai.
5. AiServices ke paas InventoryTools registered hai.
6. Model decide karta hai ki stock check karne ke liye tool call chahiye.
7. InventoryTools.checkStock("JDK-21") run hota hai.
8. Final answer JSON response me return hota hai.
```

## Important Learning

LangChain4j raw model call ko application workflow me convert karta hai.

Raw model call:

```text
prompt -> model -> text
```

LangChain4j app flow:

```text
prompt template
    -> model
    -> output parser
    -> optional tool calls
    -> optional memory
    -> optional RAG context
    -> structured response
```

Isliye framework productivity badhti hai, lekin abstraction risk bhi hota hai. Production me logs, evaluation, tests, and clear comments important hain.
