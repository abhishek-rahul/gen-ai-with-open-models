package com.javaone.openmodels.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaone.openmodels.dto.ExtractedEntities;
import com.javaone.openmodels.dto.GeneratedTicket;
import com.javaone.openmodels.dto.StructuredResponse;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class StructuredOutputService {

    private static final String STRICT_JSON_SYSTEM_PROMPT = """
        You generate strict structured JSON for a Java API.
        Return only one valid JSON object. Do not include markdown, comments, or extra text.
        Follow the supplied schema exactly and do not add extra properties.
        """;

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;
    private final int maxRetries;

    public StructuredOutputService(ChatLanguageModel chatModel,
                                   ObjectMapper objectMapper,
                                   @Value("${structured.max-retries:2}") int maxRetries) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
    }

    public StructuredResponse<GeneratedTicket> generateTicket(String message) {
        return generate(
            """
                Create a support ticket from the user message.
                Required JSON fields:
                title: short actionable title.
                description: clear problem statement.
                priority: one of LOW, MEDIUM, HIGH, CRITICAL.
                category: short category such as bug, feature, infra, data, support.
                tags: 2 to 5 lowercase tags.

                User message:
                %s
                """.formatted(message),
            GeneratedTicket.class,
            ticketSchema(),
            this::validateTicket
        );
    }

    public StructuredResponse<ExtractedEntities> extractEntities(String message) {
        return generate(
            """
                Extract named entities from the user message.
                Use empty arrays when a category has no entities.
                The summary must be one concise sentence.

                User message:
                %s
                """.formatted(message),
            ExtractedEntities.class,
            entitySchema(),
            this::validateEntities
        );
    }

    private <T> StructuredResponse<T> generate(String prompt,
                                               Class<T> type,
                                               JsonSchema schema,
                                               Consumer<T> validator) {
        String currentPrompt = prompt;
        String lastRawJson = null;
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                String rawJson = callJsonMode(currentPrompt, schema);
                lastRawJson = rawJson;

                T parsed = parse(rawJson, type);
                validator.accept(parsed);

                return new StructuredResponse<>(parsed, rawJson, attempt);
            } catch (Exception ex) {
                lastFailure = ex;
                currentPrompt = retryPrompt(prompt, lastRawJson, ex.getMessage());
            }
        }

        throw new StructuredOutputException(
            "Model did not produce valid structured output after " + (maxRetries + 1) + " attempts",
            lastFailure
        );
    }

    private String callJsonMode(String prompt, JsonSchema schema) {
        ChatRequest request = ChatRequest.builder()
            .messages(
                SystemMessage.from(STRICT_JSON_SYSTEM_PROMPT),
                UserMessage.from(prompt)
            )
            .parameters(ChatRequestParameters.builder()
                .responseFormat(ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .jsonSchema(schema)
                    .build())
                .build())
            .build();

        ChatResponse response = chatModel.chat(request);
        String text = response.aiMessage().text();
        if (text == null || text.isBlank()) {
            throw new StructuredOutputException("Model returned an empty response");
        }
        return extractJsonObject(text);
    }

    private <T> T parse(String rawJson, Class<T> type) {
        try {
            return objectMapper.readValue(rawJson, type);
        } catch (JsonProcessingException ex) {
            throw new StructuredOutputException("Output parser failed: " + ex.getOriginalMessage(), ex);
        }
    }

    private String extractJsonObject(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new StructuredOutputException("Response did not contain a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private String retryPrompt(String originalPrompt, String invalidJson, String validationError) {
        return """
            The previous response was invalid for this API contract.
            Fix it and return only one valid JSON object.

            Validation error:
            %s

            Invalid response:
            %s

            Original task:
            %s
            """.formatted(validationError, invalidJson == null ? "<empty>" : invalidJson, originalPrompt);
    }

    private void validateTicket(GeneratedTicket ticket) {
        requireText(ticket.title(), "title");
        requireText(ticket.description(), "description");
        requireOneOf(ticket.priority(), "priority", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        requireText(ticket.category(), "category");
        requireList(ticket.tags(), "tags");
    }

    private void validateEntities(ExtractedEntities entities) {
        requireListOrEmpty(entities.people(), "people");
        requireListOrEmpty(entities.organizations(), "organizations");
        requireListOrEmpty(entities.products(), "products");
        requireListOrEmpty(entities.technologies(), "technologies");
        requireListOrEmpty(entities.dates(), "dates");
        requireText(entities.summary(), "summary");
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new StructuredOutputException("Missing required field: " + field);
        }
    }

    private void requireList(List<String> values, String field) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new StructuredOutputException("Missing required list values: " + field);
        }
    }

    private void requireListOrEmpty(List<String> values, String field) {
        if (values == null || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new StructuredOutputException("Invalid list values: " + field);
        }
    }

    private void requireOneOf(String value, String field, List<String> allowedValues) {
        requireText(value, field);
        if (!allowedValues.contains(value)) {
            throw new StructuredOutputException("Field %s must be one of %s".formatted(field, allowedValues));
        }
    }

    private JsonSchema ticketSchema() {
        JsonObjectSchema root = JsonObjectSchema.builder()
            .description("Generated support ticket")
            .addStringProperty("title", "Short actionable title")
            .addStringProperty("description", "Clear problem statement")
            .addEnumProperty("priority", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"), "Ticket priority")
            .addStringProperty("category", "Ticket category")
            .addProperty("tags", stringArray("Lowercase ticket tags"))
            .required("title", "description", "priority", "category", "tags")
            .additionalProperties(false)
            .build();

        return JsonSchema.builder()
            .name("generated_ticket")
            .rootElement(root)
            .build();
    }

    private JsonSchema entitySchema() {
        JsonObjectSchema root = JsonObjectSchema.builder()
            .description("Entity extraction result")
            .addProperty("people", stringArray("Person names"))
            .addProperty("organizations", stringArray("Organization names"))
            .addProperty("products", stringArray("Product names"))
            .addProperty("technologies", stringArray("Technology names"))
            .addProperty("dates", stringArray("Dates or time expressions"))
            .addStringProperty("summary", "One sentence summary")
            .required("people", "organizations", "products", "technologies", "dates", "summary")
            .additionalProperties(false)
            .build();

        return JsonSchema.builder()
            .name("extracted_entities")
            .rootElement(root)
            .build();
    }

    private JsonSchemaElement stringArray(String description) {
        return JsonArraySchema.builder()
            .description(description)
            .items(JsonStringSchema.builder().build())
            .build();
    }
}
