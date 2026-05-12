package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.SpringAiBasicsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class ChatController {

    private final SpringAiBasicsService springAi;

    public ChatController(SpringAiBasicsService springAi) {
        this.springAi = springAi;
    }

    @GetMapping("/spring-ai/basics")
    public Map<String, Object> basics() {
        return springAi.basics();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "default") String conversationId) {
        return springAi.chat(message, conversationId);
    }

    @GetMapping("/chat/template")
    public String promptTemplate(@RequestParam(defaultValue = "Spring AI PromptTemplate") String topic,
                                 @RequestParam(defaultValue = "developers new to Spring AI") String audience) {
        return springAi.template(topic, audience);
    }

    @GetMapping("/chat/tools")
    public String chatWithTools(@RequestParam String message,
                                @RequestParam(defaultValue = "default") String conversationId) {
        return springAi.chatWithTools(message, conversationId);
    }

    @GetMapping("/chat/structured")
    public SpringAiBasicsService.AnswerSummary structured(@RequestParam String message) {
        return springAi.structured(message);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message,
                               @RequestParam(defaultValue = "default") String conversationId) {
        return springAi.stream(message, conversationId);
    }
}
