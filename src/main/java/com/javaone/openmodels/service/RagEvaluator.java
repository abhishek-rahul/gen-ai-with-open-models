package com.javaone.openmodels.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
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

    private final Assistant ragAssistant;
    private final ContentRetriever contentRetriever;

    public RagEvaluator(
            @org.springframework.beans.factory.annotation.Qualifier("ragAssistant") Assistant ragAssistant,
            ContentRetriever contentRetriever) {
        this.ragAssistant = ragAssistant;
        this.contentRetriever = contentRetriever;
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
            // [ 5. RAG chain: the assistant answers with context retrieved from the vector store. ]
            String answer = ragAssistant.chat(tc.question());
            long latency = System.currentTimeMillis() - start;

            // [ 8. LangChain internals: retrieve() exposes what the RAG layer selected before the LLM call. ]
            List<Content> retrievedContents = contentRetriever.retrieve(new Query(tc.question()));

            // [ 22. Framework productivity: evaluation can sit beside the chain and catch retrieval/answer regressions. ]
            double faithfulness = computeFaithfulness(answer, tc.expectedAnswer());

            // [ 12. Output Parsers: this demo returns a Map so API clients get structured evaluation output. ]
            double relevance = computeRelevance(answer, tc.question());

            // [ 16. RAG: context precision checks whether retrieved chunks are actually relevant. ]
            double contextPrecision = computeContextPrecision(retrievedContents, tc.question());

            // [ 17. Document Loaders: source metadata from loaded files helps validate citation/source accuracy. ]
            boolean correctSource = checkSourceAccuracy(retrievedContents, tc.expectedSource());

            totalFaithfulness += faithfulness;
            totalRelevance += relevance;
            totalContextPrecision += contextPrecision;
            if (correctSource) correctSources++;
            totalLatency += latency;

            details.add(Map.of(
                "question", tc.question(),
                "answer", answer,
                "faithfulness", Math.round(faithfulness * 100.0) / 100.0,
                "relevance", Math.round(relevance * 100.0) / 100.0,
                "correct_source", correctSource,
                "latency_ms", latency
            ));
        }

        int n = testSet.size();
        return Map.of(
            "model", "qwen2.5:0.5b",
            "test_cases", n,
            "results", Map.of(
                "faithfulness", Math.round(totalFaithfulness / n * 100.0) / 100.0,
                "answer_relevance", Math.round(totalRelevance / n * 100.0) / 100.0,
                "context_precision", Math.round(totalContextPrecision / n * 100.0) / 100.0,
                "source_accuracy", Math.round((double) correctSources / n * 100.0) / 100.0,
                "average_latency_ms", totalLatency / n
            ),
            "details", details
        );
    }

    private double computeFaithfulness(String answer, String expectedAnswer) {
        // Simple keyword overlap metric
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
        // Simple: check if answer contains key nouns from the question
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

    private double computeContextPrecision(List<Content> contents, String question) {
        if (contents.isEmpty()) return 0.0;
        String lowerQuestion = question.toLowerCase();
        long relevantCount = contents.stream()
            .filter(c -> {
                String text = c.textSegment().text().toLowerCase();
                String[] questionWords = lowerQuestion.split("\\s+");
                long matches = 0;
                for (String w : questionWords) {
                    if (w.length() > 3 && text.contains(w)) matches++;
                }
                return matches >= 2;
            })
            .count();
        return (double) relevantCount / contents.size();
    }

    private boolean checkSourceAccuracy(List<Content> contents, String expectedSource) {
        return contents.stream()
            .anyMatch(c -> {
                String sourceName = c.textSegment().metadata().getString("file_name");
                if (sourceName == null) {
                    sourceName = c.textSegment().metadata().getString("source");
                }
                return sourceName != null && sourceName.contains(expectedSource);
            });
    }
}
