package com.javaone.openmodels.config;

import com.javaone.openmodels.service.Assistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

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

    @Value("${vector-store.type}")
    private String vectorStoreType;

    @Value("${vector-store.elasticsearch.url}")
    private String elasticsearchUrl;

    @Value("${vector-store.elasticsearch.index-name}")
    private String elasticsearchIndexName;

    @Value("${vector-store.elasticsearch.api-key:}")
    private String elasticsearchApiKey;

    @Value("${vector-store.elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${vector-store.elasticsearch.password:}")
    private String elasticsearchPassword;

    @Value("${vector-store.elasticsearch.num-candidates}")
    private int elasticsearchNumCandidates;

    @Bean
    EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(embeddingModelName)
            .build();
    }

    @Bean(destroyMethod = "close")
    RestClient elasticsearchRestClient() {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticsearchUrl));

        if (StringUtils.hasText(elasticsearchApiKey)) {
            List<Header> headers = List.of(new BasicHeader("Authorization", "ApiKey " + elasticsearchApiKey));
            builder.setDefaultHeaders(headers.toArray(Header[]::new));
        }

        if (StringUtils.hasText(elasticsearchUsername) && StringUtils.hasText(elasticsearchPassword)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword)
            );
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        return builder.build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(RestClient elasticsearchRestClient) {
        if ("in-memory".equalsIgnoreCase(vectorStoreType)) {
            return new InMemoryEmbeddingStore<>();
        }

        return ElasticsearchEmbeddingStore.builder()
            .restClient(elasticsearchRestClient)
            .indexName(elasticsearchIndexName)
            .configuration(ElasticsearchConfigurationKnn.builder()
                .numCandidates(elasticsearchNumCandidates)
                .build())
            .build();
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
    Assistant ragAssistant(ChatModel chatModel,
                           ContentRetriever retriever) {
        return AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .contentRetriever(retriever)
            .build();
    }
}
