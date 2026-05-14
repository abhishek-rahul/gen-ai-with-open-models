package com.javaone.openmodels.controller;

import com.javaone.openmodels.service.Assistant;
import com.javaone.openmodels.service.DocumentIngestor;
import com.javaone.openmodels.service.RagEvaluator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
public class RagController {

    private final DocumentIngestor documentIngestor;
    private final Assistant ragAssistant;
    private final RagEvaluator ragEvaluator;
    private final String docsDir;

    public RagController(DocumentIngestor documentIngestor,
                         @Qualifier("ragAssistant") Assistant ragAssistant,
                         RagEvaluator ragEvaluator,
                         @Value("${rag.docs-dir}") String docsDir) {
        this.documentIngestor = documentIngestor;
        this.ragAssistant = ragAssistant;
        this.ragEvaluator = ragEvaluator;
        this.docsDir = docsDir;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        // [ RAG: this endpoint prepares the vector database before question answering. ]
        DocumentIngestor.IngestResult result = documentIngestor.ingest(Path.of(docsDir));
        return Map.of(
            "status", "success",
            "documents_ingested", result.documentsIngested(),
            "embedding_model", result.embeddingModel(),
            "store", result.store()
        );
    }

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question) {
        long start = System.currentTimeMillis();
        // [ RAG chain: question -> retriever -> context augmented prompt -> LLM answer. ]
        String answer = ragAssistant.chat(question);
        long latency = System.currentTimeMillis() - start;

        return Map.of(
            "answer", answer,
            "model", "qwen2.5:0.5b",
            "latency_ms", latency
        );
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluate() {
        // [ LangChain abstraction risk: evaluation makes hidden retrieval/model behavior visible. ]
        return ragEvaluator.evaluate();
    }
}
