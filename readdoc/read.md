# Vector DB Feature Testing Steps

Ye file `develop_vectordb_feature1` branch ke Vector DB changes test karne ke liye hai.

## 1. Current Branch Check

```bash
git branch
```

Expected branch:

```text
develop_vectordb_feature1
```

## 2. Required Tools Check

Java:

```bash
java -version
```

Maven:

```bash
mvn -version
```

Docker:

```bash
docker --version
```

Ollama:

```bash
ollama --version
```

## 3. Pull Required Ollama Models

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

Optional model:

```bash
ollama pull mistral:7b
```

## 4. Start Ollama

If Ollama is not already running:

```bash
ollama serve
```

Verify models:

```bash
ollama list
```

## 5. Start Elasticsearch

From project root:

```bash
docker compose up -d elasticsearch
```

Verify container:

```bash
docker ps
```

Verify Elasticsearch health:

```bash
curl "http://localhost:9200"
```

Expected: Elasticsearch JSON response.

## 6. Build and Compile Test

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

Note: Project me abhi test classes nahi hain, so this mainly verifies compile and dependency setup.

## 7. Run Spring Boot App

```bash
mvn spring-boot:run
```

Expected:

```text
Started OpenModelsDemoApplication
```

Application URL:

```text
http://localhost:8080
```

## 8. Test Basic Chat

```bash
curl "http://localhost:8080/chat?message=What+is+Java+21?"
```

Expected:

- JSON response.
- `response` field should contain model answer.

## 9. Test Vector Index Info

```bash
curl "http://localhost:8080/vector/index"
```

Expected response should include:

- `vector_database`
- `elasticsearch_index`
- `dense_vector_dimensions`
- `index_algorithm`
- `similarity`
- `retrieval`
- `abstraction`

Example fields:

```json
{
  "vector_database": "elasticsearch",
  "elasticsearch_index": "javaone-rag-vectors",
  "dense_vector_dimensions": 768,
  "index_algorithm": "HNSW via Elasticsearch approximate kNN"
}
```

## 10. Ingest Documents Into Vector DB

```bash
curl -X POST "http://localhost:8080/ingest"
```

Expected:

```json
{
  "status": "success",
  "documents_ingested": 3,
  "embedding_model": "nomic-embed-text",
  "store": "elasticsearch"
}
```

## 11. Verify Elasticsearch Index Created

```bash
curl "http://localhost:9200/_cat/indices?v"
```

Expected:

Index list should contain:

```text
javaone-rag-vectors
```

## 12. Direct Vector Similarity Search

```bash
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?"
```

Expected:

- JSON response.
- `results` array should contain matching chunks.
- Each result should have:
  - `id`
  - `score`
  - `text`
  - `metadata`

## 13. Test topK Retrieval

```bash
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?&topK=3"
```

Expected:

- Maximum 3 results.

## 14. Test Score Threshold

```bash
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?&scoreThreshold=0.75"
```

Expected:

- Results with score lower than `0.75` should be filtered out.

## 15. Test Metadata Filtering

```bash
curl "http://localhost:8080/vector/search?query=heap+size&metadataKey=file_name&metadataValue=java21-features.txt"
```

Expected:

- Results should come only from matching metadata.
- Metadata should include source file details.

If no results come, try checking actual metadata key returned by normal vector search:

```bash
curl "http://localhost:8080/vector/search?query=heap+size"
```

Then use the metadata key visible in response.

## 16. Test Full RAG Ask Flow

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

Expected:

- JSON response with `answer`.
- Answer should be grounded in ingested docs.
- Response should also include `model` and `latency_ms`.

## 17. Test RAG Evaluation

```bash
curl -X POST "http://localhost:8080/evaluate"
```

Expected:

- JSON response with evaluation metrics:
  - `faithfulness`
  - `answer_relevance`
  - `context_precision`
  - `source_accuracy`
  - `average_latency_ms`

## 18. Optional: Test In-Memory Fallback

Change `src/main/resources/application.yml`:

```yaml
vector-store:
  type: in-memory
```

Restart app:

```bash
mvn spring-boot:run
```

Ingest again:

```bash
curl -X POST "http://localhost:8080/ingest"
```

Expected:

```json
{
  "store": "in-memory"
}
```

This confirms abstraction is working.

## 19. Common Issues

### Elasticsearch connection refused

Check:

```bash
docker ps
curl "http://localhost:9200"
```

Fix:

```bash
docker compose up -d elasticsearch
```

### Ollama connection refused

Check:

```bash
ollama list
```

Fix:

```bash
ollama serve
```

### Embedding model missing

Fix:

```bash
ollama pull nomic-embed-text
```

### Chat model missing

Fix:

```bash
ollama pull qwen2.5:0.5b
```

### No vector search results

Try:

```bash
curl -X POST "http://localhost:8080/ingest"
curl "http://localhost:8080/vector/search?query=Java+virtual+threads&topK=5&scoreThreshold=0.3"
```

If lower threshold gives results, original threshold was too strict.

## 20. Cleanup

Stop Spring Boot app:

```text
Ctrl + C
```

Stop Elasticsearch:

```bash
docker compose down
```

Remove Elasticsearch data also:

```bash
docker compose down -v
```

Use `-v` only when you want to delete indexed vector data.
