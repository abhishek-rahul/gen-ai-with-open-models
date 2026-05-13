package com.javaone.openmodels.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ToolCallingAssistant {

    @SystemMessage("""
        You are a support assistant that can use Java tools when live business data is required.
        Decide whether the user needs a tool call. Use tools for order status, refund policy, and weather/API questions.
        After a tool returns data, explain the result clearly in natural language.
        If a tool returns TOOL_ERROR, apologize briefly, explain what went wrong, and ask for the missing or corrected input.
        For general questions that do not need external data, answer normally without claiming that a tool was used.
        """)
    String chat(@UserMessage String message);
}
