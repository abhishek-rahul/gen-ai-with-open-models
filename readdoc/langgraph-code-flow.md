# LangGraph Feature Code Flow - Simple Explanation

Ye document un changes ko simple terms me explain karta hai jo `develop_langgraph_feature2` branch me add kiye gaye hain.

## 1. Is feature ka goal kya hai?

Normal LangChain agent me LLM aksar khud decide karta hai ki tool call karna hai ya answer dena hai. Is feature me humne ek controlled LangGraph-style workflow banaya hai.

Simple words me:

```text
User Request
  -> START
  -> Planner
  -> Human Approval, agar risky action hai
  -> Retriever
  -> Tool, agar inventory/stock/SKU question hai
  -> Executor
  -> Validator
  -> END
```

Yahan har step ek node hai, aur nodes ke beech ka route edges decide karte hain.

## 2. Main files

### `LangGraphController.java`

Path:

```text
src/main/java/com/javaone/openmodels/controller/LangGraphController.java
```

Ye REST API expose karta hai.

Endpoints:

```text
GET /langgraph/run
GET /langgraph/state
```

Example:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=demo&message=How+many+units+of+SKU+JDK-21+are+in+stock?"
```

Controller ka kaam simple hai:

```text
HTTP request receive karo
  -> message, sessionId, approved read karo
  -> LangGraphWorkflowService.run(...) call karo
  -> response user ko return karo
```

### `LangGraphWorkflowService.java`

Path:

```text
src/main/java/com/javaone/openmodels/service/LangGraphWorkflowService.java
```

Ye feature ka core hai. Isi file me graph ka state, nodes, edges, retry, tool calling, persistence, human approval sab implemented hai.

## 3. GraphState kya hai?

`GraphState` ek shared data object hai jo har node ke paas pass hota hai.

Isme important fields hain:

```text
sessionId       -> conversation/session identify karne ke liye
userInput       -> user ka current message
messages        -> user/tool/assistant messages history
retrievedDocs   -> RAG retriever se aaye docs
plan            -> planner ka generated plan
toolResult      -> tool ka output
finalAnswer     -> final LLM answer
needsTool       -> kya tool call karna hai?
requiresApproval -> kya human approval chahiye?
approved        -> user ne approval diya ya nahi
answerReady     -> final answer ready hai ya nahi
trace           -> kaun-kaun se nodes chale
errors          -> failures/retry errors
```

Simple example:

```text
User asks: "How many units of SKU JDK-21 are in stock?"

State becomes:
userInput = same question
needsTool = true
toolResult = "SKU JDK-21: 150 units in stock"
finalAnswer = LLM generated answer
trace = START, planner, retriever, tool, executor, validator, END
```

## 4. START and END

Code me:

```java
private static final String START = "START";
private static final String END = "END";
```

`START` graph ka entry point hai.

`END` graph ka exit point hai.

Flow `START` se begin hota hai aur jab validator bolta hai answer ready hai, tab graph `END` par ruk jata hai.

## 5. Nodes kya hain?

Node ek function hai jo state leta hai aur updated state return karta hai.

Code me nodes yahan register hote hain:

```java
graphNodes.put("planner", this::plannerNode);
graphNodes.put("humanApproval", this::humanApprovalNode);
graphNodes.put("retriever", this::retrieverNode);
graphNodes.put("tool", this::toolNode);
graphNodes.put("executor", this::executorNode);
graphNodes.put("validator", this::validatorNode);
```

Matlab:

```text
plannerNode       -> decide karta hai kya karna hai
humanApprovalNode -> risky request par approval check karta hai
retrieverNode     -> RAG docs retrieve karta hai
toolNode          -> inventory tool call karta hai
executorNode      -> LLM se final answer banwata hai
validatorNode     -> check karta hai answer valid hai ya retry chahiye
```

## 6. Edges kya hain?

Edges batate hain ki ek node ke baad next node kaunsi hogi.

Example:

```text
START -> planner
tool -> executor
executor -> validator
```

Code me:

```java
graphEdges.put(START, List.of(new GraphEdge("planner", state -> true)));
```

Iska matlab: START ke baad hamesha planner chalega.

## 7. Conditional Edges kya hain?

Conditional edge decision-based routing hai.

Example:

```java
graphEdges.put("retriever", List.of(
    new GraphEdge("tool", GraphState::needsTool),
    new GraphEdge("executor", state -> true)
));
```

Iska simple meaning:

```text
Agar needsTool = true hai
  -> tool node chalao
warna
  -> executor node chalao
```

Isi tarah risky action me:

```text
Agar requiresApproval = true
  -> humanApproval node
warna
  -> retriever node
```

## 8. Planner Node

Planner user input ko analyze karta hai.

Code logic:

```text
Agar message me stock/inventory/sku hai
  -> needsTool = true

