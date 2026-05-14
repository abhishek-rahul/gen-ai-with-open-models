# Agent Feature Code Flow - Simple Explanation

Ye document un changes ko simple terms me explain karta hai jo `develop_agent_feature1` branch me add kiye gaye hain.

## Goal

Is feature ka main goal hai agent concepts ko demo karna:

- Agent kya hota hai
- LLM + tools ka relation
- Manual agent loop
- ReAct pattern
- Reason -> Act -> Observe loop
- Multi-step reasoning
- Agent stopping condition
- Tool selection
- Agent failure cases
- Infinite loop problem

## High Level Idea

Normal chat me user message directly LLM ko jata hai aur LLM answer de deta hai.

Agent flow me LLM sirf answer nahi deta. Pehle wo decide karta hai:

1. Mujhe kya sochna hai? `Reason`
2. Kya koi tool call karna hai? `Act`
3. Tool se kya result mila? `Observe`
4. Ab final answer dena hai ya next step lena hai?

Isi loop ko ReAct bolte hain: `Reason -> Act -> Observe`.

## New Files Added

### 1. `AgentController.java`

Location:

```text
src/main/java/com/javaone/openmodels/controller/AgentController.java
```

Ye REST endpoints expose karta hai.

Endpoints:

```text
GET /agents/concepts
POST /agents/run?goal=...
```

Simple flow:

```text
HTTP request
  -> AgentController
  -> ManualAgentService
  -> response
```

### 2. `ManualAgentService.java`

Location:

```text
src/main/java/com/javaone/openmodels/service/ManualAgentService.java
```

Ye main agent logic rakhta hai.

Is file me:

- Agent concepts explain kiye gaye hain
- Manual ReAct loop implement hai
- LLM prompt build hota hai
- LLM output parse hota hai
- Tool select aur execute hota hai
- Stopping condition check hoti hai
- Infinite loop protection hai

### 3. `application.yml`

Location:

```text
src/main/resources/application.yml
```

Naya config add hua:

```yaml
agent:
  max-steps: 5
```

Iska matlab agent maximum 5 steps tak try karega. Agar 5 steps ke andar final answer nahi mila, agent stop ho jayega.

## Endpoint 1: `/agents/concepts`

Request:

```bash
curl "http://localhost:8080/agents/concepts"
```

Controller method:

```java
@GetMapping("/agents/concepts")
public ManualAgentService.AgentConcepts concepts() {
    return manualAgentService.concepts();
}
```

Flow:

```text
User calls /agents/concepts
  -> AgentController.concepts()
  -> ManualAgentService.concepts()
  -> predefined explanation response
```

Ye endpoint LLM call nahi karta. Ye directly Java record response return karta hai jisme agent ke concepts simple text me hain.

## Endpoint 2: `/agents/run`

Request:

```bash
curl -X POST "http://localhost:8080/agents/run?goal=Check+stock+for+JDK-21+and+explain+what+the+agent+did"
```

Controller method:

```java
@PostMapping("/agents/run")
public ManualAgentService.AgentRun run(@RequestParam String goal) {
    return manualAgentService.run(goal);
}
```

Flow:

```text
User gives goal
  -> AgentController.run(goal)
  -> ManualAgentService.run(goal)
  -> LLM se next step poocha jata hai
  -> LLM Action choose karta hai
  -> Java selected tool run karta hai
  -> Observation scratchpad me add hoti hai
  -> Loop repeat hota hai
  -> Final answer ya failure response return hota hai
```

## Manual Agent Loop Kaise Kaam Karta Hai

Main loop `ManualAgentService.run(String goal)` method me hai.

Simplified version:

```text
steps = empty list
scratchpad = empty list

for step 1 to maxSteps:
    prompt banao
    LLM ko prompt bhejo
    LLM output parse karo

    agar Final mila:
        completed response return karo

    agar Action nahi mila:
        failure response return karo

    agar same action repeat ho raha hai:
        infinite loop protection ke liye stop karo

    tool run karo
    observation save karo
    scratchpad update karo

agar maxSteps complete ho gaye:
    failure response return karo
```

## Prompt Ka Role

`buildPrompt()` method LLM ko rules batata hai.

Prompt LLM ko bolta hai:

- Tum ek small ReAct agent ho
- Available tools ye hain:
  - `checkStock`
  - `explainAgentConcept`
- Tool call ka exact format ye hona chahiye:

```text
Thought: one short reason
Action: checkStock OR explainAgentConcept
Action Input: tool input
```

- Final answer ka exact format:

```text
Thought: one short reason
Final: final answer for the user
```

Important baat: LLM directly Java method call nahi karta. LLM sirf text me action name aur action input deta hai. Java code us text ko parse karke actual tool run karta hai.

## Scratchpad Kya Hai

Scratchpad previous steps ki memory hai.

Example:

