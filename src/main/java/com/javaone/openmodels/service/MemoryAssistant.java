package com.javaone.openmodels.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface MemoryAssistant {

    // [ 15. Memory / Chat History: the memory id keeps previous turns for the same conversation. ]
    @SystemMessage("You are a concise assistant. Use previous messages when they are relevant.")
    String chatWithMemory(@MemoryId String conversationId, @UserMessage String message);
}
