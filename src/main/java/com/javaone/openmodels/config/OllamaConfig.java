package com.javaone.openmodels.config;

import com.javaone.openmodels.service.Assistant;
import com.javaone.openmodels.service.InventoryTools;
import com.javaone.openmodels.service.MemoryAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.chat-model}")
    private String chatModelName;

    @Bean
    ChatLanguageModel chatModel() {
        // [ LLM / ChatModel: this is the model adapter; swap Ollama with OpenAI, Gemini, Claude, or another provider here. ]
        // [ Callbacks / Streaming: production apps can add listeners/streaming models around this layer for logs and token streaming. ]
        return OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(chatModelName)
            .temperature(0.7)
            .timeout(Duration.ofSeconds(120))
            .build();
    }

    @Bean
    @Qualifier("chatAssistant")
    Assistant chatAssistant(ChatLanguageModel chatModel) {
        // [ Framework productivity: AiServices removes manual request/response plumbing for common model calls. ]
        return AiServices.builder(Assistant.class)
            .chatLanguageModel(chatModel)
            .build();
    }

    @Bean
    @Qualifier("toolAssistant")
    Assistant toolAssistant(ChatLanguageModel chatModel) {
        // [ Agents: LangChain4j lets the model decide when the declared tool is useful for a user request. ]
        // [ Tool integration: InventoryTools exposes Java methods as callable LLM tools. ]
        return AiServices.builder(Assistant.class)
            .chatLanguageModel(chatModel)
            .tools(new InventoryTools())
            .build();
    }

    @Bean
    @Qualifier("memoryAssistant")
    MemoryAssistant memoryAssistant(ChatLanguageModel chatModel) {
        // [ Memory / Chat History: each memory id gets a rolling window of previous messages. ]
        return AiServices.builder(MemoryAssistant.class)
            .chatLanguageModel(chatModel)
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .build())
            .build();
    }
}
