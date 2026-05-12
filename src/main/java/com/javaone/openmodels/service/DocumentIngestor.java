package com.javaone.openmodels.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestor {

    private final VectorStore vectorStore;
    private final int chunkSize;

    public DocumentIngestor(VectorStore vectorStore,
                            @Value("${rag.chunk-size}") int chunkSize) {
        this.vectorStore = vectorStore;
        this.chunkSize = chunkSize;
    }

    public IngestResult ingest(Path documentsDir) {
        List<Document> documents = loadTextDocuments(documentsDir);

        TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(chunkSize)
            .build();

        List<Document> chunks = splitter.apply(documents);
        vectorStore.add(chunks);

        return new IngestResult(documents.size(), chunks.size(), "nomic-embed-text", "simple-in-memory");
    }

    private List<Document> loadTextDocuments(Path documentsDir) {
        try (var paths = Files.list(documentsDir)) {
            List<Document> documents = new ArrayList<>();
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".txt"))
                .forEach(path -> documents.addAll(read(path)));
            return documents;
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read documents from " + documentsDir, ex);
        }
    }

    private List<Document> read(Path path) {
        TextReader reader = new TextReader(new FileSystemResource(path));
        reader.getCustomMetadata().put("file_name", path.getFileName().toString());
        reader.getCustomMetadata().put("source", path.toString());
        return reader.get();
    }

    public record IngestResult(int documentsIngested, int chunksIngested, String embeddingModel, String store) {}
}
