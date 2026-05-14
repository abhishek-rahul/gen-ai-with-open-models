# Testing Steps

Ye file `develop_langchain_feature1` branch ke LangChain changes test karne ke liye hai.

## 1. Prerequisites Check

Java version check karo:

```bash
java -version
```

Expected:

```text
Java 21 ya higher
```

Maven version check karo:

```bash
mvn -version
```

Ollama running hai ya nahi check karo:

```bash
ollama list
```

Required models available hone chahiye:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

## 2. Branch Check

Current branch verify karo:

```bash
git status --short --branch
```

Expected:

```text
## develop_langchain_feature1
```

## 3. Build Test

Compile and test run karo:

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

Package test:

```bash
mvn -DskipTests package
```

Expected:

```text
BUILD SUCCESS
```

## 4. Application Start

Spring Boot app start karo:

```bash
mvn spring-boot:run
```

Ya packaged jar se:

```bash
java -jar target/open-models-demo-1.0.0.jar
```

Expected:

```text
Tomcat started on port 8080
Started OpenModelsDemoApplication
```

Application URL:

```text
http://localhost:8080
```

## 5. Basic LangChain Concept Test

LangChain kyun use hota hai test:

```bash
curl "http://localhost:8080/langchain/why"
```

Expected:

Response me ye keys aani chahiye:

```text
why_use_langchain
abstraction_risks
productivity
```

## 6. LangChain Internals Test

```bash
curl "http://localhost:8080/langchain/internals"
```

Expected:

Response me ye concepts aane chahiye:

```text
LLM / ChatModel
PromptTemplate
Chains / LCEL
Output Parsers
Tools
Agents
Memory / Chat History
RAG
Document Loaders
Vector Stores
Embeddings
Callbacks / Streaming
```

## 7. Raw vs LangChain Test

```bash
curl "http://localhost:8080/langchain/raw-vs-framework?message=Explain+LangChain+in+Java"
```

Expected:

Response me dono fields aani chahiye:

```text
raw_model_call
langchain4j_ai_service_call
```

Meaning:

- `raw_model_call`: direct `ChatLanguageModel` call ka output
- `langchain4j_ai_service_call`: LangChain4j `AiServices` based assistant ka output

## 8. Prompt Template And Chain Test

```bash
curl "http://localhost:8080/langchain/prompt-chain?role=senior+Java+architect&topic=LangChain&question=Why+use+prompt+templates?"
```

Expected:

Response me ye fields aani chahiye:

```text
rendered_prompt
chain
answer
parsed_learning_points
```

Check karo:

- `rendered_prompt` me role, topic, question inject hua ho
- `chain` value `prompt -> chatModel -> stringParser` ho
- `parsed_learning_points` list format me ho

## 9. Tool Integration Test

```bash
curl "http://localhost:8080/langchain/tools?sku=JDK-21"
```

Expected:

Response me SKU aur answer aaye:

```text
sku
answer
```

Answer me inventory/stock related information aani chahiye.

Example expected meaning:

```text
JDK-21 ke liye stock check hua
```

## 10. Memory Integration Test

First message:

```bash
curl -X POST "http://localhost:8080/langchain/memory?conversationId=demo&message=My+name+is+Amit"
```

Second message with same conversation id:

```bash
curl -X POST "http://localhost:8080/langchain/memory?conversationId=demo&message=What+is+my+name?"
```

Expected:

Second response me model previous message ka context use kare.

Expected meaning:

```text
Your name is Amit
```

Important:

Same `conversationId` use karna zaroori hai. Agar `conversationId` change kar doge, memory new conversation maanegi.

## 11. RAG Ingest Test

RAG question answer se pehle docs ingest karo:

```bash
curl -X POST "http://localhost:8080/ingest"
```

Expected:

Response me ye fields aani chahiye:

```text
status
documents_ingested
embedding_model
store
```

Expected meaning:

```text
docs folder ke files vector store me load ho gaye
```

## 12. Existing RAG Ask Test

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

Expected:

Response me ye fields aani chahiye:

```text
answer
model
latency_ms
```

Answer Java 21 virtual threads ke context me hona chahiye.

## 13. New Explicit RAG Chain Test

```bash
curl "http://localhost:8080/langchain/rag-chain?question=What+are+virtual+threads?"
```

Expected:

Response me ye fields aani chahiye:

```text
question
retrieved_chunks
answer
```

Check karo:

- `retrieved_chunks` 0 se greater ho, agar ingest successful tha
- answer retrieved context ke basis par ho

## 14. RAG Evaluation Test

```bash
curl -X POST "http://localhost:8080/evaluate"
```

Expected:

Response me ye fields aani chahiye:

```text
model
test_cases
results
details
```

Results ke andar:

```text
faithfulness
answer_relevance
context_precision
source_accuracy
average_latency_ms
```

## 15. Existing Chat Test

Normal chat:

```bash
curl "http://localhost:8080/chat?message=What+is+Java+record+keyword?"
```

Expected:

Plain text answer from model.

Tool chat:

```bash
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+do+we+have+in+stock?"
```

Expected:

Inventory tool based answer.

## 16. Comments Verification

Code me concept comments verify karo:

```bash
rg "\[ .* \]" src/main/java
```

Expected:

Output me comments milne chahiye like:

```text
[ 9. LLM / ChatModel: ... ]
[ 10. PromptTemplate: ... ]
[ 13. Tools: ... ]
[ 15. Memory / Chat History: ... ]
[ 5. RAG chain: ... ]
```

## 17. Common Issues

### Issue: Port 8080 already in use

Check karo:

```bash
netstat -ano | findstr :8080
```

Windows par process stop karna ho to:

```bash
taskkill /PID <PID> /F
```

Ya port change karo `src/main/resources/application.yml` me:

```yaml
server:
  port: 8081
```

### Issue: Ollama not running

Ollama start karo:

```bash
ollama serve
```

Phir app restart karo.

### Issue: Model not found

Required model pull karo:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

### Issue: RAG answer empty ya weak hai

Pehle ingest run karo:

```bash
curl -X POST "http://localhost:8080/ingest"
```

Phir question ask karo.

### Issue: Memory answer previous context nahi use kar raha

Check karo:

- same `conversationId` use ho raha hai
- app restart nahi hui
- memory in-memory hai, restart ke baad clear ho jayegi

## 18. Recommended Test Order

Best order:

```text
1. mvn test
2. mvn -DskipTests package
3. app start
4. /langchain/why
5. /langchain/internals
6. /langchain/raw-vs-framework
7. /langchain/prompt-chain
8. /langchain/tools
9. /langchain/memory
10. /ingest
11. /ask
12. /langchain/rag-chain
13. /evaluate
```

Is order me basic setup pehle verify hota hai, phir model calls, tools, memory, and finally RAG evaluation.
