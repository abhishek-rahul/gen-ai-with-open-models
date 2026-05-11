package com.javaone.openmodels.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingLearningService {

    private static final int DEFAULT_PREVIEW_SIZE = 8;

    private final EmbeddingModel embeddingModel;
    private final String embeddingModelName;

    public EmbeddingLearningService(EmbeddingModel embeddingModel,
                                    @Value("${ollama.embedding-model}") String embeddingModelName) {
        this.embeddingModel = embeddingModel;
        this.embeddingModelName = embeddingModelName;
    }

    public Map<String, Object> concepts() {
        Map<String, Object> concepts = new LinkedHashMap<>();
        concepts.put("embedding", "Text ka numeric vector representation jisme semantic meaning encode hota hai.");
        concepts.put("text_to_vector_conversion", "Embedding model text ko float numbers ke vector me convert karta hai.");
        concepts.put("embedding_model", embeddingModelName + " via Ollama");
        concepts.put("semantic_meaning_as_vector", "Similar meaning wale texts vector space me paas aate hain.");
        concepts.put("query_embedding", "User query ka vector, jo search ke time compare hota hai.");
        concepts.put("document_embedding", "Document/chunk ka vector, jo vector store me save hota hai.");
        concepts.put("embedding_dimensions", "Vector me total numeric values. Is model ke liye runtime dimension API/vector se milega.");
        concepts.put("similarity_search", "Query vector ko document vectors ke against rank karna.");
        concepts.put("cosine_similarity", "Do vectors ke angle-based similarity score. Higher means more semantically similar.");
        concepts.put("vector_distance", "1 - cosine similarity. Lower distance means closer meaning.");
        return concepts;
    }

    public EmbeddingView embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        return view("text_embedding", text, embedding);
    }

    public ComparisonResult compare(String query, String document) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        Embedding documentEmbedding = embeddingModel.embed(document).content();
        double cosineSimilarity = CosineSimilarity.between(queryEmbedding, documentEmbedding);

        return new ComparisonResult(
            embeddingModelName,
            view("query_embedding", query, queryEmbedding),
            view("document_embedding", document, documentEmbedding),
            round(cosineSimilarity),
            round(1 - cosineSimilarity),
            explainScore(cosineSimilarity)
        );
    }

    public SearchResult search(String query, List<String> documents, int maxResults, double minScore) {
        List<String> safeDocuments = documents == null ? List.of() : documents;
        List<TextSegment> segments = safeDocuments.stream()
            .filter(document -> document != null && !document.isBlank())
            .map(TextSegment::from)
            .toList();

        if (segments.isEmpty()) {
            return new SearchResult(embeddingModelName, embed(query), List.of());
        }

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<Embedding> documentEmbeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> demoStore = new InMemoryEmbeddingStore<>();
        demoStore.addAll(documentEmbeddings, segments);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(minScore)
            .build();

        List<SearchMatch> matches = demoStore.search(searchRequest).matches().stream()
            .sorted(Comparator.comparing(EmbeddingMatch<TextSegment>::score).reversed())
            .map(match -> new SearchMatch(
                match.embedded().text(),
                round(match.score()),
                round(1 - match.score()),
                match.embedding().dimension(),
                preview(match.embedding(), DEFAULT_PREVIEW_SIZE)
            ))
            .toList();

        return new SearchResult(
            embeddingModelName,
            view("query_embedding", query, queryEmbedding),
            matches
        );
    }

    private EmbeddingView view(String type, String sourceText, Embedding embedding) {
        return new EmbeddingView(
            type,
            sourceText,
            embeddingModelName,
            embedding.dimension(),
            preview(embedding, DEFAULT_PREVIEW_SIZE)
        );
    }

    private List<Double> preview(Embedding embedding, int limit) {
        float[] vector = embedding.vector();
        int previewLength = Math.min(limit, vector.length);
        List<Double> preview = new ArrayList<>(previewLength);
        for (int i = 0; i < previewLength; i++) {
            preview.add(round(vector[i]));
        }
        return preview;
    }

    private String explainScore(double cosineSimilarity) {
        if (cosineSimilarity >= 0.8) {
            return "High semantic similarity: query aur document ka meaning kaafi close hai.";
        }
        if (cosineSimilarity >= 0.5) {
            return "Medium semantic similarity: kuch meaning overlap hai.";
        }
        return "Low semantic similarity: vectors semantic space me door hain.";
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    public record EmbeddingView(
        String type,
        String sourceText,
        String model,
        int dimensions,
        List<Double> vectorPreview
    ) {}

    public record ComparisonResult(
        String model,
        EmbeddingView queryEmbedding,
        EmbeddingView documentEmbedding,
        double cosineSimilarity,
        double vectorDistance,
        String interpretation
    ) {}

    public record SearchResult(
        String model,
        EmbeddingView queryEmbedding,
        List<SearchMatch> matches
    ) {}

    public record SearchMatch(
        String document,
        double cosineSimilarity,
        double vectorDistance,
        int dimensions,
        List<Double> vectorPreview
    ) {}
}
