# Demo 1 Script: Local Inference with Ollama + LangChain4j

## Overview

This demo shows how to run an open model (Llama 3.1 8B) locally using Ollama and call it from a Java Spring Boot application via LangChain4j. You'll demonstrate chat interaction, tool calling, streaming, and a model swap to Mistral.

**Duration:** ~5 minutes  
**Requirements:** Ollama installed, Java 21+, Maven  
**No API keys needed — everything runs locally**

---

## Pre-Demo Setup (Do Before the Session)

### 1. Install Ollama

```bash
# macOS / Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows — download from ollama.com
```

- [Ollama installation guide](https://ollama.com/download)

### 2. Pull Models (Do This Ahead of Time — Downloads Are Large)

```bash
ollama pull qwen2.5:0.5b        # ~4.7 GB (Q4 quantized)
ollama pull mistral:7b         # ~4.1 GB
ollama pull nomic-embed-text   # ~274 MB (embedding model for Demo 2)
```

### 3. Verify Ollama Is Running

```bash
ollama list
# Should show: qwen2.5:0.5b, mistral:7b, nomic-embed-text

curl http://localhost:11434/api/tags | python3 -m json.tool
# Should return JSON with model list
```

### 4. Clone / Set Up the Demo Project

```bash
git clone https://github.com/bbenz/javaone-open-models-demo.git
cd javaone-open-models-demo

# Or create a minimal Spring Boot project:
# spring init --dependencies=web --java-version=21 --build=maven open-models-demo
```

### 5. Add LangChain4j Dependencies (pom.xml)

```xml
<dependencies>
    <!-- LangChain4j core -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>1.0.0-beta1</version>
    </dependency>
    <!-- Ollama integration -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-ollama</artifactId>
        <version>1.0.0-beta1</version>
    </dependency>
    <!-- Spring Boot starter (if using Spring) -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>1.0.0-beta1</version>
    </dependency>
</dependencies>
```

### 6. Build the Project

```bash
mvn clean package -DskipTests
```

### 7. Test Run

```bash
mvn spring-boot:run
# Hit http://localhost:8080/chat?message=Hello
```

---

## Live Demo Script

### Step 1: Show Ollama Running (30 seconds)

> "Let me start with what's running on my machine."

```bash
ollama list
```

**Expected output:**

```
NAME                   ID             SIZE     MODIFIED
qwen2.5:0.5b           <hash>         4.7 GB   2 hours ago
mistral:7b             <hash>         4.1 GB   2 hours ago
nomic-embed-text       <hash>         274 MB   2 hours ago
```

> "Three models pulled locally. Llama 3.1 8B from Meta — quantized to Q4, so it's under 5 gigs. Mistral 7B as an alternative. And an embedding model for RAG, which we'll use in Demo 2."

Show memory usage:

```bash
ollama ps
```

> "No models loaded yet — Ollama loads them on first request and keeps them resident. Let's see how much memory our 8B model actually uses."

### Step 2: Show the Java Code (60 seconds)

Open VS Code. Show the key files:

**OllamaConfig.java** — Model configuration:

```java
@Configuration
public class OllamaConfig {

    @Bean
    ChatLanguageModel chatModel() {
        return OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("llama3.1:8b")
            .temperature(0.7)
            .timeout(Duration.ofSeconds(120))
            .build();
    }
}
```

> "Standard LangChain4j setup. Point it at localhost:11434 — that's Ollama's default port. Specify the model name. This is the same model I just showed you in the terminal."

**ChatController.java** — REST endpoint:

```java
@RestController
public class ChatController {

    private final Assistant assistant;

    public ChatController(Assistant assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return assistant.chat(message);
    }
}
```

**Assistant.java** — AI Service interface:

```java
public interface Assistant {
    @SystemMessage("You are a helpful assistant for a Java development team.")
    String chat(@UserMessage String message);
}
```

> "Look at the interface — plain Java. The annotation `@SystemMessage` sets the system prompt. LangChain4j generates the implementation at runtime using the model we configured."

### Step 3: Run the Application (30 seconds)

```bash
mvn spring-boot:run
```

> "Spring Boot starting up. It will connect to Ollama on first request."

### Step 4: First Chat Interaction (60 seconds)

```bash
curl -s "http://localhost:8080/chat?message=What+is+the+Java+record+keyword+and+when+should+I+use+it?" | fold -s -w 80
```

> "Watch the terminal — this is the first request, so Ollama is loading the model into memory."

**Expected output** (paraphrased):

```
Java records, introduced in Java 14, are a concise way to declare classes that
are primarily data carriers. They automatically generate constructors, getters,
equals(), hashCode(), and toString()...

Use records when:
- You need immutable data holders (DTOs, value objects)
- You want less boilerplate than traditional POJOs
...
```

> "That's Llama 3.1 8B running entirely on this machine. No API key, no network call to a cloud service. Let me show you the response time and memory."

Show Ollama memory:

```bash
ollama ps
```

**Expected output:**

```
NAME            ID       SIZE     PROCESSOR    UNTIL
llama3.1:8b     <hash>   6.7 GB   100% GPU    4 minutes from now
```

> "6.7 gigs resident — the Q4 quantized weights plus KV cache. Running on GPU. On a CPU-only machine this would still work, just a bit slower."

### Step 5: Show Tool Calling (60 seconds)

Open **InventoryTools.java**:

```java
public class InventoryTools {

    @Tool("Look up current stock level for a product by SKU")
    public String checkStock(@P("Product SKU") String sku) {
        // Simulated inventory lookup
        Map<String, Integer> inventory = Map.of(
            "JDK-21", 150, "MVN-4", 89, "GRAAL-22", 12
        );
        Integer qty = inventory.get(sku);
        return qty != null
            ? "SKU %s: %d units in stock".formatted(sku, qty)
            : "SKU %s: not found in inventory".formatted(sku);
    }
}
```

> "This is a plain Java method annotated with `@Tool`. LangChain4j tells the model about this tool. The model decides when to call it."

```bash
curl -s "http://localhost:8080/chat?message=How+many+units+of+JDK-21+do+we+have+in+stock?" | fold -s -w 80
```

**Expected output:**

```
Let me check the inventory for you. According to our system, SKU JDK-21 has
150 units currently in stock.
```

> "The model received the question, decided it needed to call `checkStock`, passed 'JDK-21' as the SKU, got back '150 units', and formulated the response. That's an agent loop — reason, act, observe, respond."

### Step 6: Swap to Mistral (60 seconds)

> "Now watch this — same code, different model."

Change `application.yml` or show the config change:

```yaml
# application.yml
langchain4j:
  ollama:
    chat-model:
      model-name: mistral:7b   # ← Changed from llama3.1:8b
```

Restart the app (or if using `@RefreshScope`, hit the refresh endpoint):

```bash
# Restart
mvn spring-boot:run
```

Run the same query:

```bash
curl -s "http://localhost:8080/chat?message=What+is+the+Java+record+keyword+and+when+should+I+use+it?" | fold -s -w 80
```

> "Same question, same code, different model. Notice the response style is slightly different — Mistral tends to be more concise. Same `@Tool` annotations, same `Assistant` interface. The model is a configuration choice, not a code change.
>
> In production, this means you can A/B test models, roll back to a previous version, or switch providers — all through configuration."

### Step 7: Recap (30 seconds)

> "Let me recap what we just did:
> - Ran an 8-billion parameter model locally — no cloud, no API keys
> - Called it from a standard Spring Boot app via LangChain4j
> - Tool calling worked out of the box — the model invoked Java methods
> - Swapped models with a config change — zero code changes
> - Memory footprint was under 7 GB — fits on a standard dev laptop
>
> This is your development inner loop. Edit code, restart, test with a real model. When you're ready for production, point the same code at Azure."

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `ollama: command not found` | Install from ollama.com/download |
| Model pull fails / hangs | Check disk space; models are 4–5 GB each |
| `Connection refused` on :11434 | Start Ollama: `ollama serve` (macOS) or check the system tray (Windows) |
| Slow first response (30+ sec) | Model is loading into memory — subsequent requests will be fast |
| `OutOfMemoryError` in Java | Increase JVM heap: `-Xmx2g`; model memory is separate from JVM heap |
| Tool calling not working | Ensure model supports function calling (Llama 3.1 and Mistral do) |
| Timeout on complex queries | Increase `timeout` in `OllamaChatModel.builder()` |

---

## Links

- [Ollama — ollama.com](https://ollama.com)
- [LangChain4j — langchain4j.dev](https://langchain4j.dev)
- [LangChain4j Ollama integration](https://docs.langchain4j.dev/integrations/language-models/ollama)
- [LangChain4j Spring Boot starter](https://docs.langchain4j.dev/tutorials/spring-boot-integration)
- [Llama 3.1 model card](https://ollama.com/library/llama3.1)
- [Mistral 7B model card](https://ollama.com/library/mistral)
- [LangChain4j tools documentation](https://docs.langchain4j.dev/tutorials/tools)
- [Java 21 records](https://docs.oracle.com/en/java/javase/21/language/records.html)
