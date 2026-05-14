package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.LangChainConceptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LangChainController {

    private final LangChainConceptService conceptService;

    public LangChainController(LangChainConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @GetMapping("/langchain/why")
    public Map<String, Object> whyLangChain() {
        return conceptService.whyLangChain();
    }

    @GetMapping("/langchain/raw-vs-framework")
    public Map<String, Object> rawVsFramework(@RequestParam String message) {
        return conceptService.rawVsLangChain(message);
    }

    @GetMapping("/langchain/prompt-chain")
    public Map<String, Object> promptChain(@RequestParam(defaultValue = "senior Java architect") String role,
                                           @RequestParam(defaultValue = "LangChain") String topic,
                                           @RequestParam String question) {
        return conceptService.promptTemplateChain(role, topic, question);
    }

    @GetMapping("/langchain/tools")
    public Map<String, Object> tools(@RequestParam(defaultValue = "JDK-21") String sku) {
        return conceptService.toolIntegration(sku);
    }

    @PostMapping("/langchain/memory")
    public Map<String, Object> memory(@RequestParam(defaultValue = "demo") String conversationId,
                                      @RequestParam String message) {
        return conceptService.memoryIntegration(conversationId, message);
    }

    @GetMapping("/langchain/rag-chain")
    public Map<String, Object> ragChain(@RequestParam String question) {
        return conceptService.ragChain(question);
    }

    @GetMapping("/langchain/internals")
    public Map<String, Object> internals() {
        return conceptService.internals();
    }
}
