# Embedding Code Flow Explanation

Bilkul. Simple terms me: humne project me ek **Embedding learning/demo feature** add kiya hai jisse aap dekh sakte ho ki text ka vector kaise banta hai, query aur document vectors kaise compare hote hain, aur semantic search kaise kaam karta hai.

## Overall Flow

Request browser/curl se aati hai:

```text
User / curl
   -> EmbeddingController
      -> EmbeddingLearningService
         -> Ollama Embedding Model
            -> Vector / Similarity / Search result
```

`RagConfig.java` already ek `EmbeddingModel` bean bana raha tha:

```java
@Bean
EmbeddingModel embeddingModel() {
    return OllamaEmbeddingModel.builder()
        .baseUrl(baseUrl)
        .modelName(embeddingModelName)
        .build();
}
```

Ye same embedding model humne naye service me inject kar liya. Matlab model config duplicate nahi kiya.

## EmbeddingController.java

Ye controller bas HTTP endpoints expose karta hai.

Important endpoints:

```java
GET /embeddings
```

Concepts explain karta hai: embedding kya hota hai, cosine similarity kya hai, etc.

```java
GET /embeddings/text?text=...
```

Given text ko vector me convert karta hai.

```java
GET /embeddings/compare?query=...&document=...
```

Query aur document dono ka embedding banata hai, phir unki cosine similarity nikalta hai.

```java
POST /embeddings/search
```

Query aur list of documents leta hai, sabka vector banata hai, phir best matching documents return karta hai.

Controller khud logic nahi karta. Wo sirf request leta hai aur `EmbeddingLearningService` ko call karta hai.

## EmbeddingLearningService.java

Ye main file hai.

Iske andar ye fields hain:

```java
private final EmbeddingModel embeddingModel;
private final String embeddingModelName;
```

`embeddingModel` actual model hai jo text ko vector me convert karta hai.

`embeddingModelName` config se aata hai:

```yaml
ollama:
  embedding-model: nomic-embed-text
```

Constructor me Spring ye dono inject karta hai:

```java
public EmbeddingLearningService(EmbeddingModel embeddingModel,
                                @Value("${ollama.embedding-model}") String embeddingModelName)
```

## 1. concepts() Method

```java
public Map<String, Object> concepts()
```

Ye sirf easy explanation return karta hai.

Example output concepts:

```text
embedding = Text ka numeric vector representation
query_embedding = User query ka vector
document_embedding = Document ka vector
cosine_similarity = Do vectors kitne similar hain
vector_distance = 1 - cosine similarity
```

Ye learning/demo endpoint ke liye hai.

## 2. embed(String text)

```java
public EmbeddingView embed(String text) {
    Embedding embedding = embeddingModel.embed(text).content();
    return view("text_embedding", text, embedding);
}
```

Iska kaam:

```text
Input text
   -> embeddingModel.embed(text)
   -> Embedding object
   -> response me dimensions + vector preview
```

Actual vector bahut bada hota hai, isliye response me sirf first 8 numbers dikhaye gaye hain.

Example:

```json
{
  "type": "text_embedding",
  "sourceText": "Java virtual threads",
  "model": "nomic-embed-text",
  "dimensions": 768,
  "vectorPreview": [0.1234, -0.0441]
}
```

## 3. compare(String query, String document)

```java
public ComparisonResult compare(String query, String document)
```

Iska flow:

```text
Query text ka vector banao
Document text ka vector banao
Dono vectors ki cosine similarity nikalo
Vector distance nikalo
Readable interpretation return karo
```

Code:

```java
Embedding queryEmbedding = embeddingModel.embed(query).content();
Embedding documentEmbedding = embeddingModel.embed(document).content();
double cosineSimilarity = CosineSimilarity.between(queryEmbedding, documentEmbedding);
```

Then:

```java
vectorDistance = 1 - cosineSimilarity
```

Simple meaning:

```text
cosineSimilarity high = query aur document ka meaning close
vectorDistance low = query aur document paas hain
```

## 4. search(String query, List<String> documents, ...)

Ye sabse important method hai.

```java
public SearchResult search(String query, List<String> documents, int maxResults, double minScore)
```

Flow:

```text
Query aayi
Documents ki list aayi
Documents ko TextSegment banaya
Query ka embedding banaya
Documents ke embeddings banaye
Temporary in-memory vector store banaya
Documents ke vectors store me add kiye
Query vector se similarity search ki
Best matching documents return kiye
```

Important part:

```java
Embedding queryEmbedding = embeddingModel.embed(query).content();
List<Embedding> documentEmbeddings = embeddingModel.embedAll(segments).content();
```

Yaha query aur documents dono vector me convert ho gaye.

Phir:

```java
EmbeddingStore<TextSegment> demoStore = new InMemoryEmbeddingStore<>();
demoStore.addAll(documentEmbeddings, segments);
```

Yaha temporary vector store ban raha hai. Ye actual RAG store ko touch nahi karta. Sirf demo/search request ke liye local memory me store banata hai.

Search request:

```java
EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
    .queryEmbedding(queryEmbedding)
    .maxResults(maxResults)
    .minScore(minScore)
    .build();
```

Then:

```java
demoStore.search(searchRequest).matches()
```

Ye matching documents return karta hai.

## Why Temporary Store?

RAG pipeline ka actual store already hai:

```java
EmbeddingStore<TextSegment> embeddingStore()
```

Lekin `/embeddings/search` learning demo endpoint hai. User request body me documents bhejta hai. Hum un documents ko permanent RAG store me nahi daalte.

So:

```text
RAG /ingest store = real docs ke liye
/embeddings/search store = temporary demo ke liye
```

Good separation.

## Helper Methods

`view(...)`

```java
private EmbeddingView view(String type, String sourceText, Embedding embedding)
```

Ye response object banata hai:

```text
type
source text
model name
dimensions
vector preview
```

`preview(...)`

```java
private List<Double> preview(Embedding embedding, int limit)
```

Actual vector me hundreds of numbers ho sakte hain. Ye sirf first 8 values return karta hai.

`explainScore(...)`

```java
private String explainScore(double cosineSimilarity)
```

Score ko human language me explain karta hai:

```text
>= 0.8  high similarity
>= 0.5  medium similarity
else    low similarity
```

`round(...)`

```java
private double round(double value)
```

Response clean dikhane ke liye values ko 4 decimal places tak round karta hai.

## DocumentIngestor.java Change

Pehle hardcoded tha:

```java
return new IngestResult(documents.size(), "nomic-embed-text", "in-memory");
```

Ab config se value aa rahi hai:

```java
return new IngestResult(documents.size(), embeddingModelName, "in-memory");
```

Iska benefit: agar kal `application.yml` me model change karoge, response bhi correct model name dikhayega.

## Short Version

```text
EmbeddingController = endpoints
EmbeddingLearningService = actual embedding logic
RagConfig = embedding model bean
Ollama = actual vector banata hai
InMemoryEmbeddingStore = temporary vector search
README + notes = demo usage and learning material
```

Sabse important baat: embedding feature ne existing RAG flow ko disturb nahi kiya. Ye ek separate learning/demo layer hai jo same configured embedding model reuse karti hai.
