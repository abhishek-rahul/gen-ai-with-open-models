package com.javaone.openmodels.config;

import com.javaone.openmodels.service.Assistant;
import com.javaone.openmodels.service.InventoryTools;
import dev.langchain4j.model.chat.ChatModel;
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
    ChatModel chatModel() {
        return OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(chatModelName)
            .temperature(0.7)
            .timeout(Duration.ofSeconds(120))
            .build();
    }

    @Bean
    @Qualifier("chatAssistant")
    Assistant chatAssistant(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .build();
    }

    @Bean
    @Qualifier("toolAssistant")
    Assistant toolAssistant(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .tools(new InventoryTools())
            .build();
    }
}
