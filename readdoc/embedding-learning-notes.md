# Embeddings Learning Notes

## Embedding Kya Hota Hai

Embedding text ka numeric representation hota hai. Model sentence, query, ya document ko float numbers ke vector me convert karta hai, jisme meaning encode hota hai.

Example:

```text
"Java virtual threads are lightweight"
  -> [0.0123, -0.2281, 0.4410, ...]
```

## Text To Vector Conversion

`EmbeddingModel` text ko read karta hai aur `Embedding` object return karta hai. Is project me `OllamaEmbeddingModel` use ho raha hai:

```java
Embedding embedding = embeddingModel.embed(text).content();
float[] vector = embedding.vector();
```

## Embedding Model

Current model `src/main/resources/application.yml` me configured hai:

```yaml
ollama:
  embedding-model: nomic-embed-text
```

Model badalne par vector dimensions aur similarity behavior change ho sakta hai.

## Semantic Meaning As Vector

Embedding ka goal exact keyword match nahi, meaning match karna hota hai. Isliye "Spring profile config" aur "application-profile.yml configuration" close vectors ho sakte hain even if words exact same nahi hain.

## Query Embedding

User ka question vector me convert hota hai. Similarity search me ye query vector document vectors se compare hota hai.

Endpoint:

```bash
curl "http://localhost:8080/embeddings/text?text=How+do+Spring+profiles+work"
```

## Document Embedding

Document ya chunk ka vector banaya jata hai. RAG pipeline me documents split hote hain, har chunk embedded hota hai, aur embedding store me save hota hai.

Code reference: `DocumentIngestor` uses `EmbeddingStoreIngestor`.

## Embedding Dimensions

Dimensions vector ke numbers ki count hai. Agar vector me 768 values hain, dimension 768 hai. Endpoint response me `dimensions` field aata hai.

## Similarity Search

Similarity search query embedding ko document embeddings ke against rank karta hai. Highest score wala document semantic meaning me closest hota hai.

Endpoint:

```bash
curl -X POST "http://localhost:8080/embeddings/search" \
  -H "Content-Type: application/json" \
  -d '{"query":"Java threads","documents":["Virtual threads are lightweight JVM threads.","Spring Boot uses application.yml."],"minScore":0.0}'
```

## Cosine Similarity

Cosine similarity vectors ke angle ko compare karta hai. Higher score ka matlab meaning closer hai.

```text
0.90 = very close
0.55 = somewhat related
0.20 = weakly related
```

## Vector Distance

This demo uses:

```text
vectorDistance = 1 - cosineSimilarity
```

Lower distance ka matlab query aur document vector space me closer hain.
