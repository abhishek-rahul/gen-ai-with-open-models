# Structured Output Learning Notes

This note summarizes what we added and learned in this chat, starting from the request to create a new branch and implement structured output features.

## Branch

We created a new branch from the current branch:

```bash
develop_structured_feature1
```

All structured output changes were added on this branch.

## Goal

The goal was to make the application return predictable JSON instead of free-form model text.

Features covered:

- JSON mode
- Schema-based output
- DTO mapping
- Output parser
- Validation
- Retry on invalid output
- Strict response format
- Ticket generation JSON
- Entity extraction output

## Why Structured Output?

Normal chat APIs can return text in many formats. That is fine for human reading, but risky for APIs.

Structured output means the model must return JSON in a fixed shape. This makes the response easier to parse, validate, and use in Java code.

## New APIs

Two new APIs were added.

### Ticket Generation

Endpoint:

```http
GET /structured/ticket?message=My payment failed after upgrading to premium plan
```

Expected output shape:

```json
{
  "data": {
    "title": "Payment failed after premium upgrade",
    "description": "User payment failed after upgrading to premium plan.",
    "priority": "HIGH",
    "category": "billing",
    "tags": ["payment", "premium", "billing"]
  },
  "rawJson": "{...}",
  "attempts": 1
}
```

### Entity Extraction

Endpoint:

```http
GET /structured/entities?message=Brian from Oracle discussed Java 21 with the Spring team on Monday
```

Expected output shape:

```json
{
  "data": {
    "people": ["Brian"],
    "organizations": ["Oracle"],
    "products": [],
    "technologies": ["Java 21", "Spring"],
    "dates": ["Monday"],
    "summary": "Brian from Oracle discussed Java 21 with the Spring team on Monday."
  },
  "rawJson": "{...}",
  "attempts": 1
}
```

## Main Files Added

Controller:

```text
src/main/java/com/javaone/openmodels/controller/StructuredOutputController.java
```

Service:

```text
src/main/java/com/javaone/openmodels/service/StructuredOutputService.java
```

DTOs:

```text
src/main/java/com/javaone/openmodels/dto/GeneratedTicket.java
src/main/java/com/javaone/openmodels/dto/ExtractedEntities.java
src/main/java/com/javaone/openmodels/dto/StructuredResponse.java
```

Exception:

```text
src/main/java/com/javaone/openmodels/service/StructuredOutputException.java
```

Config:

```yaml
structured:
  max-retries: 2
```

## Controller Flow

The controller only receives HTTP requests and delegates work to the service.

Ticket API:

```java
@GetMapping("/structured/ticket")
public StructuredResponse<GeneratedTicket> generateTicket(@RequestParam String message) {
    return structuredOutputService.generateTicket(message);
}
```

Entity API:

```java
@GetMapping("/structured/entities")
public StructuredResponse<ExtractedEntities> extractEntities(@RequestParam String message) {
    return structuredOutputService.extractEntities(message);
}
```

The controller does not create prompts, schemas, or validations. That logic lives in the service.

## Ticket API Code Flow

Request:

```http
GET /structured/ticket?message=Login fails after password reset
```

Flow:

```text
HTTP request
-> StructuredOutputController.generateTicket()
-> StructuredOutputService.generateTicket()
-> generate()
-> callJsonMode()
-> chatModel.chat(request)
-> extractJsonObject()
-> parse()
-> validateTicket()
-> return StructuredResponse
```

Why each function is called:

- `generateTicket()` creates the ticket-specific prompt.
- `ticketSchema()` defines the exact JSON structure required from the model.
- `generate()` handles common retry, parsing, and validation logic.
- `callJsonMode()` sends the request to the model using JSON mode and schema.
- `extractJsonObject()` extracts only the JSON object from the model response.
- `parse()` maps raw JSON into `GeneratedTicket`.
- `validateTicket()` checks required fields and valid priority.

## Entity API Code Flow

Request:

```http
GET /structured/entities?message=Alice from Microsoft is testing Java 21 on May 10
```

Flow:

```text
HTTP request
-> StructuredOutputController.extractEntities()
-> StructuredOutputService.extractEntities()
-> generate()
-> callJsonMode()
-> chatModel.chat(request)
-> extractJsonObject()
-> parse()
-> validateEntities()
-> return StructuredResponse
```

Why each function is called:

- `extractEntities()` creates the entity-extraction prompt.
- `entitySchema()` defines the expected JSON fields.
- `generate()` handles shared retry, parser, and validation logic.
- `callJsonMode()` calls the model with strict JSON instructions.
- `extractJsonObject()` keeps only the JSON object.
- `parse()` maps raw JSON into `ExtractedEntities`.
- `validateEntities()` checks lists and summary.

## Why We Use ChatRequest

For simple chat, this is enough:

```java
chatModel.chat("hello")
```

But structured output needs more than a string. We need:

- system message
- user message
- JSON mode
- JSON schema
- strict response format

So we create a `ChatRequest`:

```java
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
```

This is correct because `chatModel.chat(...)` is overloaded. It can accept a simple string or a full `ChatRequest`.

## Strict JSON Prompt

The service uses a system prompt like this:

```text
Return only one valid JSON object. Do not include markdown, comments, or extra text.
```

This reduces the chance of responses like:

```text
Here is your JSON:
```json
{ ... }
```
```

For API usage, we want only:

```json
{ "...": "..." }
```

## DTO Mapping

The JSON response is converted into Java records.

Ticket DTO:

```java
public record GeneratedTicket(
    String title,
    String description,
    String priority,
    String category,
    List<String> tags
) {
}
```

Entity DTO:

```java
public record ExtractedEntities(
    List<String> people,
    List<String> organizations,
    List<String> products,
    List<String> technologies,
    List<String> dates,
    String summary
) {
}
```

This conversion is done by Jackson `ObjectMapper`.

## Validation

Parsing only checks whether JSON can become Java DTO. Validation checks whether the content is useful and correct.

Ticket validation checks:

- `title` is present
- `description` is present
- `priority` is one of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `category` is present
- `tags` has values

Entity validation checks:

- all lists are present
- list values are not blank
- `summary` is present

## Retry on Invalid Output

If parsing or validation fails, the service creates a retry prompt.

Example reason:

```text
Field priority must be one of [LOW, MEDIUM, HIGH, CRITICAL]
```

Then it asks the model again:

```text
The previous response was invalid for this API contract.
Fix it and return only one valid JSON object.
```

Retry count is controlled by:

```yaml
structured:
  max-retries: 2
```

That means:

```text
1 original attempt + 2 retries = 3 total attempts
```

## How To Test

Start Ollama and make sure the model exists:

```powershell
ollama list
```

If needed:

```powershell
ollama pull qwen2.5:0.5b
```

Start the Spring Boot app:

```powershell
mvn spring-boot:run
```

Test ticket API:

```powershell
curl "http://localhost:8080/structured/ticket?message=Login fails after password reset and user cannot access dashboard"
```

Test entity API:

```powershell
curl "http://localhost:8080/structured/entities?message=Alice from Microsoft is testing Java 21 with Spring Boot on May 10"
```

Compile and run tests:

```powershell
mvn clean test
```

## Simple Summary

We added a structured-output layer on top of the existing Ollama chat model.

Instead of trusting free-form model text, the app now:

```text
asks for strict JSON
-> enforces schema
-> parses into Java DTO
-> validates fields
-> retries if invalid
-> returns clean API response
```

This is useful whenever model output needs to be consumed by backend code, UI code, automation, ticketing systems, or downstream APIs.