Agar message me delete/restart/deploy/payment/refund hai
  -> requiresApproval = true
```

Example:

```text
"How many units of SKU JDK-21?"
needsTool = true
requiresApproval = false
```

Example:

```text
"Deploy the payment workflow"
needsTool = false
requiresApproval = true
```

## 9. Human Approval Node

Risky action ke liye approval gate hai.

Without approval:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=ops&message=Deploy+payment+workflow"
```

Response me final answer aayega:

```text
Approval required before executing this risky workflow. Retry with approved=true.
```

With approval:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=ops&message=Deploy+payment+workflow&approved=true"
```

Ab graph retriever/executor tak continue karega.

## 10. Retriever Node

Retriever node existing RAG setup use karta hai:

```java
contentRetriever.retrieve(new Query(state.userInput()));
```

Ye docs se relevant chunks fetch karta hai aur `retrievedDocs` me store karta hai.

Simple flow:

```text
User question
  -> vector store search
  -> relevant docs
  -> state.retrievedDocs
```

## 11. Tool Node

Tool node inventory lookup karta hai.

Ye `InventoryTools.checkStock(...)` call karta hai.

Example:

```text
Input: "SKU JDK-21 stock?"
Extracted SKU: JDK-21
Tool output: SKU JDK-21: 150 units in stock
```

Important point:

Tool free-form LLM decision se nahi chal raha. Graph route controlled hai:

```text
needsTool = true
  -> tool node execute
```

## 12. Executor Node

Executor final LLM answer generate karta hai.

Ye prompt banata hai jisme:

```text
user input
plan
retrieved docs
tool result
previous messages
```

Phir:

```java
chatModel.chat(prompt);
```

call hota hai.

## 13. Validator Node

Validator check karta hai ki answer ready hai ya nahi.

Simple validation:

```text
finalAnswer null nahi hona chahiye
finalAnswer length 20 characters se zyada honi chahiye
```

Agar answer ready hai:

```text
validator -> END
```

Agar answer ready nahi hai:

```text
validator -> executor
```

Ye agent loop banata hai.

## 14. Retry Flow and Error Handling

Har node `executeWithRetry(...)` ke through run hota hai.

Retry logic:

```text
Attempt 1
Attempt 2
Attempt 3
```

Agar teeno attempts fail ho jaye:

```text
Workflow failed after retries: error message
```

Error messages `state.errors` me save hote hain.

## 15. Reducers

Reducer ka matlab hai state update ka rule.

Example:

```java
appendMessage("tool", toolResult)
```

Ye old messages ko remove nahi karta. Old messages ke end me new message append karta hai.

Simple:

```text
Before:
messages = ["user: hello"]

After:
messages = ["user: hello", "tool: SKU JDK-21: 150 units in stock"]
```

## 16. Checkpointer / Persistence

Service me ye map hai:

```java
private final Map<String, GraphState> checkpointer = new ConcurrentHashMap<>();
```

Ye session-wise state save karta hai.

Example:

```text
sessionId = demo
state = latest graph state
```

State inspect karne ke liye:

```bash
curl "http://localhost:8080/langgraph/state?sessionId=demo"
```

Note: Ye in-memory hai. App restart hone par state reset ho jayegi.

## 17. Subgraphs

Code me planner aur executor logic ko separate helper methods me rakha gaya hai:

```java
runPlannerSubgraph(...)
runExecutorSubgraph(...)
```

Real LangGraph me subgraph ek smaller graph hota hai. Yahan demo ke liye same idea simple Java methods me show kiya gaya hai.

## 18. Complete Flow Example: Tool Question

Request:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=demo&message=How+many+units+of+SKU+JDK-21+are+in+stock?"
```

Flow:

```text
START
  -> planner
      needsTool = true
  -> retriever
      docs fetch
  -> tool
      InventoryTools.checkStock("JDK-21")
  -> executor
      LLM final answer
  -> validator
      answerReady = true
  -> END
```

Response me important fields:

```text
plan
messages
retrieved_docs
tool_result
final_answer
trace
errors
```

## 19. Complete Flow Example: Risky Request

Request:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=ops&message=Deploy+payment+workflow"
```

Flow:

```text
START
  -> planner
      requiresApproval = true
  -> humanApproval
      approved = false
  -> validator
  -> END
```

Approval ke saath:

```bash
curl "http://localhost:8080/langgraph/run?sessionId=ops&message=Deploy+payment+workflow&approved=true"
```

Flow:

```text
START
  -> planner
  -> humanApproval
      approved = true
  -> retriever
  -> executor
  -> validator
  -> END
```

## 20. One-line Summary

Is feature me humne Java/Spring Boot app ke andar LangGraph-style controlled workflow banaya hai jahan LLM, RAG, tools, approval, retry, state, aur final answer ek graph ke nodes aur edges ke through execute hote hain.
