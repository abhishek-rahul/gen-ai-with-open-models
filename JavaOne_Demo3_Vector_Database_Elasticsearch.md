# Demo 3 Script: Vector Database with Elasticsearch

This demo shows how a Java RAG application stores dense vectors in Elasticsearch and retrieves relevant chunks using similarity search.

## What Is a Vector Database?

A vector database stores embeddings: dense numeric arrays that represent the semantic meaning of text, images, or other content. Instead of exact keyword matching, a vector DB finds nearby vectors, so questions like "JVM lightweight threads" can retrieve chunks about "virtual threads".

## Elasticsearch as Vector DB

Elasticsearch supports `dense_vector` fields and approximate kNN search. In this app, documents are split into chunks, embedded with Ollama `nomic-embed-text`, and stored through LangChain4j's `EmbeddingStore<TextSegment>`.

## Dense Vector

`nomic-embed-text` produces a dense vector with 768 dimensions. The vector is stored alongside the original text chunk and metadata such as the source file name.

## HNSW Index

Elasticsearch indexes dense vectors with HNSW for approximate nearest-neighbor search. HNSW trades a small amount of recall for much faster search on large indexes. The app configures kNN through `ElasticsearchConfigurationKnn` and exposes `num-candidates` in `application.yml`.

## Similarity Search

The `/vector/search` endpoint embeds the query text, searches the vector store, and returns matching chunks with scores:

```bash
curl "http://localhost:8080/vector/search?query=What+are+virtual+threads?"
```

## topK Retrieval

`topK` controls the maximum number of chunks returned:

```bash
curl "http://localhost:8080/vector/search?query=Spring+profiles&topK=3"
```

## Score Threshold

`scoreThreshold` filters low-confidence matches:

```bash
curl "http://localhost:8080/vector/search?query=Spring+profiles&scoreThreshold=0.75"
```

## Metadata Filtering

Metadata filters restrict search to specific chunks, for example a source file:

```bash
curl "http://localhost:8080/vector/search?query=heap+size&metadataKey=file_name&metadataValue=java21-features.txt"
```

## Vector Index Setup

Start Elasticsearch locally:

```bash
docker compose up -d elasticsearch
```

Then ingest the sample documents:

```bash
curl -X POST "http://localhost:8080/ingest"
```

Check the configured vector index:

```bash
curl "http://localhost:8080/vector/index"
```

## VectorStore Abstraction in Spring AI

This repository currently uses LangChain4j, where `EmbeddingStore<TextSegment>` is the vector-store abstraction. The Spring AI equivalent is `org.springframework.ai.vectorstore.VectorStore`: both hide vendor-specific details so the application code can switch from in-memory storage to Elasticsearch without rewriting ingestion and retrieval logic.
