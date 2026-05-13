package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.Assistant;
import com.javaone.openmodels.service.ToolAuditLog;
import com.javaone.openmodels.service.ToolCallingAssistant;
import com.javaone.openmodels.service.ToolCatalog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatController {

    private final Assistant chatAssistant;
    private final ToolCallingAssistant toolAssistant;
    private final ToolCatalog toolCatalog;
    private final ToolAuditLog toolAuditLog;

    public ChatController(@Qualifier("chatAssistant") Assistant chatAssistant,
                          @Qualifier("toolAssistant") ToolCallingAssistant toolAssistant,
                          ToolCatalog toolCatalog,
                          ToolAuditLog toolAuditLog) {
        this.chatAssistant = chatAssistant;
        this.toolAssistant = toolAssistant;
        this.toolCatalog = toolCatalog;
        this.toolAuditLog = toolAuditLog;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatAssistant.chat(message);
    }

    @GetMapping("/chat/tools")
    public String chatWithTools(@RequestParam String message) {
        return toolAssistant.chat(message);
    }

    @GetMapping("/tools/schema")
    public List<ToolCatalog.ToolSchema> toolSchemas() {
        return toolCatalog.schemas();
    }

    @GetMapping("/tools/audit")
    public List<ToolAuditLog.ToolAuditEntry> toolAuditLog() {
        return toolAuditLog.entries();
    }

    @DeleteMapping("/tools/audit")
    public void clearToolAuditLog() {
        toolAuditLog.clear();
    }
}
