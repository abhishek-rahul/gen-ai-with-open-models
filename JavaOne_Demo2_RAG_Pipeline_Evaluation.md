# Demo 2 Script: RAG Pipeline with Quality Evaluation

## Overview

This demo builds a Retrieval-Augmented Generation pipeline in Java using LangChain4j with an open model (Llama 3.1 8B via Ollama). You ingest documents into a vector store, query them, generate grounded answers, and run reproducible quality evaluations.

**Duration:** ~5 minutes  
**Requirements:** Ollama running (from Demo 1), Java 21+, Maven, PostgreSQL with pgvector (or in-memory store)  
**No external API keys needed — runs entirely locally**

---

## Pre-Demo Setup (Do Before the Session)

### 1. Prerequisites from Demo 1

- Ollama installed and running with `llama3.1:8b` and `nomic-embed-text` pulled
- Java 21+ and Maven installed

```bash
ollama list
# Should show: llama3.1:8b, nomic-embed-text
```

### 2. Set Up pgvector (Recommended) or Use In-Memory Store

**Option A: pgvector via Docker**

```bash
docker run -d \
  --name pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vectordb \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

**Option B: Testcontainers (auto-starts in tests)**

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
```

**Option C: In-memory store (simplest for demo)**

No setup needed — LangChain4j's `InMemoryEmbeddingStore` works out of the box.

### 3. Add RAG Dependencies (pom.xml)

```xml
<!-- Embedding store (pick one) -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>1.0.0-beta1</version>
</dependency>

<!-- Document loaders -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-document-parser-apache-pdfbox</artifactId>
    <version>1.0.0-beta1</version>
</dependency>

<!-- Ollama for embeddings (already added in Demo 1) -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>1.0.0-beta1</version>
</dependency>
```

### 4. Prepare Sample Documents

Create a `docs/` directory with 3–5 sample files (plain text or PDF). Example:

```
docs/
├── java21-features.txt        # Java 21 release notes excerpt
├── spring-boot-config.txt     # Spring Boot configuration guide
├── kubernetes-java-deploy.txt # K8s deployment guide for Java apps
```

**java21-features.txt** (sample):

```text
Java 21 LTS introduces several key features:

Virtual Threads (JEP 444): Lightweight threads that dramatically reduce the cost
of concurrent programming. Virtual threads are managed by the JVM rather than the
OS, allowing millions of concurrent threads.

Record Patterns (JEP 440): Extends pattern matching to destructure record values,
enabling concise and type-safe data extraction.

String Templates (Preview - JEP 430): Allows embedding expressions directly in
strings for safer string composition.

Sequenced Collections (JEP 431): New interfaces for collections with a defined
encounter order, adding first(), last(), and reversed() methods.

The minimum recommended heap size for Java 21 applications is 256 MB for
microservices and 512 MB for typical web applications.
```

### 5. Build and Verify

```bash
mvn clean package -DskipTests
mvn spring-boot:run
# Hit http://localhost:8080/ingest to populate the vector store
# Hit http://localhost:8080/ask?question=What+are+virtual+threads
```

---

## Live Demo Script

### Step 1: Show the RAG Architecture (30 seconds)

> "In Demo 1 we ran an LLM locally. But the model only knows what it was trained on. For enterprise apps, you need it to answer questions about YOUR data — internal docs, runbooks, product specs. That's what RAG does."

Show the slide with the RAG architecture diagram, then switch to VS Code.

### Step 2: Show the Ingestion Code (60 seconds)

Open **DocumentIngestor.java**:

```java
@Service
public class DocumentIngestor {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentIngestor(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public int ingest(Path documentsDir) {
        // 1. Load documents
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(documentsDir);

        // 2. Split into chunks
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);

        // 3. Embed and store
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(splitter)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();

        ingestor.ingest(documents);
        return documents.size();
    }
}
```

> "Three steps: load documents, split into chunks of 500 characters with 50-character overlap, then embed each chunk with `nomic-embed-text` — our local embedding model in Ollama — and store in pgvector.
>
> Chunk size matters. Too big and you get irrelevant noise in the context. Too small and you lose meaning. 500 characters with overlap is a reasonable default — but you should experiment."

Show **RagConfig.java**:

```java
@Configuration
public class RagConfig {

    @Bean
    EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("nomic-embed-text")
            .build();
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store,
                                      EmbeddingModel model) {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(model)
            .maxResults(5)
            .minScore(0.7)
            .build();
    }

    @Bean
    Assistant ragAssistant(ChatLanguageModel chatModel,
                           ContentRetriever retriever) {
        return AiServices.builder(Assistant.class)
            .chatLanguageModel(chatModel)
            .contentRetriever(retriever)
            .build();
    }
}
```

> "The `ContentRetriever` handles the vector search automatically. When the assistant gets a question, LangChain4j embeds the query, searches the store for the top 5 most similar chunks above a 0.7 similarity threshold, and injects them into the prompt. All behind the scenes."

### Step 3: Ingest Documents (30 seconds)

```bash
curl -s -X POST "http://localhost:8080/ingest" | python3 -m json.tool
```

**Expected output:**

```json
{
  "status": "success",
  "documents_ingested": 3,
  "chunks_created": 18,
  "embedding_model": "nomic-embed-text",
  "store": "pgvector"
}
```

> "Three documents, 18 chunks. Embedding was done locally by `nomic-embed-text` in Ollama."

### Step 4: Query the RAG Pipeline (60 seconds)

```bash
curl -s "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21+and+what+is+the+recommended+heap+size?" | python3 -m json.tool
```

**Expected output:**

```json
{
  "answer": "Virtual Threads, introduced in Java 21 via JEP 444, are lightweight threads managed by the JVM rather than the operating system. They dramatically reduce the cost of concurrent programming by allowing millions of concurrent threads without the overhead of OS threads. According to the documentation, the minimum recommended heap size for Java 21 microservices is 256 MB, and 512 MB for typical web applications.",
  "sources": [
    {
      "content": "Virtual Threads (JEP 444): Lightweight threads that dramatically reduce ...",
      "score": 0.89,
      "source": "java21-features.txt"
    },
    {
      "content": "The minimum recommended heap size for Java 21 applications is 256 MB ...",
      "score": 0.82,
      "source": "java21-features.txt"
    }
  ],
  "model": "llama3.1:8b",
  "latency_ms": 1842
}
```

> "The answer cites specific numbers from our document — 256 MB for microservices, 512 MB for web apps. Those aren't in the model's training data; they came from our vector store. Notice the similarity scores — 0.89 and 0.82. The model used two chunks as context to compose a single coherent answer.
>
> This is the core value of RAG: the model generates fluent answers grounded in your actual documentation."

### Step 5: Run Quality Evaluation (90 seconds)

> "But how do we know the answer is GOOD? In production, you can't eyeball every response. You need reproducible metrics."

Show **RagEvaluator.java**:

```java
@Service
public class RagEvaluator {

    record EvalCase(String question, String expectedAnswer, String expectedSource) {}

    private final List<EvalCase> testSet = List.of(
        new EvalCase(
            "What are virtual threads?",
            "Lightweight threads managed by the JVM, introduced in Java 21 via JEP 444",
            "java21-features.txt"
        ),
        new EvalCase(
            "What is the minimum heap for microservices?",
            "256 MB",
            "java21-features.txt"
        ),
        new EvalCase(
            "How do you configure Spring Boot profiles?",
            "Using application-{profile}.yml files",
            "spring-boot-config.txt"
        )
    );

    public EvalResult evaluate(Assistant assistant, ContentRetriever retriever) {
        // Run each test case
        // Measure: faithfulness, relevance, context precision, source accuracy
        // Return aggregate scores
    }
}
```

> "A golden test set: known questions with expected answers and expected sources. This is your CI/CD gate for LLMs."

Run the evaluation:

```bash
curl -s -X POST "http://localhost:8080/evaluate" | python3 -m json.tool
```

**Expected output:**

```json
{
  "model": "llama3.1:8b",
  "test_cases": 3,
  "results": {
    "faithfulness": 0.92,
    "answer_relevance": 0.88,
    "context_precision": 0.85,
    "source_accuracy": 1.0,
    "average_latency_ms": 1650
  },
  "details": [
    {
      "question": "What are virtual threads?",
      "faithfulness": 0.95,
      "relevance": 0.90,
      "correct_source": true,
      "latency_ms": 1842
    },
    {
      "question": "What is the minimum heap for microservices?",
      "faithfulness": 1.0,
      "relevance": 0.90,
      "correct_source": true,
      "latency_ms": 1203
    },
    {
      "question": "How do you configure Spring Boot profiles?",
      "faithfulness": 0.82,
      "relevance": 0.85,
      "correct_source": true,
      "latency_ms": 1905
    }
  ]
}
```

