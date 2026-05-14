package com.javaone.openmodels.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Service
public class LangGraphWorkflowService {

    private static final String START = "START";
    private static final String END = "END";

    private final ChatLanguageModel chatModel;
    private final ContentRetriever contentRetriever;
    private final InventoryTools inventoryTools = new InventoryTools();
    private final Map<String, GraphState> checkpointer = new ConcurrentHashMap<>();
    private final Map<String, GraphNode> nodes;
    private final Map<String, List<GraphEdge>> edges;

    public LangGraphWorkflowService(ChatLanguageModel chatModel,
                                    ContentRetriever contentRetriever) {
        this.chatModel = chatModel;
        this.contentRetriever = contentRetriever;
        this.nodes = buildNodes();
        this.edges = buildEdges();
    }

    public Map<String, Object> run(String sessionId, String userInput, boolean approved) {
        // [ 1. LangGraph kya hai ] LangGraph ek stateful graph pattern hai jahan agent ka kaam nodes aur edges me model hota hai.
        GraphState previousState = checkpointer.get(sessionId);
        GraphState state = GraphState.start(sessionId, userInput, approved, previousState);

        // [ 15. START / END ] START graph ka fixed entry point hai; END par workflow stop hota hai.
        String current = START;
        while (!END.equals(current)) {
            Optional<String> routedNode = nextNode(current, state);
            if (routedNode.isEmpty()) {
                throw new IllegalStateException("No edge found from node: " + current);
            }
            current = routedNode.get();

            if (END.equals(current)) {
                state = state.addTrace("END");
                break;
            }

            state = executeWithRetry(nodes.get(current), state);
            checkpointer.put(sessionId, state);
        }

        checkpointer.put(sessionId, state);
        return state.toResponse();
    }

    public Map<String, Object> state(String sessionId) {
        return checkpointer.getOrDefault(sessionId, GraphState.empty(sessionId)).toResponse();
    }

    private Map<String, GraphNode> buildNodes() {
        Map<String, GraphNode> graphNodes = new LinkedHashMap<>();
        graphNodes.put("planner", this::plannerNode);
        graphNodes.put("humanApproval", this::humanApprovalNode);
        graphNodes.put("retriever", this::retrieverNode);
        graphNodes.put("tool", this::toolNode);
        graphNodes.put("executor", this::executorNode);
        graphNodes.put("validator", this::validatorNode);
        return graphNodes;
    }

    private Map<String, List<GraphEdge>> buildEdges() {
        Map<String, List<GraphEdge>> graphEdges = new LinkedHashMap<>();

        // [ 5. Edges ] Edge define karta hai ki node A ke baad node B chalega.
        // [ 13. Edges ] START ke baad planner fixed edge se execute hota hai.
        graphEdges.put(START, List.of(new GraphEdge("planner", state -> true)));

        // [ 6. Conditional flow ] Predicate ke basis par graph alag route choose karta hai.
        // [ 14. Conditional Edges ] Risky request approval node par jati hai; tool request tool node par jati hai.
        graphEdges.put("planner", List.of(
            new GraphEdge("humanApproval", GraphState::requiresApproval),
            new GraphEdge("retriever", state -> true)
        ));
        graphEdges.put("humanApproval", List.of(
            new GraphEdge("retriever", GraphState::approved),
            new GraphEdge("validator", state -> !state.approved())
        ));
        graphEdges.put("retriever", List.of(
            new GraphEdge("tool", GraphState::needsTool),
            new GraphEdge("executor", state -> true)
        ));
        graphEdges.put("tool", List.of(new GraphEdge("executor", state -> true)));
        graphEdges.put("executor", List.of(new GraphEdge("validator", state -> true)));
        graphEdges.put("validator", List.of(
            new GraphEdge(END, GraphState::answerReady),
            new GraphEdge("executor", state -> !state.answerReady())
        ));
        return graphEdges;
    }

    private Optional<String> nextNode(String current, GraphState state) {
        return edges.getOrDefault(current, List.of()).stream()
            .filter(edge -> edge.condition().test(state))
            .map(GraphEdge::to)
            .findFirst();
    }

