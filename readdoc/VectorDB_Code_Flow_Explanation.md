# Vector DB Feature Code Flow - Simple Explanation

Ye document un code changes ko simple terms me explain karta hai jo `develop_vectordb_feature1` branch me add kiye gaye hain.

## 1. High-Level Picture

Is feature ka goal hai ki hum apne RAG demo me documents ko normal text ki tarah nahi, balki vectors/embeddings ki tarah store karein.

Flow simple hai:

1. Documents `docs/` folder se read hote hain.
2. Documents chhote chunks me split hote hain.
3. Har chunk ka embedding banta hai using Ollama embedding model.
4. Embeddings Elasticsearch me vector database ke रूप me store hote hain.
5. User question poochta hai.
6. Question ka bhi embedding banta hai.
7. Elasticsearch similar vectors search karta hai.
8. Best matching chunks LLM ko context ke रूप me milte hain.
9. LLM final grounded answer generate karta hai.

## 2. Important Files

### `pom.xml`

Yaha dependencies add/update ki gayi hain.

Important dependency:

```xml
<artifactId>langchain4j-elasticsearch</artifactId>
```

Is dependency se app Elasticsearch ko vector database ki tarah use kar paati hai.

LangChain4j version bhi update hua hai, kyunki Elasticsearch vector store ke liye newer version chahiye tha.

## 3. `application.yml`

Yaha vector DB ki settings rakhi gayi hain.

```yaml
vector-store:
  type: elasticsearch
  elasticsearch:
    url: http://localhost:9200
    index-name: javaone-rag-vectors
    dimensions: 768
    num-candidates: 100
```

Meaning:

- `type: elasticsearch`: app Elasticsearch vector store use karegi.
- `url`: Elasticsearch kaha chal raha hai.
- `index-name`: Elasticsearch index ka naam.
- `dimensions: 768`: `nomic-embed-text` model 768 size ka dense vector banata hai.
- `num-candidates: 100`: HNSW/kNN search me kitne candidate vectors consider karne hain.

RAG retrieval settings:

```yaml
rag:
  max-results: 5
  min-score: 0.7
```

Meaning:

- `max-results`: topK, yani maximum kitne chunks retrieve karne hain.
- `min-score`: score threshold, yani weak matches ignore karne hain.

## 4. `compose.yaml`

Is file se local Elasticsearch start hota hai.

```bash
docker compose up -d elasticsearch
```

Ye local machine par Elasticsearch ko `http://localhost:9200` par run karta hai.

## 5. `RagConfig.java`

Ye main configuration class hai.

### Embedding Model

```java
OllamaEmbeddingModel.builder()
    .baseUrl(baseUrl)
    .modelName(embeddingModelName)
    .build();
```

Iska kaam:

- Ollama se connect karna.
- `nomic-embed-text` model use karna.
- Text ko dense vector me convert karna.

### Elasticsearch Client

```java
RestClient.builder(HttpHost.create(elasticsearchUrl))
```

Iska kaam:

- Java app ko Elasticsearch se connect karna.
- Agar API key ya username/password diya ho, to authentication set karna.

### Embedding Store

```java
ElasticsearchEmbeddingStore.builder()
    .restClient(elasticsearchRestClient)
    .indexName(elasticsearchIndexName)
    .configuration(ElasticsearchConfigurationKnn.builder()
        .numCandidates(elasticsearchNumCandidates)
        .build())
    .build();
```

Iska kaam:

- Elasticsearch ko vector store banana.
- Index name set karna.
- kNN/HNSW search config set karna.

Fallback bhi hai:

```java
if ("in-memory".equalsIgnoreCase(vectorStoreType)) {
    return new InMemoryEmbeddingStore<>();
}
```

Agar `vector-store.type` ko `in-memory` kar diya, to app Elasticsearch ki jagah memory store use karegi.

### Content Retriever

```java
EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(model)
    .maxResults(maxResults)
    .minScore(minScore)
    .build();
```

Iska kaam:

- User question ko embedding me convert karna.
- Vector DB me similar chunks search karna.
- `topK` aur score threshold apply karna.

Ye retriever `/ask` endpoint ke RAG answer me use hota hai.

## 6. `DocumentIngestor.java`

Ye class documents ko vector DB me ingest karti hai.

Flow:

```java
List<Document> documents = FileSystemDocumentLoader.loadDocuments(documentsDir);
```

`docs/` folder se documents load hote hain.

```java
DocumentSplitters.recursive(chunkSize, chunkOverlap)
```

Documents chunks me split hote hain.

```java
EmbeddingStoreIngestor.builder()
    .documentSplitter(...)
    .embeddingModel(embeddingModel)
    .embeddingStore(embeddingStore)
    .build();
```

Har chunk ka embedding banta hai aur vector store me save hota hai.

Endpoint:

```bash
curl -X POST "http://localhost:8080/ingest"
```

## 7. `VectorSearchService.java`

Ye new service direct vector search ke liye banayi gayi hai.

### Query Embedding

```java
Embedding queryEmbedding = embeddingModel.embed(query).content();
```

User query ko vector me convert karta hai.

