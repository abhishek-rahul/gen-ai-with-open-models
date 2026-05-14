# Testing Steps

Ye file project ke existing demos aur naye LangGraph-style workflow ko test karne ke liye hai.

## 1. Prerequisites Check

Java version:

```bash
java -version
```

Maven version:

```bash
mvn -version
```

Ollama running hai ya nahi:

```bash
ollama list
```

Required models available hone chahiye:

```bash
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
```

## 2. Build Test

Project compile aur tests run karne ke liye:

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

Note: Abhi project me test source files nahi hain, lekin ye command compile verification kar deta hai.

## 3. Start Application

Spring Boot app start karo:

```bash
mvn spring-boot:run
```

Expected:

```text
Tomcat started on port 8080
Started OpenModelsDemoApplication
```

Base URL:

```text
http://localhost:8080
```

## 4. Basic Chat Test

```bash
curl "http://localhost:8080/chat?message=What+is+Java+21?"
```

Expected:

```text
LLM se normal text answer aana chahiye.
```

## 5. Existing Tool Calling Test

```bash
curl "http://localhost:8080/chat/tools?message=How+many+units+of+JDK-21+do+we+have+in+stock?"
```

Expected:

```text
Inventory/stock related answer aana chahiye.
```

## 6. RAG Ingest Test

LangGraph workflow retriever node RAG content retriever use karta hai. Isliye docs ingest karna useful hai.

```bash
curl -X POST "http://localhost:8080/ingest"
```

Expected response fields:

```text
status
documents_ingested
embedding_model
store
```

Example:

```json
{
  "status": "success",
  "documents_ingested": 3,
  "embedding_model": "nomic-embed-text",
  "store": "in-memory"
}
```

## 7. RAG Ask Test

```bash
curl "http://localhost:8080/ask?question=What+are+virtual+threads+in+Java+21?"
```

Expected response fields:

```text
answer
model
latency_ms
```

## 8. LangGraph Normal Flow Test

Simple LangGraph workflow test:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=demo&message=What+are+virtual+threads+in+Java+21?"
```

Expected flow:

```text
START
planner
retriever
executor
validator
END
```

Expected response fields:

```text
session_id
user_input
plan
messages
retrieved_docs
final_answer
needs_tool
requires_approval
answer_ready
trace
errors
updated_at
```

Check:

```text
needs_tool should be false
requires_approval should be false
answer_ready should be true
trace should contain START, planner, retriever, executor, validator, END
errors should be empty
```

## 9. LangGraph Tool Flow Test

Inventory/stock/SKU question:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=tool-demo&message=How+many+units+of+SKU+JDK-21+are+in+stock?"
```

Expected flow:

```text
START
planner
retriever
tool
executor
validator
END
```

Check:

```text
needs_tool should be true
tool_result should contain JDK-21 stock information
trace should contain tool:JDK-21
final_answer should mention tool output or stock result
```

## 10. LangGraph State Persistence Test

Pehle workflow run karo:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=state-demo&message=How+many+units+of+SKU+JDK-21+are+in+stock?"
```

Ab saved state inspect karo:

```bash
curl "http://localhost:8080/langgraph/state?sessionId=state-demo"
```

Expected:

```text
Same session ka latest graph state return hona chahiye.
messages me user/tool/assistant entries honi chahiye.
trace me graph path visible hona chahiye.
```

Note:

```text
State in-memory hai. App restart karne par reset ho jayegi.
```

## 11. LangGraph Human Approval Block Test

Risky request without approval:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=approval-demo&message=Deploy+the+new+payment+workflow"
```

Expected flow:

```text
START
planner
humanApproval:blocked
validator
END
```

Check:

```text
requires_approval should be true
approved should be false
final_answer should say approval is required
```

## 12. LangGraph Human Approval Success Test

Same risky request with approval:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=approval-demo&message=Deploy+the+new+payment+workflow&approved=true"
```

Expected flow:

```text
START
planner
humanApproval:approved
retriever
executor
validator
END
```

Check:

```text
requires_approval should be true
approved should be true
trace should contain humanApproval:approved
answer_ready should be true
```

## 13. LangGraph Unknown SKU Test

```bash
curl "http://localhost:8080/langgraph/run?sessionId=unknown-sku&message=How+many+units+of+SKU+ABC-99+are+in+stock?"
```

Expected:

```text
needs_tool should be true
tool_result should say SKU ABC-99 not found in inventory
final_answer should explain that SKU was not found
```

## 14. Error Handling Check

Ollama stop hone par executor node fail ho sakta hai.

Manual negative test:

```text
1. Stop Ollama.
2. Run /langgraph/run endpoint.
3. Response me errors field me error messages aane chahiye.
4. Retry ke baad graceful failure answer aana chahiye.
```

Expected:

```text
errors should not be empty
final_answer should contain workflow failure message
```

## 15. Quick Smoke Test Sequence

Recommended quick sequence:

```bash
mvn test
mvn spring-boot:run
curl -X POST "http://localhost:8080/ingest"
curl "http://localhost:8080/langgraph/run?sessionId=demo&message=How+many+units+of+SKU+JDK-21+are+in+stock?"
curl "http://localhost:8080/langgraph/state?sessionId=demo"
curl "http://localhost:8080/langgraph/run?sessionId=ops&message=Deploy+the+new+payment+workflow"
curl "http://localhost:8080/langgraph/run?sessionId=ops&message=Deploy+the+new+payment+workflow&approved=true"
```

Final expected result:

```text
Build successful
App starts on 8080
Docs ingest successfully
LangGraph normal/tool flow works
State endpoint returns checkpointed state
Human approval block and approved paths both work
```
