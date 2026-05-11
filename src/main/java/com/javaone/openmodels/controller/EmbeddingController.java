package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.EmbeddingLearningService;
import com.javaone.openmodels.service.EmbeddingLearningService.ComparisonResult;
import com.javaone.openmodels.service.EmbeddingLearningService.EmbeddingView;
import com.javaone.openmodels.service.EmbeddingLearningService.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class EmbeddingController {

    private final EmbeddingLearningService embeddingLearningService;
    private final int defaultMaxResults;
    private final double defaultMinScore;

    public EmbeddingController(EmbeddingLearningService embeddingLearningService,
                               @Value("${rag.max-results}") int defaultMaxResults,
                               @Value("${rag.min-score}") double defaultMinScore) {
        this.embeddingLearningService = embeddingLearningService;
        this.defaultMaxResults = defaultMaxResults;
        this.defaultMinScore = defaultMinScore;
    }

    @GetMapping("/embeddings")
    public Map<String, Object> concepts() {
        return Map.of(
            "status", "ready",
            "concepts", embeddingLearningService.concepts()
        );
    }

    @GetMapping("/embeddings/text")
    public EmbeddingView embedText(@RequestParam String text) {
        return embeddingLearningService.embed(text);
    }

    @GetMapping("/embeddings/compare")
    public ComparisonResult compare(@RequestParam String query,
                                    @RequestParam String document) {
        return embeddingLearningService.compare(query, document);
    }

    @PostMapping("/embeddings/search")
    public SearchResult search(@RequestBody SearchRequest request) {
        int maxResults = request.maxResults() == null ? defaultMaxResults : request.maxResults();
        double minScore = request.minScore() == null ? defaultMinScore : request.minScore();
        return embeddingLearningService.search(request.query(), request.documents(), maxResults, minScore);
    }

    public record SearchRequest(
        String query,
        List<String> documents,
        Integer maxResults,
        Double minScore
    ) {}
}
