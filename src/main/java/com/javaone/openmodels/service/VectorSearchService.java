package com.javaone.openmodels.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class VectorSearchService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final int defaultTopK;
    private final double defaultScoreThreshold;

    public VectorSearchService(EmbeddingModel embeddingModel,
                               EmbeddingStore<TextSegment> embeddingStore,
                               @Value("${rag.max-results}") int defaultTopK,
                               @Value("${rag.min-score}") double defaultScoreThreshold) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.defaultTopK = defaultTopK;
        this.defaultScoreThreshold = defaultScoreThreshold;
    }

    public List<SearchResult> similaritySearch(String query,
                                               Integer topK,
                                               Double scoreThreshold,
                                               String metadataKey,
                                               String metadataValue) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(topK == null ? defaultTopK : topK)
            .minScore(scoreThreshold == null ? defaultScoreThreshold : scoreThreshold)
            .filter(metadataFilter(metadataKey, metadataValue))
            .build();

        return embeddingStore.search(request).matches().stream()
            .map(VectorSearchService::toSearchResult)
            .toList();
    }

    private static Filter metadataFilter(String metadataKey, String metadataValue) {
        if (!StringUtils.hasText(metadataKey) || !StringUtils.hasText(metadataValue)) {
            return null;
        }
        return new IsEqualTo(metadataKey, metadataValue);
    }

    private static SearchResult toSearchResult(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        return new SearchResult(
            match.embeddingId(),
            match.score(),
            segment.text(),
            segment.metadata().toMap()
        );
    }

    public record SearchResult(String id, double score, String text, Map<String, Object> metadata) {}
}
