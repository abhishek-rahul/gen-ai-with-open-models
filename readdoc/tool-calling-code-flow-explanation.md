# Tool Calling Code Flow Explanation

Ye document simple terms me explain karta hai ki branch `develop_tools_feature1` me tool calling / function calling ke liye kya code changes kiye gaye hain aur pura flow kaise work karta hai.

## 1. Simple Meaning

Normal chat me LLM sirf apni training knowledge se answer deta hai.

Tool calling me LLM ke paas kuch Java methods available hote hain. Jab user aisa question poochta hai jisme app data, order data, refund policy, ya API-style data chahiye hota hai, to LLM Java method call karne ka decision leta hai.

Important point:

- LLM method directly execute nahi karta.
- LLM decide karta hai ki kaunsa tool call karna hai.
- Java application actual method execute karti hai.
- Tool ka result wapas LLM ko diya jata hai.
- LLM final user-friendly answer banata hai.

## 2. New Files Added

### `CommerceSupportTools.java`

Ye main tool class hai. Isme actual Java methods hain jo LLM use kar sakta hai.

Tools:

- `getOrderStatus`
- `getRefundPolicy`
- `getWeather`

Har method ke upar `@Tool` annotation hai. Ye annotation LangChain4j ko batata hai ki ye method LLM ke liye tool ke roop me available hai.

Example:

```java
@Tool("Get current order status, delivery date, and tracking information by order id")
public String getOrderStatus(@P("Order id, for example ORD-1001") String orderId)
```

Yaha:

- `@Tool` method ka purpose describe karta hai.
- `@P` parameter ka meaning describe karta hai.
- LLM in descriptions ko padh kar decide karta hai ki tool kab use karna hai.

### `ToolCallingAssistant.java`

Ye tool-enabled assistant interface hai.

Isme system prompt diya gaya hai:

- Order status ke liye tool use karo.
- Refund policy ke liye tool use karo.
- Weather/API question ke liye tool use karo.
- Agar tool `TOOL_ERROR` return kare to user ko corrected input maango.
- Normal general question ho to tool use mat karo.

Ye file LLM ke behavior ko guide karti hai.

### `ToolAuditLog.java`

Ye in-memory audit log maintain karta hai.

Jab bhi koi tool call hota hai, ye details save hoti hain:

- Tool name
- Arguments
- Success ya error status
- Result ya error message
- Timestamp

Ye production idea samjhane ke liye important hai, kyunki real systems me tool calls traceable hone chahiye.

### `ToolCatalog.java`

Ye demo ke liye tool schema expose karta hai.

Schema ka matlab:

- Tool ka naam kya hai
- Tool kis kaam ke liye hai
- Tool ko kaunse parameters chahiye
- Parameter required hai ya optional

Endpoint:

```bash
GET /tools/schema
```

### `InventoryTools.java`

Purana inventory-only tool remove kar diya gaya hai, kyunki ab broader demo ke liye `CommerceSupportTools.java` use ho raha hai.

## 3. Existing Files Updated

### `OllamaConfig.java`

Is file me LangChain4j aur Ollama configuration hai.

Important beans:

```java
ChatLanguageModel chatModel()
```

Ye Ollama model create karta hai using:

- `ollama.base-url`
- `ollama.chat-model`
- temperature
- timeout

Normal assistant:

```java
Assistant chatAssistant(ChatLanguageModel chatModel)
```

Ye simple chat ke liye hai. Isme koi tools nahi diye gaye.

Tool assistant:

```java
ToolCallingAssistant toolAssistant(
    ChatLanguageModel chatModel,
    CommerceSupportTools commerceSupportTools
)
```

Yaha important line hai:

```java
.tools(commerceSupportTools)
```

Is line se LangChain4j ko pata chalta hai ki `CommerceSupportTools` ke `@Tool` methods LLM ke liye available hain.

### `ChatController.java`

Ye REST endpoints expose karta hai.

Normal chat:

```bash
GET /chat?message=...
```

Is endpoint me tools available nahi hain.

Tool chat:

```bash
GET /chat/tools?message=...
```

Is endpoint me tools available hain.

Tool schema:

```bash
GET /tools/schema
```

Tool audit log:

```bash
GET /tools/audit
```

Audit clear:

```bash
DELETE /tools/audit
```

## 4. Full Code Flow

### Flow A: Normal Chat

Request:

```bash
curl "http://localhost:8080/chat?message=What+is+Java+21?"
```

Flow:

1. Request `ChatController.chat()` method me aati hai.
2. Controller `chatAssistant.chat(message)` call karta hai.
3. `chatAssistant` simple `Assistant` interface use karta hai.
4. Is assistant ke paas koi tools nahi hain.
5. LLM normal answer return karta hai.

Use case:

- General explanation
- Java concept
- Simple Q&A

### Flow B: Tool Calling Chat

Request:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

Flow:

1. Request `ChatController.chatWithTools()` method me aati hai.
2. Controller `toolAssistant.chat(message)` call karta hai.
3. `toolAssistant` ke paas `CommerceSupportTools` registered hai.
4. LangChain4j LLM ko available tool schemas batata hai.
5. LLM user message ko dekhta hai.
6. LLM decide karta hai ki order status ke liye `getOrderStatus` tool use karna chahiye.
7. LangChain4j Java method call karta hai:

```java
getOrderStatus("ORD-1001")
```

8. Java method order data lookup karta hai.
9. Method result return karta hai.
10. Audit log me success entry save hoti hai.
11. Tool result LLM ko wapas diya jata hai.
12. LLM final natural language answer banata hai.

## 5. Tool Details

### Order Status Tool

Method:

```java
getOrderStatus(String orderId)
```

Sample supported orders:

- `ORD-1001`
- `ORD-1002`
- `ORD-1003`

Example:

```bash
curl "http://localhost:8080/chat/tools?message=Where+is+ORD-1001?"
```

### Refund Policy Tool

Method:

```java
getRefundPolicy(String category)
```

Supported categories:

- `software`
- `workshop`
- `merchandise`

Example:

```bash
curl "http://localhost:8080/chat/tools?message=Can+I+refund+a+workshop+pass?"
```

### Weather/API Tool

Method:

```java
getWeather(String city)
```

Supported demo cities:

- `Bangalore`
- `Delhi`
- `San Francisco`

Example:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+weather+in+Bangalore?"
```

Isko API-style example bola gaya hai kyunki real project me yahi method kisi external weather API ko call kar sakta hai.

## 6. Error Handling

Har tool input validate karta hai.

Example invalid order:

```bash
curl "http://localhost:8080/chat/tools?message=Where+is+ORD-9999?"
```

Internally:

1. `getOrderStatus("ORD-9999")` call hota hai.
2. Order map me order nahi milta.
3. Method `TOOL_ERROR: No order found...` return karta hai.
4. Audit log me error entry save hoti hai.
5. LLM user ko politely bolta hai ki order id verify kare.

## 7. Audit Log Flow

Successful tool call:

```java
auditLog.success("getOrderStatus", arguments, result);
```

Failed tool call:

```java
auditLog.failure("getOrderStatus", arguments, error);
```

Audit dekhne ke liye:

```bash
curl "http://localhost:8080/tools/audit"
```

Clear karne ke liye:

```bash
curl -X DELETE "http://localhost:8080/tools/audit"
```

## 8. Tool Schema Endpoint

Request:

```bash
curl "http://localhost:8080/tools/schema"
```

Ye endpoint manually batata hai ki app me kaunse tools available hain.

Example schema information:

- `getOrderStatus` needs `orderId`
- `getRefundPolicy` needs `category`
- `getWeather` needs `city`

Ye concept important hai because LLM ko bhi similar schema diya jata hai so that it can decide the correct tool call.

## 9. Normal Chat vs Tool Chat

### Normal Chat

Endpoint:

```bash
/chat
```

Behavior:

- Sirf LLM answer deta hai.
- App ke private data tak access nahi hai.
- Order status accurately nahi bata sakta.

### Tool Chat

Endpoint:

```bash
/chat/tools
```

Behavior:

- LLM tools use kar sakta hai.
- Java methods actual data lookup karte hain.
- Final answer tool result ke basis par banta hai.

Simple comparison:

| Feature | Normal Chat | Tool Chat |
|---|---|---|
| General answer | Yes | Yes |
| Java tool access | No | Yes |
| Order status lookup | No | Yes |
| Refund policy lookup | No | Yes |
| Weather/API-style lookup | No | Yes |
| Audit log entry | No | Yes, when tool is called |

## 10. One-Line Summary

Is implementation me LLM brain ka kaam karta hai, Java tools hands ka kaam karte hain, aur audit log memory ka kaam karta hai.