    private GraphState executeWithRetry(GraphNode node, GraphState state) {
        RuntimeException lastError = null;

        // [ 7. Retry flow ] Failed node ko max do baar retry kiya jata hai before graceful error answer.
        // [ 22. Error Handling / Retry ] API failures, retrieval failures, ya tool errors state.errors me capture hote hain.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                GraphState attemptedState = state.withRetryCount(attempt);
                return node.apply(attemptedState);
            } catch (RuntimeException error) {
                lastError = error;
                state = state.addError(error.getMessage());
            }
        }

        return state.withFinalAnswer(
            "Workflow failed after retries: " + (lastError == null ? "unknown error" : lastError.getMessage())
        ).withAnswerReady(true);
    }

    private GraphState plannerNode(GraphState state) {
        // [ 3. Stateful agent workflow ] Planner previous state aur current input dono dekh kar next action plan karta hai.
        // [ 8. Planner/executor workflow ] Planner decide karta hai ki retrieval, tool, approval, ya direct answer path chahiye.
        String input = state.userInput().toLowerCase();
        boolean needsTool = input.contains("stock") || input.contains("inventory") || input.contains("sku");
        boolean riskyAction = input.contains("delete") || input.contains("restart") || input.contains("deploy")
            || input.contains("payment") || input.contains("refund");
        String plan = runPlannerSubgraph(state.userInput(), needsTool, riskyAction);

        return state
            .withPlan(plan)
            .withNeedsTool(needsTool)
            .withRequiresApproval(riskyAction)
            .addTrace("planner");
    }

    private GraphState humanApprovalNode(GraphState state) {
        // [ 20. Human-in-the-loop ] Risky action par graph user approval ka gate lagata hai.
        if (!state.approved()) {
            return state
                .withFinalAnswer("Approval required before executing this risky workflow. Retry with approved=true.")
                .withAnswerReady(true)
                .addTrace("humanApproval:blocked");
        }
        return state.addTrace("humanApproval:approved");
    }

    private GraphState retrieverNode(GraphState state) {
        // [ 12. Nodes ] Retriever node ek function hai jo RAG context fetch karta hai.
        List<Content> contents = contentRetriever.retrieve(new Query(state.userInput()));
        List<String> docs = contents.stream()
            .map(content -> content.textSegment().text())
            .limit(3)
            .toList();

        // [ 16. Reducers ] retrieved_docs replace hote hain, messages append reducer se grow hoti hain.
        return state.withRetrievedDocs(docs)
            .appendMessage("retriever", "retrieved_docs=" + docs.size())
            .addTrace("retriever");
    }

    private GraphState toolNode(GraphState state) {
        // [ 17. Tool Calling ] Tool ko LLM ke free-form output se nahi, graph ke controlled node se execute kiya jata hai.
        String sku = extractSku(state.userInput()).orElse("JDK-21");
        String toolResult = inventoryTools.checkStock(sku);
        return state.withToolResult(toolResult)
            .appendMessage("tool", toolResult)
            .addTrace("tool:" + sku);
    }

    private GraphState executorNode(GraphState state) {
        // [ 18. Agent Loop ] LLM -> tool/retrieval -> LLM -> final answer ka loop yahan complete hota hai.
        String prompt = """
            You are running a LangGraph-style workflow for a Java team.
            User input: %s
            Plan: %s
            Retrieved docs: %s
            Tool result: %s
            Previous messages: %s

            Give a concise final answer. Mention when the answer used retrieved docs or tool output.
            """.formatted(
            state.userInput(),
            state.plan(),
            state.retrievedDocs(),
            state.toolResult(),
            state.messages()
        );

        String answer = runExecutorSubgraph(prompt);
        return state.withFinalAnswer(answer)
            .appendMessage("assistant", answer)
            .addTrace("executor");
    }

    private GraphState validatorNode(GraphState state) {
        // [ 9. Agent control ] Validator node decides whether graph can end or executor must try again.
        boolean ready = state.finalAnswer() != null && state.finalAnswer().trim().length() >= 20;
        if (!ready && state.retryCount() >= 2) {
            return state.withFinalAnswer("I could not produce a valid answer after retries.")
                .withAnswerReady(true)
                .addTrace("validator:forced-end");
        }
        return state.withAnswerReady(ready)
            .addTrace(ready ? "validator:ready" : "validator:retry");
    }

    private String runPlannerSubgraph(String userInput, boolean needsTool, boolean riskyAction) {
        // [ 21. Subgraphs ] Planner logic ko chhote subgraph jaisa isolate kiya gaya hai.
        List<String> steps = new ArrayList<>();
        steps.add("read user input");
        steps.add("retrieve project context");
        if (needsTool) {
            steps.add("execute inventory tool");
        }
        if (riskyAction) {
            steps.add("wait for human approval");
        }
        steps.add("generate and validate final answer");
        return "Planner subgraph for '%s': %s".formatted(userInput, String.join(" -> ", steps));
    }

    private String runExecutorSubgraph(String prompt) {
        // [ 8. Planner/executor workflow ] Executor consumes planner output and performs the LLM answer step.
        return chatModel.chat(prompt);
    }

    private Optional<String> extractSku(String input) {
        String[] tokens = input.split("[^A-Za-z0-9.-]+");
        for (String token : tokens) {
            if (token.matches("[A-Za-z]+-[0-9]+")) {
                return Optional.of(token.toUpperCase());
            }
        }
        return Optional.empty();
    }

    @FunctionalInterface
    private interface GraphNode {
        // [ 4. Nodes ] Har node ek function hai jo shared GraphState leta hai aur updated GraphState return karta hai.
        GraphState apply(GraphState state);
    }

    private record GraphEdge(String to, Predicate<GraphState> condition) {}

    public record GraphState(
        String sessionId,
        String userInput,
        List<String> messages,
        List<String> retrievedDocs,
        String plan,
        String toolResult,
        String finalAnswer,
        boolean needsTool,
        boolean answerReady,
        boolean requiresApproval,
        boolean approved,
        int retryCount,
        List<String> trace,
        List<String> errors,
        Instant updatedAt
    ) {

        static GraphState start(String sessionId, String userInput, boolean approved, GraphState previousState) {
            // [ 11. State ] State graph ka shared data structure hai: messages, user_input, retrieved_docs, final_answer.
            List<String> messages = previousState == null ? new ArrayList<>() : new ArrayList<>(previousState.messages());
            messages.add("user: " + userInput);
            return new GraphState(
                sessionId,
                userInput,
                List.copyOf(messages),
                List.of(),
                "",
                "",
                "",
                false,
                false,
                false,
                approved,
                0,
                List.of("START"),
                List.of(),
                Instant.now()
            );
        }

        static GraphState empty(String sessionId) {
            return new GraphState(sessionId, "", List.of(), List.of(), "", "", "", false, false,
                false, false, 0, List.of(), List.of(), Instant.now());
        }

        GraphState withPlan(String value) {
            return copy(messages, retrievedDocs, value, toolResult, finalAnswer, needsTool, answerReady,
                requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState withNeedsTool(boolean value) {
            return copy(messages, retrievedDocs, plan, toolResult, finalAnswer, value, answerReady,
                requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState withRequiresApproval(boolean value) {
            return copy(messages, retrievedDocs, plan, toolResult, finalAnswer, needsTool, answerReady,
                value, approved, retryCount, trace, errors);
        }

        GraphState withRetrievedDocs(List<String> value) {
            return copy(messages, List.copyOf(value), plan, toolResult, finalAnswer, needsTool, answerReady,
                requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState withToolResult(String value) {
            return copy(messages, retrievedDocs, plan, value, finalAnswer, needsTool, answerReady,
                requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState withFinalAnswer(String value) {
            return copy(messages, retrievedDocs, plan, toolResult, value, needsTool, answerReady,
                requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState withAnswerReady(boolean value) {
            return copy(messages, retrievedDocs, plan, toolResult, finalAnswer, needsTool, value,
                requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState withRetryCount(int value) {
            return copy(messages, retrievedDocs, plan, toolResult, finalAnswer, needsTool, answerReady,
                requiresApproval, approved, value, trace, errors);
        }

        GraphState appendMessage(String role, String content) {
            // [ 16. Reducers ] Message reducer old messages me new message append karta hai.
            List<String> updatedMessages = new ArrayList<>(messages);
            updatedMessages.add(role + ": " + content);
            return copy(List.copyOf(updatedMessages), retrievedDocs, plan, toolResult, finalAnswer,
                needsTool, answerReady, requiresApproval, approved, retryCount, trace, errors);
        }

        GraphState addTrace(String value) {
            List<String> updatedTrace = new ArrayList<>(trace);
            updatedTrace.add(value);
            return copy(messages, retrievedDocs, plan, toolResult, finalAnswer, needsTool, answerReady,
                requiresApproval, approved, retryCount, List.copyOf(updatedTrace), errors);
        }

        GraphState addError(String value) {
            List<String> updatedErrors = new ArrayList<>(errors);
            updatedErrors.add(value == null ? "unknown error" : value);
            return copy(messages, retrievedDocs, plan, toolResult, finalAnswer, needsTool, answerReady,
                requiresApproval, approved, retryCount, trace, List.copyOf(updatedErrors));
        }

        Map<String, Object> toResponse() {
            // [ 19. Checkpointer / Persistence ] Response me persisted state expose hoti hai for demo inspection.
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("session_id", sessionId);
            response.put("user_input", userInput);
            response.put("plan", plan);
            response.put("messages", messages);
            response.put("retrieved_docs", retrievedDocs);
            response.put("tool_result", toolResult);
            response.put("final_answer", finalAnswer);
            response.put("needs_tool", needsTool);
            response.put("requires_approval", requiresApproval);
            response.put("approved", approved);
            response.put("answer_ready", answerReady);
            response.put("retry_count", retryCount);
            response.put("trace", trace);
            response.put("errors", errors);
            response.put("updated_at", updatedAt.toString());
            return response;
        }

        private GraphState copy(List<String> messages,
                                List<String> retrievedDocs,
                                String plan,
                                String toolResult,
                                String finalAnswer,
                                boolean needsTool,
                                boolean answerReady,
                                boolean requiresApproval,
                                boolean approved,
                                int retryCount,
                                List<String> trace,
                                List<String> errors) {
            return new GraphState(sessionId, userInput, messages, retrievedDocs, plan, toolResult, finalAnswer,
                needsTool, answerReady, requiresApproval, approved, retryCount, trace, errors, Instant.now());
        }
    }
}
