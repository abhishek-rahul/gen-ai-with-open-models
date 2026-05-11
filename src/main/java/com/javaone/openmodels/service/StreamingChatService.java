package com.javaone.openmodels.service;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StreamingChatService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final StreamingChatLanguageModel streamingChatModel;

    public StreamingChatService(StreamingChatLanguageModel streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
    }

    public SseEmitter stream(String message) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        long startedAt = System.currentTimeMillis();
        AtomicLong firstTokenAt = new AtomicLong(0);
        AtomicLong tokenCount = new AtomicLong(0);

        sendEvent(emitter, "start", Map.of(
            "message", "Streaming started",
            "streaming", true
        ));

        try {
            streamingChatModel.chat(message, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    long tokenNumber = tokenCount.incrementAndGet();
                    firstTokenAt.compareAndSet(0, System.currentTimeMillis());

                    sendEvent(emitter, "token", Map.of(
                        "token", partialResponse,
                        "token_index", tokenNumber
                    ));
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    long completedAt = System.currentTimeMillis();
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("message", "Streaming complete");
                    payload.put("tokens_streamed", tokenCount.get());
                    payload.put("first_token_latency_ms", firstTokenLatency(startedAt, firstTokenAt.get()));
                    payload.put("total_latency_ms", completedAt - startedAt);
                    payload.put("finish_reason", completeResponse.metadata().finishReason());
                    payload.put("token_usage", completeResponse.metadata().tokenUsage());

                    sendEvent(emitter, "complete", payload);
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    sendError(emitter, startedAt, error);
                }
            });
        } catch (RuntimeException error) {
            sendError(emitter, startedAt, error);
        }

        return emitter;
    }

    private static long firstTokenLatency(long startedAt, long firstTokenAt) {
        return firstTokenAt == 0 ? -1 : firstTokenAt - startedAt;
    }

    private static void sendError(SseEmitter emitter, long startedAt, Throwable error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "Streaming failed");
        payload.put("error", error.getMessage());
        payload.put("total_latency_ms", System.currentTimeMillis() - startedAt);

        sendEvent(emitter, "error", payload);
        emitter.completeWithError(error);
    }

    private static void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }
}