### Search Request

```java
EmbeddingSearchRequest.builder()
    .queryEmbedding(queryEmbedding)
    .maxResults(topK)
    .minScore(scoreThreshold)
    .filter(metadataFilter(...))
    .build();
```

Is request me:

- `queryEmbedding`: user question ka vector.
- `maxResults`: topK.
- `minScore`: score threshold.
- `filter`: metadata filter.

### Metadata Filtering

```java
new IsEqualTo(metadataKey, metadataValue)
```

Example:

```bash
curl "http://localhost:8080/vector/search?query=heap+size&metadataKey=file_name&metadataValue=java21-features.txt"
```

Iska meaning:

Sirf `java21-features.txt` file ke chunks ke andar search karo.

## 8. `RagController.java`

Ye REST endpoints expose karta hai.

### `/ingest`

```bash
curl -X POST "http://localhost:8080/ingest"
```

Documents ko read, split, embed aur store karta hai.

### `/ask`

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads?"
```

Full RAG flow run karta hai:

1. Question aata hai.
2. Similar chunks retrieve hote hain.
3. Chunks LLM ko context ke रूप me milte hain.
4. LLM answer return karta hai.

### `/vector/search`

```bash
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?&topK=3&scoreThreshold=0.7"
```

Ye direct vector search result dikhata hai. LLM answer generate nahi karta.

Use this when you want to see:

- Kaun se chunks retrieve hue.
- Similarity score kya hai.
- Metadata kya hai.

### `/vector/index`

```bash
curl "http://localhost:8080/vector/index"
```

Ye current vector DB settings show karta hai:

- Vector DB type.
- Elasticsearch index name.
- Dense vector dimensions.
- HNSW/kNN setting.
- topK.
- score threshold.
- Spring AI `VectorStore` equivalent abstraction.

## 9. `OllamaConfig.java`

LangChain4j newer version me `ChatLanguageModel` ki jagah `ChatModel` use hua.

Old style:

```java
ChatLanguageModel
```

New style:

```java
ChatModel
```

Aur builder method bhi update hua:

```java
.chatModel(chatModel)
```

Ye chat, tools aur RAG assistant ko LLM se connect karta hai.

## 10. Elasticsearch as Vector DB

Elasticsearch normally search engine hai, lekin modern Elasticsearch dense vectors store kar sakta hai.

Hum isme store kar rahe hain:

- Text chunk
- Embedding vector
- Metadata

Search ke time Elasticsearch nearest vectors find karta hai.

## 11. Dense Vector

Dense vector ek number array hota hai.

Example idea:

```text
"virtual threads" -> [0.12, -0.44, 0.98, ... total 768 numbers]
```

Similar meaning wale text ke vectors ek dusre ke near hote hain.

## 12. HNSW Index

HNSW ek approximate nearest-neighbor algorithm hai.

Simple meaning:

Pure database me har vector se compare karna slow hota hai. HNSW smart graph structure banata hai jisse similar vectors fast mil jaate hain.

Elasticsearch internally HNSW use karta hai for kNN search.

## 13. Similarity Search

Similarity search ka matlab:

Keyword exact match nahi, meaning match.

Example:

Query:

```text
JVM lightweight threads
```

Relevant chunk:

```text
Virtual threads are lightweight threads managed by the JVM.
```

Ye match ho sakta hai even agar exact words same na ho.

## 14. topK Retrieval

`topK` ka matlab top matching chunks ki count.

Example:

```text
topK = 3
```

To vector DB best 3 chunks return karegi.

## 15. Score Threshold

Score threshold weak matches ko filter karta hai.

Example:

```text
scoreThreshold = 0.7
```

To 0.7 se kam score wale chunks ignore honge.

## 16. Spring AI VectorStore Abstraction

Is repo me actual code LangChain4j use kar raha hai.

LangChain4j abstraction:

```java
EmbeddingStore<TextSegment>
```

Spring AI equivalent:

```java
VectorStore
```

Concept same hai:

Application ko Elasticsearch details directly nahi pata hoti. App abstraction se baat karti hai. Isse future me vector DB swap karna easy hota hai.

Example:

- Today: Elasticsearch
- Later: pgvector, Azure AI Search, Redis, etc.

## 17. Complete Run Flow

### Step 1: Start Elasticsearch

```bash
docker compose up -d elasticsearch
```

### Step 2: Start Ollama

```bash
ollama serve
```

Required models:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

### Step 3: Run Spring Boot App

```bash
mvn spring-boot:run
```

### Step 4: Ingest Documents

```bash
curl -X POST "http://localhost:8080/ingest"
```

### Step 5: Inspect Vector Index

```bash
curl "http://localhost:8080/vector/index"
```

### Step 6: Test Direct Vector Search

```bash
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?&topK=3&scoreThreshold=0.7"
```

### Step 7: Ask RAG Question

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

## 18. One-Line Summary

Documents ko chunks me tod kar embeddings banaye jaate hain, embeddings Elasticsearch me dense vectors ke रूप me store hote hain, aur query time par similar chunks retrieve karke LLM ko grounded answer generate karne ke liye diye jaate hain.
