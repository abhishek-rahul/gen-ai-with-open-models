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
        // [ 19. Embeddings: text chunks are converted into vectors for similarity search. ]
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(embeddingModelName)
            .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        // [ 18. Vector Stores: in-memory is demo-friendly; FAISS, Chroma, Pinecone, Weaviate, and Qdrant fit this role in production. ]
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store,
                                      EmbeddingModel model) {
        // [ 16. RAG: retriever searches the vector store and sends the best matching chunks to the LLM. ]
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
        // [ 5. RAG chain: AiServices wires retriever -> prompt augmentation -> ChatModel answer. ]
        return AiServices.builder(Assistant.class)
            .chatLanguageModel(chatModel)
            .contentRetriever(retriever)
            .build();
    }
}
