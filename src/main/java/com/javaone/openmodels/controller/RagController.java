package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.DocumentIngestor;
import com.javaone.openmodels.service.RagEvaluator;
import com.javaone.openmodels.service.SpringAiBasicsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Map;

@RestController
public class RagController {

    private final DocumentIngestor documentIngestor;
    private final SpringAiBasicsService springAi;
    private final RagEvaluator ragEvaluator;
    private final String docsDir;

    public RagController(DocumentIngestor documentIngestor,
                         SpringAiBasicsService springAi,
                         RagEvaluator ragEvaluator,
                         @Value("${rag.docs-dir}") String docsDir) {
        this.documentIngestor = documentIngestor;
        this.springAi = springAi;
        this.ragEvaluator = ragEvaluator;
        this.docsDir = docsDir;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        DocumentIngestor.IngestResult result = documentIngestor.ingest(Path.of(docsDir));
        return Map.of(
            "status", "success",
            "documents_ingested", result.documentsIngested(),
            "chunks_ingested", result.chunksIngested(),
            "embedding_model", result.embeddingModel(),
            "store", result.store()
        );
    }

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question,
                                   @RequestParam(defaultValue = "rag") String conversationId) {
        long start = System.currentTimeMillis();
        String answer = springAi.askWithRag(question, conversationId);
        long latency = System.currentTimeMillis() - start;

        return Map.of(
            "answer", answer,
            "model", "qwen2.5:0.5b",
            "latency_ms", latency
        );
    }

    @GetMapping("/retrieve")
    public Map<String, Object> retrieve(@RequestParam String question) {
        return Map.of("matches", springAi.retrieve(question));
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate() {
        return ragEvaluator.evaluate();
    }
}