> "Let's read these metrics:
> - **Faithfulness 0.92** — the model's answers are supported by the retrieved context 92% of the time
> - **Answer relevance 0.88** — the answers actually address the questions asked
> - **Context precision 0.85** — the chunks we retrieved were relevant (vs. noise)
> - **Source accuracy 1.0** — every answer came from the correct source document
>
> These numbers become your baseline. When you swap models, change chunk sizes, or update your prompt — re-run this eval. If faithfulness drops below 0.85, don't ship it."

### Step 6: Compare Models (Optional, 30 seconds)

If time permits:

```bash
# Switch to Mistral 7B and re-evaluate
curl -s -X POST "http://localhost:8080/evaluate?model=mistral:7b" | python3 -m json.tool
```

> "Same test set, same documents, different model. Mistral scores 0.88 on faithfulness vs Llama's 0.92 on this particular data set, but it's 20% faster. THIS is how you make production decisions — not from public benchmarks, but from your own eval on your own data."

### Step 7: Recap (30 seconds)

> "What we just built:
> - A complete RAG pipeline: ingest → embed → store → retrieve → generate
> - All running locally — Ollama for both the LLM and the embedding model, pgvector for storage
> - Reproducible quality evaluation with four metrics
> - Model comparison on our actual data
>
> The same code deploys to Azure. Swap `OllamaChatModel` for `AzureOpenAiChatModel`, swap the in-memory store for Azure AI Search — the `Assistant` interface and evaluation code don't change.
>
> That's the power of LangChain4j's abstraction layer."

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `nomic-embed-text` not found | Run `ollama pull nomic-embed-text` |
| pgvector connection refused | Start container: `docker start pgvector` or use `InMemoryEmbeddingStore` |
| Low similarity scores | Check chunk size — try 300 or 800 characters; adjust `minScore` threshold |
| Slow ingestion | Embedding large docs takes time; pre-ingest before the demo |
| `OutOfMemoryError` during ingestion | Increase heap: `MAVEN_OPTS="-Xmx4g" mvn spring-boot:run` |
| Eval scores seem random | Ensure `temperature(0.0)` for deterministic evaluation runs |
| Model refuses tool calling | `nomic-embed-text` is embedding-only; use `llama3.1:8b` for chat |

---

## Key Code Files

| File | Purpose |
|------|---------|
| `OllamaConfig.java` | Chat and embedding model configuration |
| `RagConfig.java` | ContentRetriever and embedding store wiring |
| `DocumentIngestor.java` | Document loading, splitting, embedding, storage |
| `RagController.java` | REST endpoints: `/ingest`, `/ask`, `/evaluate` |
| `RagEvaluator.java` | Golden test set with faithfulness/relevance/precision metrics |
| `InventoryTools.java` | Example `@Tool` for Demo 1 tool calling |
| `Assistant.java` | AI Service interface |

---

## Links

- [LangChain4j RAG documentation](https://docs.langchain4j.dev/tutorials/rag)
- [LangChain4j embedding stores](https://docs.langchain4j.dev/integrations/embedding-stores/)
- [pgvector — github.com/pgvector/pgvector](https://github.com/pgvector/pgvector)
- [Ollama embedding models](https://ollama.com/search?c=embedding)
- [nomic-embed-text model card](https://ollama.com/library/nomic-embed-text)
- [LangChain4j document loaders](https://docs.langchain4j.dev/integrations/document-loaders/)
- [Azure AI Search + LangChain4j](https://docs.langchain4j.dev/integrations/embedding-stores/azure-ai-search)
- [RAGAS evaluation framework](https://docs.ragas.io/) (Python reference for metric definitions)
- [Microsoft Foundry model catalog](https://ai.azure.com/explore/models)
- [GitHub Models](https://github.com/marketplace/models)
- [JavaOne 2026 session catalog](https://reg.rf.oracle.com/flow/oracle/javaone26/catalog/page/catalog?search=benz)
