# Testing Steps

Ye file project ke manual testing steps batati hai. Commands project root se run karein:

```text
C:\workfolder\spring_ai\gen-ai-with-open-models
```

## 1. Prerequisites Check

Java version check:

```bash
java -version
```

Expected: Java 21 ya usse upar.

Maven version check:

```bash
mvn -version
```

Ollama running hai ya nahi:

```bash
ollama list
```

Expected models:

```text
qwen2.5:0.5b
nomic-embed-text
```

Agar model missing ho:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

## 2. Build/Test Check

Compile aur tests run karein:

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

## 3. Start Application

Spring Boot app start karein:

```bash
mvn spring-boot:run
```

Expected:

```text
Tomcat started on port 8080
Started OpenModelsDemoApplication
```

Application default URL:

```text
http://localhost:8080
```

## 4. Basic Chat Endpoint Test

Command:

```bash
curl "http://localhost:8080/chat?message=What+is+Java+21?"
```

Expected:

```text
LLM Java 21 ke baare me answer dega.
```

Isse verify hota hai ki:

- Spring Boot app running hai
- Ollama chat model reachable hai
- `ChatController` aur `chatAssistant` working hain

## 5. Tool Calling Endpoint Test

Command:

```bash
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+do+we+have+in+stock?"
```

Expected:

```text
Response me JDK-21 stock ka mention hona chahiye.
```

Example:

```text
JDK-21 has 150 units in stock
```

Isse verify hota hai ki:

- LangChain4j automatic tool calling working hai
- `InventoryTools.checkStock()` call ho raha hai

## 6. RAG Ingest Test

Command:

```bash
curl -X POST "http://localhost:8080/ingest"
```

Expected:

```json
{
  "status": "success",
  "documents_ingested": 3,
  "embedding_model": "nomic-embed-text",
  "store": "InMemoryEmbeddingStore"
}
```

Note: `documents_ingested` number docs folder ke files ke hisaab se change ho sakta hai.

Isse verify hota hai ki:

- Docs read ho rahe hain
- Embeddings generate ho rahe hain
- Vector store me data save ho raha hai

## 7. RAG Ask Test

Ingest ke baad command run karein:

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

Expected:

```text
Answer me virtual threads, lightweight threads, JVM, ya Java 21 ka mention hona chahiye.
```

Isse verify hota hai ki:

- RAG retriever relevant context la raha hai
- `ragAssistant` answer generate kar raha hai

## 8. RAG Evaluation Test

Command:

```bash
curl -X POST "http://localhost:8080/evaluate"
```

Expected response fields:

```text
model
test_cases
results
details
```

`results` ke andar ye metrics aane chahiye:

```text
faithfulness
answer_relevance
context_precision
source_accuracy
average_latency_ms
```

Isse verify hota hai ki:

- `RagEvaluator` working hai
- Test questions execute ho rahe hain
- Basic quality metrics calculate ho rahe hain

## 9. Agent Concepts Endpoint Test

Command:

```bash
curl "http://localhost:8080/agents/concepts"
```

Expected response fields:

```text
agent
llmPlusTools
manualAgentLoop
reasonActObserveLoop
reactPattern
multiStepReasoning
agentStoppingCondition
toolSelection
agentFailureCases
infiniteLoopProblem
```

Isse verify hota hai ki:

- `AgentController` working hai
- `ManualAgentService.concepts()` response return kar raha hai
- Agent theory content available hai

## 10. Manual Agent Run Test

Command:

```bash
curl -X POST "http://localhost:8080/agents/run?goal=Check+stock+for+JDK-21+and+explain+what+the+agent+did"
```

Expected response fields:

```text
goal
status
maxSteps
stepsUsed
stoppingCondition
answer
trace
```

Expected `trace` fields:

```text
step
thought
action
actionInput
observation
rawModelOutput
finalStep
```

Expected behavior:

```text
Agent pehle stock check karne ke liye checkStock tool choose karega.
Observation me JDK-21 stock result aayega.
Phir agent final answer dega.
```

Example observation:

```text
SKU JDK-21: 150 units in stock
```

Isse verify hota hai ki:

- Manual ReAct loop working hai
- LLM next action decide kar raha hai
- Java tool dispatcher tool run kar raha hai
- Observation scratchpad me add ho rahi hai
- Final answer ya stopping condition return ho rahi hai

## 11. Agent Concept Tool Test

Command:

```bash
curl -X POST "http://localhost:8080/agents/run?goal=Explain+the+ReAct+pattern+using+a+tool"
```

Expected:

```text
Trace me explainAgentConcept action dikh sakta hai.
Final answer me Reason -> Act -> Observe explanation honi chahiye.
```

Isse verify hota hai ki:

- Agent inventory ke alawa concept explanation tool bhi choose kar sakta hai
- Tool selection prompt ke basis par ho raha hai

## 12. Agent Failure/Stopping Test

Max steps check karne ke liye `src/main/resources/application.yml` me temporarily set karein:

```yaml
agent:
  max-steps: 1
```

App restart karein:

```bash
mvn spring-boot:run
```

Command:

```bash
curl -X POST "http://localhost:8080/agents/run?goal=Check+stock+for+JDK-21+and+then+explain+manual+agent+loop"
```

Expected:

```text
status failed ho sakta hai agar one step me Final nahi mila.
stoppingCondition me max agent steps reached message aa sakta hai.
```

Test ke baad config wapas set karein:

```yaml
agent:
  max-steps: 5
```

Isse verify hota hai ki:

- Agent infinite loop me nahi jayega
- Max step guard working hai

## 13. Common Issues

### App start nahi ho raha

Check:

```bash
mvn test
```

Aur ensure karein ki port 8080 free ho.

### Ollama connection refused

Ollama start karein:

```bash
ollama serve
```

Ya Windows me Ollama desktop app/system tray check karein.

### First response slow hai

Ye normal hai. First request par Ollama model memory me load hota hai.

### RAG answer empty ya weak hai

Pehle ingest run karein:

```bash
curl -X POST "http://localhost:8080/ingest"
```

### Agent malformed output de raha hai

Small local models kabhi-kabhi exact format miss kar sakte hain. Response me failure trace check karein:

```text
rawModelOutput
stoppingCondition
```

## 14. Quick Happy Path Commands

Full quick test:

```bash
mvn test
mvn spring-boot:run
```

Dusre terminal me:

```bash
curl "http://localhost:8080/chat?message=Hello"
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+do+we+have+in+stock?"
curl -X POST "http://localhost:8080/ingest"
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
curl -X POST "http://localhost:8080/evaluate"
curl "http://localhost:8080/agents/concepts"
curl -X POST "http://localhost:8080/agents/run?goal=Check+stock+for+JDK-21+and+explain+what+the+agent+did"
```

Final expected signal:

```text
Chat works
Tool calling works
RAG ingest works
RAG ask works
RAG evaluation works
Agent concepts work
Manual agent loop works
```
