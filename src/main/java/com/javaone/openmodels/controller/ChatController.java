package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.Assistant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final Assistant chatAssistant;
    private final Assistant toolAssistant;

    public ChatController(@Qualifier("chatAssistant") Assistant chatAssistant,
                          @Qualifier("toolAssistant") Assistant toolAssistant) {
        this.chatAssistant = chatAssistant;
        this.toolAssistant = toolAssistant;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatAssistant.chat(message);
    }

    @GetMapping("/chat/tools")
    public String chatWithTools(@RequestParam String message) {
        return toolAssistant.chat(message);
    }
}
