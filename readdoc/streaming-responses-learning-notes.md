# Streaming Responses Learning Notes

Ye notes is chat mein kiye gaye changes aur concepts ko simple terms mein explain karte hain.

## Goal

Hume app mein streaming responses add karni thi:

- Normal response vs streaming response
- Token-by-token response
- Server-Sent Events streaming endpoint
- Chat UI experience
- LangChain4j/Ollama streaming
- Streaming error handling
- Streaming latency benefit

Branch create ki gayi:

```text
develop_streaming_feature1
```

## Pehle App Ka Flow

Pehle app mein normal chat endpoint tha:

```text
GET /chat?message=...
```

Flow:

```text
Client
  -> ChatController
  -> Assistant
  -> ChatLanguageModel
  -> Ollama
  -> Full answer return
```

Is flow mein client ko response tab milta hai jab model pura answer generate kar leta hai.

Short version:

```text
Normal response = wait karo, phir full answer ek baar mein milta hai
```

## Streaming Flow

Streaming mein answer chhote-chhote parts mein aata hai. In parts ko usually tokens bolte hain.

New endpoint:

```text
GET /chat/stream?message=...
```

Flow:

```text
Client
  -> ChatController
  -> StreamingChatService
  -> StreamingChatLanguageModel
  -> Ollama
  -> token events back to client
```

Short version:

```text
Streaming response = answer generate hote hi screen par dikhna start
```

## Files Changed

Important files:

```text
src/main/java/com/javaone/openmodels/config/OllamaConfig.java
src/main/java/com/javaone/openmodels/controller/ChatController.java
src/main/java/com/javaone/openmodels/service/StreamingChatService.java
src/main/resources/static/chat.html
README.md
```

## OllamaConfig.java

Pehle config mein normal model bean tha:

```java
ChatLanguageModel chatModel()
```

Ye full response ke liye use hota hai.

Humne streaming model bean add kiya:

```java
StreamingChatLanguageModel streamingChatModel()
```

Simple difference:

```text
ChatLanguageModel = full answer ek baar mein
StreamingChatLanguageModel = token-by-token answer
```

Dono same Ollama settings use karte hain:

```text
ollama.base-url
ollama.chat-model
temperature
timeout
```

## ChatController.java

Pehle endpoints:

```text
/chat
/chat/tools
```

New endpoints:

```text
/chat/normal
/chat/stream
```

### /chat/normal

```text
GET /chat/normal?message=...
```

Ye normal non-streaming response deta hai.

Isme:

1. Start time note hota hai
2. Model ko message bheja jata hai
3. Full answer wait kiya jata hai
4. Total latency calculate hoti hai
5. JSON response return hota hai

Example response:

```json
{
  "answer": "Java virtual threads are...",
  "streaming": false,
  "latency_ms": 2400
}
```

### /chat/stream

```text
GET /chat/stream?message=...
```

Ye SSE streaming response deta hai.

Important code idea:

```java
produces = MediaType.TEXT_EVENT_STREAM_VALUE
```

Iska matlab:

```text
Response normal JSON nahi hai.
Response ek open stream hai jisme events aate rahenge.
```

## StreamingChatService.java

Ye main streaming logic hai.

Main method:

```java
public SseEmitter stream(String message)
```

`SseEmitter` Spring ka object hai jo browser/client ke saath HTTP connection open rakhta hai.

Us open connection par server repeatedly data bhej sakta hai.

Simple meaning:

```text
SseEmitter = server se browser tak open pipe
```

## SSE Events

Humne 4 event types use kiye:

```text
start
token
complete
error
```

### start event

Streaming start hote hi bheja jata hai.

Purpose:

```text
Client ko pata chale ki stream open ho gayi hai
```

### token event

Har partial response/token ke liye bheja jata hai.

Example:

```json
{
  "token": "Java",
  "token_index": 1
}
```

Browser token ko current answer ke end mein append karta hai.

### complete event

Jab model answer complete kar deta hai tab bheja jata hai.

Example:

```json
{
  "message": "Streaming complete",
  "tokens_streamed": 82,
  "first_token_latency_ms": 450,
  "total_latency_ms": 3100
}
```

### error event

Agar streaming ke beech mein issue aaye, error event bheja jata hai.

Example reasons:

```text
Ollama down hai
Model available nahi hai
Client disconnect ho gaya
Network issue aa gaya
```

## sendEvent Method

Code:

```java
private static void sendEvent(SseEmitter emitter, String eventName, Object data) {
    try {
        emitter.send(SseEmitter.event()
            .name(eventName)
            .data(data));
    } catch (IOException ignored) {
        emitter.complete();
    }
}
```

Simple explanation:

```text
sendEvent = browser ko named SSE message bhejna
```

Parameters:

```text
emitter   = open connection
eventName = event ka naam, jaise token ya complete
data      = actual payload
```

Example call:

```java
sendEvent(emitter, "token", Map.of("token", "Java"));
```

Browser ko roughly ye milega:

```text
event: token
data: {"token":"Java"}
```

`try-catch` isliye hai kyunki browser tab close kar sakta hai ya connection break ho sakta hai.

Agar `IOException` aaye to:

```java
emitter.complete();
```

Matlab server side stream close kar do.

## Latency Benefit

Normal response:

```text
User waits until full answer is ready
```

Streaming response:

```text
User sees first token quickly, even if full answer takes longer
```

Example:

```text
Normal total latency: 3000 ms
Streaming first token latency: 400 ms
Streaming total latency: 3000 ms
```

Total time same ho sakta hai, lekin user experience better hota hai kyunki response jaldi start ho jata hai.

## Chat UI

File:

```text
src/main/resources/static/chat.html
```

Spring Boot static files ko automatically serve karta hai.

URL:

```text
http://localhost:8080/chat.html
```

UI mein 2 buttons hain:

```text
Stream
Normal
```

### Normal Button

Calls:

```text
/chat/normal
```

Full answer ek baar mein show hota hai.

### Stream Button

Uses browser API:

```javascript
new EventSource("/chat/stream?message=...")
```

`EventSource` SSE events listen karta hai.

It listens for:

```text
token
complete
error
```

Token event aate hi UI answer text mein token append karta hai.

## How To Test

### 1. Branch check

```bash
git branch --show-current
```

Expected:

```text
develop_streaming_feature1
```

### 2. Ollama check

```bash
ollama list
```

If model missing:

```bash
ollama pull qwen2.5:0.5b
```

### 3. Start app

```bash
mvn spring-boot:run
```

### 4. Test normal response

```bash
curl "http://localhost:8080/chat/normal?message=Explain+Java+virtual+threads+in+simple+words"
```

Expected:

```text
JSON with answer, streaming=false, latency_ms
```

### 5. Test streaming response

```bash
curl -N "http://localhost:8080/chat/stream?message=Explain+Spring+Boot+in+5+points"
```

Expected:

```text
event:start
event:token
event:token
event:complete
```

`-N` important hai because curl buffering disable karta hai.

### 6. Test UI

Open:

```text
http://localhost:8080/chat.html
```

Try both buttons:

```text
Stream = token-by-token response
Normal = full response at once
```

### 7. Build verification

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

## End-to-End Summary

Normal:

```text
User sends message
  -> app waits for full answer
  -> returns complete response
```

Streaming:

```text
User sends message
  -> app opens SSE stream
  -> model generates tokens
  -> server sends token events
  -> browser updates answer live
  -> complete event closes stream
```

Final simple line:

```text
Normal response waits for the full answer.
Streaming response starts showing the answer as soon as tokens are generated.
```
