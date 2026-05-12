package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.Assistant;
import com.javaone.openmodels.service.DocumentIngestor;
import com.javaone.openmodels.service.RagEvaluator;
import com.javaone.openmodels.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
public class RagController {

    private final DocumentIngestor documentIngestor;
    private final Assistant ragAssistant;
    private final RagEvaluator ragEvaluator;
    private final VectorSearchService vectorSearchService;
    private final String docsDir;
    private final String vectorStoreType;
    private final String elasticsearchIndexName;
    private final int vectorDimensions;
    private final int numCandidates;
    private final int maxResults;
    private final double minScore;

    public RagController(DocumentIngestor documentIngestor,
                         @Qualifier("ragAssistant") Assistant ragAssistant,
                         RagEvaluator ragEvaluator,
                         VectorSearchService vectorSearchService,
                         @Value("${rag.docs-dir}") String docsDir,
                         @Value("${vector-store.type}") String vectorStoreType,
                         @Value("${vector-store.elasticsearch.index-name}") String elasticsearchIndexName,
                         @Value("${vector-store.elasticsearch.dimensions}") int vectorDimensions,
                         @Value("${vector-store.elasticsearch.num-candidates}") int numCandidates,
                         @Value("${rag.max-results}") int maxResults,
                         @Value("${rag.min-score}") double minScore) {
        this.documentIngestor = documentIngestor;
        this.ragAssistant = ragAssistant;
        this.ragEvaluator = ragEvaluator;
        this.vectorSearchService = vectorSearchService;
        this.docsDir = docsDir;
        this.vectorStoreType = vectorStoreType;
        this.elasticsearchIndexName = elasticsearchIndexName;
        this.vectorDimensions = vectorDimensions;
        this.numCandidates = numCandidates;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        DocumentIngestor.IngestResult result = documentIngestor.ingest(Path.of(docsDir));
        return Map.of(
            "status", "success",
            "documents_ingested", result.documentsIngested(),
            "embedding_model", result.embeddingModel(),
            "store", result.store()
        );
    }

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question) {
        long start = System.currentTimeMillis();
        String answer = ragAssistant.chat(question);
        long latency = System.currentTimeMillis() - start;

        return Map.of(
            "answer", answer,
            "model", "qwen2.5:0.5b",
            "latency_ms", latency
        );
    }

    @GetMapping("/vector/search")
    public Map<String, Object> vectorSearch(@RequestParam String query,
                                            @RequestParam(required = false) Integer topK,
                                            @RequestParam(required = false) Double scoreThreshold,
                                            @RequestParam(required = false) String metadataKey,
                                            @RequestParam(required = false) String metadataValue) {
        return Map.of(
            "query", query,
            "topK", topK == null ? maxResults : topK,
            "score_threshold", scoreThreshold == null ? minScore : scoreThreshold,
            "metadata_filter", Map.of(
                "key", metadataKey == null ? "" : metadataKey,
                "value", metadataValue == null ? "" : metadataValue
            ),
            "results", vectorSearchService.similaritySearch(
                query,
                topK,
                scoreThreshold,
                metadataKey,
                metadataValue
            )
        );
    }

    @GetMapping("/vector/index")
    public Map<String, Object> vectorIndex() {
        return Map.of(
            "vector_database", vectorStoreType,
            "elasticsearch_index", elasticsearchIndexName,
            "dense_vector_dimensions", vectorDimensions,
            "index_algorithm", "HNSW via Elasticsearch approximate kNN",
            "similarity", "cosine",
            "num_candidates", numCandidates,
            "retrieval", Map.of(
                "topK", maxResults,
                "score_threshold", minScore,
                "metadata_filtering", "Use /vector/search?metadataKey=file_name&metadataValue=java21-features.txt"
            ),
            "abstraction", Map.of(
                "current_code", "LangChain4j EmbeddingStore<TextSegment>",
                "spring_ai_equivalent", "org.springframework.ai.vectorstore.VectorStore"
            )
        );
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate() {
        return ragEvaluator.evaluate();
    }
}
