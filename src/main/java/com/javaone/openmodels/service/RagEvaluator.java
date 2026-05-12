package com.javaone.openmodels.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RagEvaluator {

    record EvalCase(String question, String expectedAnswer, String expectedSource) {}

    private final List<EvalCase> testSet = List.of(
        new EvalCase(
            "What are virtual threads?",
            "Lightweight threads managed by the JVM, introduced in Java 21 via JEP 444",
            "java21-features.txt"
        ),
        new EvalCase(
            "What is the minimum heap for microservices?",
            "256 MB",
            "java21-features.txt"
        ),
        new EvalCase(
            "How do you configure Spring Boot profiles?",
            "Using application-{profile}.yml files",
            "spring-boot-config.txt"
        )
    );

    private final SpringAiBasicsService springAi;
    private final VectorStore vectorStore;
    private final int maxResults;
    private final double minScore;

    public RagEvaluator(SpringAiBasicsService springAi,
                        VectorStore vectorStore,
                        @Value("${rag.max-results}") int maxResults,
                        @Value("${rag.min-score}") double minScore) {
        this.springAi = springAi;
        this.vectorStore = vectorStore;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    public Map<String, Object> evaluate() {
        List<Map<String, Object>> details = new ArrayList<>();
        double totalFaithfulness = 0;
        double totalRelevance = 0;
        double totalContextPrecision = 0;
        int correctSources = 0;
        long totalLatency = 0;

        for (EvalCase tc : testSet) {
            long start = System.currentTimeMillis();
            String answer = springAi.askWithRag(tc.question(), "evaluation");
            long latency = System.currentTimeMillis() - start;

            List<Document> retrievedDocuments = retrieve(tc.question());
            double faithfulness = computeFaithfulness(answer, tc.expectedAnswer());
            double relevance = computeRelevance(answer, tc.question());
            double contextPrecision = computeContextPrecision(retrievedDocuments, tc.question());
            boolean correctSource = checkSourceAccuracy(retrievedDocuments, tc.expectedSource());

            totalFaithfulness += faithfulness;
            totalRelevance += relevance;
            totalContextPrecision += contextPrecision;
            if (correctSource) {
                correctSources++;
            }
            totalLatency += latency;

            details.add(Map.of(
                "question", tc.question(),
                "answer", answer,
                "faithfulness", round(faithfulness),
                "relevance", round(relevance),
                "correct_source", correctSource,
                "latency_ms", latency
            ));
        }

        int n = testSet.size();
        return Map.of(
            "model", "qwen2.5:0.5b",
            "test_cases", n,
            "results", Map.of(
                "faithfulness", round(totalFaithfulness / n),
                "answer_relevance", round(totalRelevance / n),
                "context_precision", round(totalContextPrecision / n),
                "source_accuracy", round((double) correctSources / n),
                "average_latency_ms", totalLatency / n
            ),
            "details", details
        );
    }

    private List<Document> retrieve(String question) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(question)
            .topK(maxResults)
            .similarityThreshold(minScore)
            .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    private double computeFaithfulness(String answer, String expectedAnswer) {
        String[] expectedWords = expectedAnswer.toLowerCase().split("\\s+");
        long matchCount = 0;
        String lowerAnswer = answer.toLowerCase();
        for (String word : expectedWords) {
            if (word.length() > 3 && lowerAnswer.contains(word)) {
                matchCount++;
            }
        }
        return expectedWords.length > 0 ? (double) matchCount / expectedWords.length : 0.0;
    }

    private double computeRelevance(String answer, String question) {
        String[] questionWords = question.toLowerCase().split("\\s+");
        long matchCount = 0;
        String lowerAnswer = answer.toLowerCase();
        for (String word : questionWords) {
            if (word.length() > 3 && lowerAnswer.contains(word)) {
                matchCount++;
            }
        }
        return questionWords.length > 0 ? Math.min(1.0, (double) matchCount / questionWords.length * 1.5) : 0.0;
    }

    private double computeContextPrecision(List<Document> documents, String question) {
        if (documents.isEmpty()) {
            return 0.0;
        }
        String lowerQuestion = question.toLowerCase();
        long relevantCount = documents.stream()
            .filter(document -> {
                String text = document.getText().toLowerCase();
                String[] questionWords = lowerQuestion.split("\\s+");
                long matches = 0;
                for (String word : questionWords) {
                    if (word.length() > 3 && text.contains(word)) {
                        matches++;
                    }
                }
                return matches >= 2;
            })
            .count();
        return (double) relevantCount / documents.size();
    }

    private boolean checkSourceAccuracy(List<Document> documents, String expectedSource) {
        return documents.stream()
            .anyMatch(document -> {
                Object sourceName = document.getMetadata().get("file_name");
                if (sourceName == null) {
                    sourceName = document.getMetadata().get("source");
                }
                return sourceName != null && sourceName.toString().contains(expectedSource);
            });
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
