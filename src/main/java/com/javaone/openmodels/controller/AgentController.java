package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.ManualAgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private final ManualAgentService manualAgentService;

    public AgentController(ManualAgentService manualAgentService) {
        this.manualAgentService = manualAgentService;
    }

    @GetMapping("/agents/concepts")
    public ManualAgentService.AgentConcepts concepts() {
        return manualAgentService.concepts();
    }

    @PostMapping("/agents/run")
    public ManualAgentService.AgentRun run(@RequestParam String goal) {
        return manualAgentService.run(goal);
    }
}
