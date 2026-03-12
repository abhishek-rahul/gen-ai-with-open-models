package com.javaone.openmodels.config;

import com.javaone.openmodels.service.Assistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.embedding-model}")
    private String embeddingModelName;

    @Value("${rag.max-results}")
    private int maxResults;

    @Value("${rag.min-score}")
    private double minScore;

    @Bean
    EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(embeddingModelName)
            .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store,
                                      EmbeddingModel model) {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(model)
            .maxResults(maxResults)
            .minScore(minScore)
            .build();
    }

    @Bean
    @Qualifier("ragAssistant")
    Assistant ragAssistant(ChatLanguageModel chatModel,
                           ContentRetriever retriever) {
        return AiServices.builder(Assistant.class)
            .chatLanguageModel(chatModel)
            .contentRetriever(retriever)
            .build();
    }
}