```text
Thought: Need inventory data
Action: checkStock
Action Input: JDK-21
Observation: SKU JDK-21: 150 units in stock
```

Next LLM call me ye scratchpad prompt me bheja jata hai, taaki model ko pata rahe pichhle step me kya hua tha.

## Tool Selection Kaise Hoti Hai

LLM prompt dekhkar decide karta hai ki kaunsa tool use karna hai.

Available tools:

```text
checkStock
explainAgentConcept
```

Phir Java method `runTool()` action name ke basis par tool run karta hai:

```java
private String runTool(String actionName, String actionInput) {
    return switch (actionName.trim()) {
        case "checkStock" -> checkStock(actionInput);
        case "explainAgentConcept" -> explainAgentConcept(actionInput);
        default -> "Failure: unknown tool...";
    };
}
```

Iska benefit: LLM kisi bhi random method ko execute nahi kar sakta. Sirf allowed tools hi run honge.

## Tool 1: `checkStock`

Ye inventory check karta hai.

Internally ye existing `InventoryTools` class use karta hai:

```java
private final InventoryTools inventoryTools = new InventoryTools();
```

Input example:

```text
JDK-21
```

Output example:

```text
SKU JDK-21: 150 units in stock
```

SKU validation ke liye regex use hota hai:

```java
Pattern.compile("(?i)\\b[A-Z]{2,}-\\d+\\b")
```

Matlab input me `JDK-21`, `MVN-4`, `GRAAL-22` jaisa SKU hona chahiye.

## Tool 2: `explainAgentConcept`

Ye agent concepts explain karta hai.

Allowed inputs:

```text
agent
llm_tools
manual_loop
react
stopping
failures
infinite_loop
```

Example:

```text
Action: explainAgentConcept
Action Input: react
```

Observation:

```text
ReAct is Reason -> Act -> Observe repeated until the agent can produce Final.
```

## LLM Output Parsing

LLM output plain text hota hai. Code usme se ye labels dhundta hai:

```text
Thought:
Action:
Action Input:
Final:
```

Parsing `parseDecision()` aur `findLineValue()` methods me hoti hai.

Example LLM output:

```text
Thought: I need inventory data first.
Action: checkStock
Action Input: JDK-21
```

Parsed decision:

```text
thought = I need inventory data first.
action = checkStock
actionInput = JDK-21
finalAnswer = empty
```

## Stopping Conditions

Agent loop stop hone ke cases:

### 1. Final answer mil gaya

LLM output:

```text
Final: JDK-21 has 150 units in stock.
```

Status:

```text
completed
```

### 2. Model ne Action ya Final nahi diya

Example:

```text
I think stock is available.
```

Is case me code stop karega, kyunki required format missing hai.

Status:

```text
failed
```

### 3. Same action repeat hua

Example:

```text
Step 1: checkStock JDK-21
Step 2: checkStock JDK-21
```

Ye infinite loop ka sign ho sakta hai, isliye code stop karta hai.

### 4. Max steps complete ho gaye

Config:

```yaml
agent:
  max-steps: 5
```

Agar 5 steps ke baad bhi `Final:` nahi mila, agent stop ho jayega.

## Response Structure

`/agents/run` endpoint `AgentRun` response return karta hai.

Fields:

```text
goal              user ka original goal
status            completed ya failed
maxSteps          allowed max steps
stepsUsed         kitne steps use hue
stoppingCondition loop kyu stop hua
answer            final answer ya failure message
trace             har step ki detail
```

`trace` ke andar har step me:

```text
step
thought
action
actionInput
observation
rawModelOutput
finalStep
```

Isse demo me clearly dikh jata hai ki agent ne kaise reason kiya, tool choose kiya, observe kiya, aur final answer diya.

## Example End-to-End Flow

User request:

```text
Check stock for JDK-21 and explain what the agent did
```

Possible flow:

```text
Step 1
Thought: Need to check inventory for JDK-21.
Action: checkStock
Action Input: JDK-21
Observation: SKU JDK-21: 150 units in stock

Step 2
Thought: I have the inventory result, now I can answer.
Final: JDK-21 has 150 units in stock. I used the checkStock tool after reasoning that inventory data was required.
```

## Existing Tool Calling vs Manual Agent Loop

Already existing endpoint:

```text
GET /chat/tools
```

Ye LangChain4j AI Service ke through automatic tool calling karta hai.

New endpoint:

```text
POST /agents/run
```

Ye manual loop dikhata hai.

Difference:

```text
/chat/tools
  -> framework handles tool calling internally

/agents/run
  -> our Java code shows each Reason, Act, Observe step
```

Demo aur learning ke liye `/agents/run` better hai kyunki pura trace visible hai.

## One-Line Summary

Ye feature ek visible agent loop add karta hai jisme LLM next step decide karta hai, Java allowed tool execute karta hai, observation wapas LLM ko milti hai, aur loop final answer ya stopping condition tak chalta hai.
