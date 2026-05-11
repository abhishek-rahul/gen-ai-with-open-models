package com.javaone.openmodels.controller;

import com.javaone.openmodels.dto.ExtractedEntities;
import com.javaone.openmodels.dto.GeneratedTicket;
import com.javaone.openmodels.dto.StructuredResponse;
import com.javaone.openmodels.service.StructuredOutputService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StructuredOutputController {

    private final StructuredOutputService structuredOutputService;

    public StructuredOutputController(StructuredOutputService structuredOutputService) {
        this.structuredOutputService = structuredOutputService;
    }

    @GetMapping("/structured/ticket")
    public StructuredResponse<GeneratedTicket> generateTicket(@RequestParam String message) {
        return structuredOutputService.generateTicket(message);
    }

    @GetMapping("/structured/entities")
    public StructuredResponse<ExtractedEntities> extractEntities(@RequestParam String message) {
        return structuredOutputService.extractEntities(message);
    }
}
