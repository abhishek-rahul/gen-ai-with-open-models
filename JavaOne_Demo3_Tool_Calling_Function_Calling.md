# Demo 3: Tool Calling / Function Calling

## What Function Calling Means

Function calling means the LLM does not directly invent answers for questions that need live or private data. Instead, the application exposes safe Java methods as tools. The model chooses a tool, passes structured arguments, receives the tool result, and then writes the final response.

In this project, `@Tool` methods live in `CommerceSupportTools`.

## Tool Schema Design

Each tool has three schema parts:

- Name: the Java method name, such as `getOrderStatus`
- Description: when the model should use the tool
- Parameters: typed inputs described with `@P`

You can inspect the demo schema:

```bash
curl "http://localhost:8080/tools/schema"
```

## LLM Decides Tool Call

The user still sends normal text:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

LangChain4j gives the model the available tool schemas. The model decides that order status needs a tool and calls `getOrderStatus(orderId="ORD-1001")`.

## Tool Execution

The Java app executes the selected method:

- `getOrderStatus` for order status and tracking
- `getRefundPolicy` for refund rules
- `getWeather` for a simulated API/weather lookup

The model does not execute code. The application does.

## Tool Result Back To LLM

After Java returns the result, LangChain4j sends that observation back to the model. The model turns the raw tool result into a user-friendly answer.

## Order Status Tool

```bash
curl "http://localhost:8080/chat/tools?message=Where+is+ORD-1001?"
```

## Refund Policy Tool

```bash
curl "http://localhost:8080/chat/tools?message=Can+I+refund+software?"
```

## Weather/API Tool Example

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+weather+in+San+Francisco?"
```

## Tool Error Handling

Tools validate required inputs and supported values. Errors are returned as `TOOL_ERROR` so the assistant can ask for corrected input instead of hallucinating.

```bash
curl "http://localhost:8080/chat/tools?message=Where+is+ORD-9999?"
```

## Tool Call Audit Log

Every tool execution records:

- Tool name
- Arguments
- Success or error status
- Result or error message
- Timestamp

```bash
curl "http://localhost:8080/tools/audit"
curl -X DELETE "http://localhost:8080/tools/audit"
```

## Tool Calling vs Normal Chat

Normal chat:

```bash
curl "http://localhost:8080/chat?message=What+is+the+status+of+order+ORD-1001?"
```

This endpoint has no tools. The model should not know private order data.

Tool chat:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

This endpoint can use Java tools, so it can answer from application data.
