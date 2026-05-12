package com.javaone.openmodels.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpringAiBasicsService {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final InventoryTools inventoryTools;
    private final int maxResults;
    private final double minScore;

    public SpringAiBasicsService(ChatClient chatClient,
                                 ChatModel chatModel,
                                 EmbeddingModel embeddingModel,
                                 VectorStore vectorStore,
                                 InventoryTools inventoryTools,
                                 @Value("${rag.max-results}") int maxResults,
                                 @Value("${rag.min-score}") double minScore) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.inventoryTools = inventoryTools;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    public Map<String, Object> basics() {
        Map<String, Object> basics = new LinkedHashMap<>();
        basics.put("spring_ai", "Spring AI is Spring's portable abstraction layer for chat models, embedding models, vector stores, advisors, tool calling, memory, structured output, and streaming.");
        basics.put("chat_client", "Fluent high-level API used by /chat, /chat/template, /chat/tools, /chat/structured, and /chat/stream.");
        basics.put("prompt_template", "PromptTemplate fills runtime variables before calling the ChatModel.");
        basics.put("advisors", "MessageChatMemoryAdvisor adds conversation history; QuestionAnswerAdvisor adds retrieved VectorStore context.");
        basics.put("vector_store", vectorStore.getName());
        basics.put("embedding_model", embeddingModel.getClass().getSimpleName());
        basics.put("chat_model", chatModel.getClass().getSimpleName());
        basics.put("tool_calling", "InventoryTools.checkStock is exposed with @Tool.");
        basics.put("chat_memory", "MessageWindowChatMemory is enabled through ChatMemory.CONVERSATION_ID.");
        basics.put("structured_output", "ChatClient.call().entity(AnswerSummary.class) maps model output to a Java record.");
        basics.put("streaming", "ChatClient.stream().content() returns Flux<String>.");
        return basics;
    }

    public String chat(String message, String conversationId) {
        return chatClient.prompt()
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
            .user(message)
            .call()
            .content();
    }

    public String template(String topic, String audience) {
        PromptTemplate promptTemplate = PromptTemplate.builder()
            .template("""
                Explain {topic} for {audience}.
                Keep it practical for Java developers and include one tiny example.
                """)
            .variables(Map.of("topic", topic, "audience", audience))
            .build();
        Prompt prompt = promptTemplate.create();
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    public String chatWithTools(String message, String conversationId) {
        return chatClient.prompt()
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
            .tools(inventoryTools)
            .user(message)
            .call()
            .content();
    }

    public AnswerSummary structured(String message) {
        return chatClient.prompt()
            .user(user -> user
                .text("""
                    Summarize this request for a Java team:
                    {message}
                    """)
                .param("message", message))
            .call()
            .entity(AnswerSummary.class);
    }

    public Flux<String> stream(String message, String conversationId) {
        return chatClient.prompt()
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
            .user(message)
            .stream()
            .content();
    }

    public String askWithRag(String question, String conversationId) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(question)
            .topK(maxResults)
            .similarityThreshold(minScore)
            .build();

        return chatClient.prompt()
            .advisors(
                advisor -> advisor
                    .param(ChatMemory.CONVERSATION_ID, conversationId)
                    .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(searchRequest)
                        .build())
            )
            .user(question)
            .call()
            .content();
    }

    public List<Map<String, Object>> retrieve(String question) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(question)
            .topK(maxResults)
            .similarityThreshold(minScore)
            .build();
        return vectorStore.similaritySearch(searchRequest).stream()
            .map(document -> Map.<String, Object>of(
                "text", document.getText(),
                "metadata", document.getMetadata(),
                "score", document.getScore() == null ? 0.0 : document.getScore()
            ))
            .toList();
    }

    public record AnswerSummary(String intent, List<String> keyTopics, String suggestedNextStep) {}
}
