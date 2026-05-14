package com.javaone.openmodels.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LangChainConceptService {

    private final ChatLanguageModel chatModel;
    private final Assistant chatAssistant;
    private final Assistant toolAssistant;
    private final MemoryAssistant memoryAssistant;
    private final ContentRetriever contentRetriever;

    public LangChainConceptService(ChatLanguageModel chatModel,
                                   @Qualifier("chatAssistant") Assistant chatAssistant,
                                   @Qualifier("toolAssistant") Assistant toolAssistant,
                                   @Qualifier("memoryAssistant") MemoryAssistant memoryAssistant,
                                   ContentRetriever contentRetriever) {
        this.chatModel = chatModel;
        this.chatAssistant = chatAssistant;
        this.toolAssistant = toolAssistant;
        this.memoryAssistant = memoryAssistant;
        this.contentRetriever = contentRetriever;
    }

    public Map<String, Object> whyLangChain() {
        // [ LangChain kyun use hota hai: repeated GenAI plumbing becomes reusable application components. ]
        // [ Framework productivity: prompt, model, parser, tools, memory, and RAG can be wired declaratively. ]
        // [ LangChain abstraction risk: convenience can hide model behavior, latency, token cost, and version changes. ]
        return Map.of(
            "why_use_langchain", List.of(
                "Models ko application workflows se connect karna easy hota hai.",
                "Prompt templates, chains, tools, memory, RAG, callbacks/streaming jaise concerns standardize hote hain.",
                "Raw HTTP/model calls ke comparison me code kam aur reusable hota hai."
            ),
            "abstraction_risks", List.of(
                "Framework internals samjhe bina debugging mushkil ho sakti hai.",
                "Hidden prompts, tool decisions, retrieval settings, and parser assumptions production behavior badal sakte hain.",
                "Version upgrades me APIs ya defaults change ho sakte hain."
            ),
            "productivity", "Fast prototypes plus production patterns, as long as teams keep observability and tests."
        );
    }

    public Map<String, Object> rawVsLangChain(String message) {
        // [ Raw implementation vs LangChain: raw call directly invokes the model with one prompt string. ]
        String raw = chatModel.chat(message);

        // [ Chains: AiServices wraps model calls behind an application interface. ]
        String langChain = chatAssistant.chat(message);

        return Map.of(
            "raw_model_call", raw,
            "langchain4j_ai_service_call", langChain
        );
    }

    public Map<String, Object> promptTemplateChain(String role, String topic, String question) {
        // [ PromptTemplate: dynamic prompt text is rendered from variables role/topic/question. ]
        PromptTemplate template = PromptTemplate.from("""
            You are a {{role}}.
            Answer about {{topic}} for Java developers.
            Question: {{question}}
            """);

        String renderedPrompt = template.apply(Map.of(
            "role", role,
            "topic", topic,
            "question", question
        )).text();

        // [ Chains / LCEL: this Java chain mirrors prompt | model | parser as prompt -> model -> String output. ]
        String chainAnswer = chatModel.chat(renderedPrompt);

        // [ Output Parsers: AiServices can parse model text into Java collections. ]
        List<String> parsedLearningPoints = chatAssistant.threeLearningPoints(topic);

        return Map.of(
            "rendered_prompt", renderedPrompt,
            "chain", "prompt -> chatModel -> stringParser",
            "answer", chainAnswer,
            "parsed_learning_points", parsedLearningPoints
        );
    }

    public Map<String, Object> toolIntegration(String sku) {
        // [ Tools: the assistant may call InventoryTools.checkStock instead of answering from model memory. ]
        String answer = toolAssistant.chat("Check inventory for SKU " + sku);
        return Map.of("sku", sku, "answer", answer);
    }

    public Map<String, Object> memoryIntegration(String conversationId, String message) {
        // [ Memory integration: same conversation id reuses prior chat history from MessageWindowChatMemory. ]
        String answer = memoryAssistant.chatWithMemory(conversationId, message);
        return Map.of(
            "conversation_id", conversationId,
            "answer", answer
        );
    }

    public Map<String, Object> ragChain(String question) {
        // [ RAG chain: retriever finds relevant document chunks before the LLM answers. ]
        List<Content> contexts = contentRetriever.retrieve(new Query(question));
        String answer = chatAssistant.chat("""
            Answer using the provided context. If context is not enough, say what is missing.

            Context:
            %s

            Question:
            %s
            """.formatted(contexts.stream().map(Content::textSegment).toList(), question));

        return Map.of(
            "question", question,
            "retrieved_chunks", contexts.size(),
            "answer", answer
        );
    }

    public Map<String, Object> internals() {
        // [ LangChain internals: these are the building blocks used in this branch. ]
        return Map.ofEntries(
            Map.entry("LLM / ChatModel", "ChatLanguageModel calls Ollama today; the same role can be OpenAI, Gemini, Claude, or local models."),
            Map.entry("PromptTemplate", "Dynamic prompts use variables like: You are a {role}. Answer about {topic}."),
            Map.entry("Chains / LCEL", "Components are piped as prompt -> model -> parser; LangChain4j expresses this with Java services/builders."),
            Map.entry("Output Parsers", "Model text can become String, JSON-like maps, lists, enums, or typed Java objects."),
            Map.entry("Tools", "External functions such as calculator, database, API, search, or InventoryTools can be invoked by the model."),
            Map.entry("Agents", "An agent lets the model decide which tool to use and when; toolAssistant demonstrates that pattern."),
            Map.entry("Memory / Chat History", "MessageWindowChatMemory keeps previous turns by conversation id."),
            Map.entry("RAG", "loaders -> splitters -> embeddings -> vector DB -> retriever -> LLM."),
            Map.entry("Document Loaders", "FileSystemDocumentLoader is used here; PDF, CSV, web, Notion, and Drive loaders follow the same idea."),
            Map.entry("Vector Stores", "This branch uses InMemoryEmbeddingStore; production options include FAISS, Chroma, Pinecone, Weaviate, Qdrant."),
            Map.entry("Embeddings", "OllamaEmbeddingModel converts text chunks into vectors for similarity search."),
            Map.entry("Callbacks / Streaming", "Callbacks and streaming expose token flow, logs, and monitoring for production observability.")
        );
    }
}
