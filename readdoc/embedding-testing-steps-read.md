# Embedding Feature Testing Steps

Ye guide embedding feature ko local machine par test karne ke liye hai.

## 1. Prerequisites Check

Java version check karo:

```bash
java -version
```

Maven version check karo:

```bash
mvn -version
```

Ollama running hona chahiye:

```bash
ollama list
```

Required models available hone chahiye:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

## 2. Build/Test Project

Project compile aur test karo:

```bash
mvn test
```

Expected result:

```text
BUILD SUCCESS
```

Note: Project me abhi test classes nahi hain, so Maven compile verify karega aur "No tests to run" dikha sakta hai.

## 3. Start Application

Spring Boot app start karo:

```bash
mvn spring-boot:run
```

Expected:

```text
Started OpenModelsDemoApplication
```

Application default port:

```text
http://localhost:8080
```

## 4. Test Embedding Concepts Endpoint

Command:

```bash
curl "http://localhost:8080/embeddings"
```

Expected:

```json
{
  "status": "ready",
  "concepts": {
    "embedding": "...",
    "text_to_vector_conversion": "...",
    "cosine_similarity": "...",
    "vector_distance": "..."
  }
}
```

Is endpoint se verify hota hai ki controller working hai aur embedding concepts response aa raha hai.

## 5. Test Text To Vector Conversion

Command:

```bash
curl "http://localhost:8080/embeddings/text?text=Java+virtual+threads+are+lightweight"
```

Expected fields:

```json
{
  "type": "text_embedding",
  "sourceText": "Java virtual threads are lightweight",
  "model": "nomic-embed-text",
  "dimensions": 768,
  "vectorPreview": [0.1234, -0.0441]
}
```

Important checks:

```text
dimensions > 0
vectorPreview contains numeric values
model should match application.yml
```

## 6. Test Query And Document Comparison

Command:

```bash
curl "http://localhost:8080/embeddings/compare?query=What+are+virtual+threads%3F&document=Virtual+threads+are+lightweight+JVM+threads+introduced+in+Java+21"
```

Expected fields:

```json
{
  "model": "nomic-embed-text",
  "queryEmbedding": {
    "type": "query_embedding",
    "dimensions": 768
  },
  "documentEmbedding": {
    "type": "document_embedding",
    "dimensions": 768
  },
  "cosineSimilarity": 0.8,
  "vectorDistance": 0.2,
  "interpretation": "High semantic similarity..."
}
```

Important checks:

```text
queryEmbedding and documentEmbedding should both come
cosineSimilarity should be higher for related text
vectorDistance should be 1 - cosineSimilarity
```

## 7. Test Similarity Search

Command:

```bash
curl -X POST "http://localhost:8080/embeddings/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How does Spring Boot configuration work?",
    "documents": [
      "Spring Boot supports application.yml and profile-specific configuration files.",
      "Java virtual threads are lightweight threads managed by the JVM.",
      "Kubernetes can deploy Java services with containers and health checks."
    ],
    "maxResults": 3,
    "minScore": 0.0
  }'
```

Expected:

```json
{
  "model": "nomic-embed-text",
  "queryEmbedding": {
    "type": "query_embedding",
    "dimensions": 768
  },
  "matches": [
    {
      "document": "Spring Boot supports application.yml and profile-specific configuration files.",
      "cosineSimilarity": 0.7,
      "vectorDistance": 0.3,
      "dimensions": 768,
      "vectorPreview": [0.1234, -0.0441]
    }
  ]
}
```

Important checks:

```text
Spring Boot document should generally rank above Java/Kubernetes docs
matches count should be <= maxResults
vectorDistance should be lower for better matches
```

## 8. Test With Higher minScore

Command:

```bash
curl -X POST "http://localhost:8080/embeddings/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How does Spring Boot configuration work?",
    "documents": [
      "Spring Boot supports application.yml and profile-specific configuration files.",
      "Java virtual threads are lightweight threads managed by the JVM."
    ],
    "maxResults": 3,
    "minScore": 0.8
  }'
```

Expected behavior:

```text
Only high similarity matches return honge.
Agar score threshold high hai to matches empty bhi ho sakte hain.
```

## 9. Test Existing RAG Flow Is Still Working

Ingest documents:

```bash
curl -X POST "http://localhost:8080/ingest"
```

Ask question:

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

Expected:

```text
RAG answer should come.
Embedding demo changes existing /ingest and /ask flow ko break nahi karne chahiye.
```

## 10. Common Issues

If Ollama is not running:

```text
Connection refused / timeout error aa sakta hai.
Fix: ollama serve start karo.
```

If embedding model missing hai:

```text
Model not found error aa sakta hai.
Fix: ollama pull nomic-embed-text
```

If response slow hai:

```text
First request model load kar sakti hai, isliye first call slow ho sakta hai.
Second call usually faster hoti hai.
```

## 11. Final Verification Checklist

```text
[ ] mvn test BUILD SUCCESS
[ ] mvn spring-boot:run app starts
[ ] GET /embeddings returns status ready
[ ] GET /embeddings/text returns dimensions and vectorPreview
[ ] GET /embeddings/compare returns cosineSimilarity and vectorDistance
[ ] POST /embeddings/search returns ranked matches
[ ] POST /ingest still works
[ ] GET /ask still works
```
