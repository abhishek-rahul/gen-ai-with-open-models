package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.LangGraphWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LangGraphController {

    private final LangGraphWorkflowService langGraphWorkflowService;

    public LangGraphController(LangGraphWorkflowService langGraphWorkflowService) {
        this.langGraphWorkflowService = langGraphWorkflowService;
    }

    @GetMapping("/langgraph/run")
    public Map<String, Object> run(@RequestParam String message,
                                   @RequestParam(defaultValue = "demo") String sessionId,
                                   @RequestParam(defaultValue = "false") boolean approved) {
        // [ 2. Graph-based workflow ] HTTP request ek graph execution trigger karta hai, single LLM call nahi.
        // [ 10. LangChain Agent vs LangGraph Workflow ] LangChain agent tools autonomously choose karta hai; yahan route graph control karta hai.
        return langGraphWorkflowService.run(sessionId, message, approved);
    }

    @GetMapping("/langgraph/state")
    public Map<String, Object> state(@RequestParam(defaultValue = "demo") String sessionId) {
        // [ 19. Checkpointer / Persistence ] Saved conversation/graph state ko inspect karne ka endpoint.
        return langGraphWorkflowService.state(sessionId);
    }
}
