# Testing Steps For Tool Calling Feature

Ye file tool calling / function calling feature ko test karne ke simple steps batati hai.

## 1. Current Branch Check

```bash
git branch
```

Expected:

```text
* develop_tools_feature1
```

## 2. Build And Compile Test

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

Note: Project me abhi test classes nahi hain, isliye ye command mainly compile/build verification karti hai.

## 3. Ollama Running Check

```bash
ollama list
```

Expected:

```text
qwen2.5:0.5b
nomic-embed-text
```

At least `qwen2.5:0.5b` available hona chahiye, kyunki `application.yml` me chat model wahi configured hai.

## 4. Start Spring Boot App

```bash
mvn spring-boot:run
```

Expected:

```text
Started OpenModelsDemoApplication
```

App default port:

```text
http://localhost:8080
```

## 5. Normal Chat Test

Normal chat endpoint tools use nahi karta.

```bash
curl "http://localhost:8080/chat?message=What+is+Java+21?"
```

Expected:

- LLM normal Java 21 explanation dega.
- Koi tool call nahi hoga.
- `/tools/audit` me entry add nahi hogi.

## 6. Tool Chat: Order Status

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

Expected:

- LLM `getOrderStatus` tool use karega.
- Response me order status, item, tracking number, aur estimated delivery mention hoga.

Expected data:

```text
Order ORD-1001 is SHIPPED
Item: JDK 21 Hoodie
Tracking: TRK-JAVA-1001
Estimated delivery: 2026-05-15
```

## 7. Tool Chat: Refund Policy

```bash
curl "http://localhost:8080/chat/tools?message=Can+I+refund+a+workshop+pass?"
```

Expected:

- LLM `getRefundPolicy` tool use karega.
- Response me workshop refund rule mention hoga.

Expected policy idea:

```text
Workshop passes are refundable up to 7 days before the session.
```

## 8. Tool Chat: Weather/API Example

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+weather+in+Bangalore?"
```

Expected:

- LLM `getWeather` tool use karega.
- Response me Bangalore weather data aayega.

Expected data:

```text
Bangalore
29 C
Partly cloudy
```

## 9. Tool Error Handling: Invalid Order

```bash
curl "http://localhost:8080/chat/tools?message=Where+is+ORD-9999?"
```

Expected:

- `getOrderStatus` tool call hoga.
- Order nahi milega.
- Tool internally `TOOL_ERROR` return karega.
- LLM user ko bolega ki order id verify kare.

Expected behavior:

```text
No order found for ORD-9999
```

Ya similar polite error message.

## 10. Tool Error Handling: Unsupported Refund Category

```bash
curl "http://localhost:8080/chat/tools?message=Can+I+refund+food?"
```

Expected:

- `getRefundPolicy` tool call hoga.
- Unsupported category error aayega.
- LLM supported categories suggest karega.

Supported categories:

```text
software
workshop
merchandise
```

## 11. Tool Schema Endpoint Test

```bash
curl "http://localhost:8080/tools/schema"
```

Expected:

Response me ye tools dikhne chahiye:

```text
getOrderStatus
getRefundPolicy
getWeather
```

Expected parameters:

```text
orderId
category
city
```

## 12. Tool Audit Log Test

Pehle ek tool call run karo:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

Ab audit log dekho:

```bash
curl "http://localhost:8080/tools/audit"
```

Expected:

Audit log me entry honi chahiye:

```text
toolName: getOrderStatus
arguments: orderId=ORD-1001
status: SUCCESS
```

## 13. Tool Audit Error Entry Test

Invalid order call karo:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-9999?"
```

Audit log dekho:

```bash
curl "http://localhost:8080/tools/audit"
```

Expected:

```text
toolName: getOrderStatus
arguments: orderId=ORD-9999
status: ERROR
```

## 14. Clear Audit Log Test

```bash
curl -X DELETE "http://localhost:8080/tools/audit"
```

Phir audit log check karo:

```bash
curl "http://localhost:8080/tools/audit"
```

Expected:

```json
[]
```

## 15. Normal Chat Vs Tool Chat Comparison

Normal chat:

```bash
curl "http://localhost:8080/chat?message=What+is+the+status+of+order+ORD-1001?"
```

Expected:

- Model ko actual order data nahi milna chahiye.
- It may say it cannot access order systems.
- Audit log entry nahi banegi.

Tool chat:

```bash
curl "http://localhost:8080/chat/tools?message=What+is+the+status+of+order+ORD-1001?"
```

Expected:

- Tool call hoga.
- Actual configured order data response me aayega.
- Audit log entry banegi.

## 16. Quick Smoke Test Commands

App running hone ke baad ye commands quick check ke liye enough hain:

```bash
curl "http://localhost:8080/chat?message=What+is+Java+record?"
curl "http://localhost:8080/chat/tools?message=Where+is+ORD-1001?"
curl "http://localhost:8080/chat/tools?message=Can+I+refund+software?"
curl "http://localhost:8080/chat/tools?message=Weather+in+Delhi?"
curl "http://localhost:8080/tools/schema"
curl "http://localhost:8080/tools/audit"
```

## 17. Troubleshooting

### App Start Nahi Ho Raha

Check karo port 8080 already use me to nahi:

```bash
netstat -ano | findstr :8080
```

### Ollama Connection Error

Check:

```bash
ollama list
```

Aur ensure karo `application.yml` me:

```yaml
ollama:
  base-url: http://localhost:11434
```

### Model Response Slow Hai

First request slow ho sakti hai kyunki Ollama model load karta hai. Second request usually faster hoti hai.

### Tool Call Nahi Ho Raha

Try clearer prompt:

```bash
curl "http://localhost:8080/chat/tools?message=Use+the+order+status+tool+for+ORD-1001"
```

Also confirm karo ki `/chat/tools` endpoint use ho raha hai, `/chat` nahi.
