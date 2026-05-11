package com.javaone.openmodels.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentIngestor {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final String embeddingModelName;
    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentIngestor(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            @Value("${ollama.embedding-model}") String embeddingModelName,
                            @Value("${rag.chunk-size}") int chunkSize,
                            @Value("${rag.chunk-overlap}") int chunkOverlap) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.embeddingModelName = embeddingModelName;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public IngestResult ingest(Path documentsDir) {
        // 1. Load documents
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(documentsDir);

        // 2. Split into chunks and embed
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();

        ingestor.ingest(documents);

        return new IngestResult(documents.size(), embeddingModelName, "in-memory");
    }

    public record IngestResult(int documentsIngested, String embeddingModel, String store) {}
}
