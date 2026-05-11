package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.Assistant;
import com.javaone.openmodels.service.StreamingChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
public class ChatController {

    private final Assistant chatAssistant;
    private final Assistant toolAssistant;
    private final StreamingChatService streamingChatService;

    public ChatController(@Qualifier("chatAssistant") Assistant chatAssistant,
                          @Qualifier("toolAssistant") Assistant toolAssistant,
                          StreamingChatService streamingChatService) {
        this.chatAssistant = chatAssistant;
        this.toolAssistant = toolAssistant;
        this.streamingChatService = streamingChatService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatAssistant.chat(message);
    }

    @GetMapping("/chat/normal")
    public Map<String, Object> normalChat(@RequestParam String message) {
        long start = System.currentTimeMillis();
        String answer = chatAssistant.chat(message);
        long latency = System.currentTimeMillis() - start;

        return Map.of(
            "answer", answer,
            "streaming", false,
            "latency_ms", latency
        );
    }

    @GetMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam String message) {
        return streamingChatService.stream(message);
    }

    @GetMapping("/chat/tools")
    public String chatWithTools(@RequestParam String message) {
        return toolAssistant.chat(message);
    }
}
