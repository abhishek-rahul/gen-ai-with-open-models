package com.javaone.openmodels.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ManualAgentService {

    private static final Pattern SKU_PATTERN = Pattern.compile("(?i)\\b[A-Z]{2,}-\\d+\\b");

    private final ChatLanguageModel chatModel;
    private final InventoryTools inventoryTools = new InventoryTools();
    private final int maxSteps;

    public ManualAgentService(ChatLanguageModel chatModel,
                              @Value("${agent.max-steps:5}") int maxSteps) {
        this.chatModel = chatModel;
        this.maxSteps = maxSteps;
    }

    public AgentConcepts concepts() {
        return new AgentConcepts(
            "An agent is an LLM-driven workflow that can reason about a goal, choose tools, observe results, and keep going until it can answer.",
            "The LLM plans and decides. Tools give it access to external actions such as inventory lookup, search, database queries, or APIs.",
            "This demo implements a manual agent loop in Java instead of hiding the loop inside an AI service.",
            List.of("Reason", "Act", "Observe"),
            "ReAct means the model writes a short reason, chooses an action, then uses the observation from that action in the next reasoning step.",
            "Multi-step reasoning happens when the agent needs more than one tool call or observation before it can produce a final answer.",
            "The loop stops when the model emits Final:, when the max step limit is reached, or when a repeated action suggests a loop.",
            "Tool selection is controlled by the prompt and a small Java dispatcher that only allows known tools.",
            List.of(
                "Unknown tool name",
                "Missing or bad tool input",
                "Tool returns no useful data",
                "Model never emits Final:",
                "Model repeats the same action"
            ),
            "The max-step limit and repeated-action guard prevent the agent from running forever."
        );
    }

    public AgentRun run(String goal) {
        List<AgentStep> steps = new ArrayList<>();
        List<String> scratchpad = new ArrayList<>();
        String previousActionKey = null;

        for (int stepNumber = 1; stepNumber <= maxSteps; stepNumber++) {
            String modelOutput = chatModel.chat(buildPrompt(goal, scratchpad));
            AgentDecision decision = parseDecision(modelOutput);

            if (decision.finalAnswer().isPresent()) {
                steps.add(AgentStep.finalStep(stepNumber, decision.thought(), decision.finalAnswer().get(), modelOutput));
                return AgentRun.completed(goal, maxSteps, steps, decision.finalAnswer().get());
            }

            if (decision.action().isEmpty()) {
                String observation = "Failure: the model did not provide Action: or Final:. Stopping the agent.";
                steps.add(AgentStep.failed(stepNumber, decision.thought(), observation, modelOutput));
                return AgentRun.failed(goal, maxSteps, steps, observation);
            }

            String actionName = decision.action().get();
            String actionInput = decision.actionInput().orElse("");
            String actionKey = actionName.toLowerCase(Locale.ROOT) + ":" + actionInput.toLowerCase(Locale.ROOT);
            if (actionKey.equals(previousActionKey)) {
                String observation = "Failure: repeated action detected. This protects the agent from an infinite loop.";
                steps.add(new AgentStep(stepNumber, decision.thought(), actionName, actionInput, observation, modelOutput, false));
                return AgentRun.failed(goal, maxSteps, steps, observation);
            }
            previousActionKey = actionKey;

            String observation = runTool(actionName, actionInput);
            steps.add(new AgentStep(stepNumber, decision.thought(), actionName, actionInput, observation, modelOutput, false));
            scratchpad.add("Thought: " + decision.thought());
            scratchpad.add("Action: " + actionName);
            scratchpad.add("Action Input: " + actionInput);
            scratchpad.add("Observation: " + observation);
        }

        String message = "Failure: max agent steps reached before Final:. Increase agent.max-steps only after checking the prompt and tool behavior.";
        return AgentRun.failed(goal, maxSteps, steps, message);
    }

    private String buildPrompt(String goal, List<String> scratchpad) {
        return """
            You are a small ReAct agent for a JavaOne demo.

            Goal: %s

            Available tools:
            - checkStock: input is a product SKU such as JDK-21, MVN-4, or GRAAL-22.
            - explainAgentConcept: input is one of agent, llm_tools, manual_loop, react, stopping, failures, infinite_loop.

            Rules:
            - Use this exact format for tool use:
              Thought: one short reason
              Action: checkStock OR explainAgentConcept
              Action Input: tool input
            - Use this exact format when done:
              Thought: one short reason
              Final: final answer for the user
            - Choose a tool only when it helps answer the goal.
            - Do not repeat the same action with the same input.

            Previous steps:
            %s

            Next step:
            """.formatted(goal, scratchpad.isEmpty() ? "(none)" : String.join("\n", scratchpad));
    }

    private AgentDecision parseDecision(String modelOutput) {
        String thought = findLineValue(modelOutput, "Thought").orElse("No explicit thought provided.");
        Optional<String> finalAnswer = findLineValue(modelOutput, "Final");
        Optional<String> action = findLineValue(modelOutput, "Action");
        Optional<String> actionInput = findLineValue(modelOutput, "Action Input");
        return new AgentDecision(thought, finalAnswer, action, actionInput);
    }

    private Optional<String> findLineValue(String text, String label) {
        String prefix = label + ":";
        return text.lines()
            .map(String::trim)
            .filter(line -> line.regionMatches(true, 0, prefix, 0, prefix.length()))
            .map(line -> line.substring(prefix.length()).trim())
            .filter(value -> !value.isBlank())
            .findFirst();
    }

    private String runTool(String actionName, String actionInput) {
        return switch (actionName.trim()) {
            case "checkStock" -> checkStock(actionInput);
            case "explainAgentConcept" -> explainAgentConcept(actionInput);
            default -> "Failure: unknown tool '%s'. Allowed tools are checkStock and explainAgentConcept.".formatted(actionName);
        };
    }

    private String checkStock(String actionInput) {
        Matcher matcher = SKU_PATTERN.matcher(actionInput);
        if (!matcher.find()) {
            return "Failure: checkStock needs a SKU like JDK-21, MVN-4, or GRAAL-22.";
        }
        return inventoryTools.checkStock(matcher.group().toUpperCase(Locale.ROOT));
    }

    private String explainAgentConcept(String concept) {
        Map<String, String> explanations = new LinkedHashMap<>();
        explanations.put("agent", "An agent is an LLM plus a loop that can select tools, observe results, and continue toward a goal.");
        explanations.put("llm_tools", "The LLM decides what is needed; Java tools perform bounded actions such as inventory lookup.");
        explanations.put("manual_loop", "A manual loop makes each Reason, Act, and Observe step visible in application code.");
        explanations.put("react", "ReAct is Reason -> Act -> Observe repeated until the agent can produce Final.");
        explanations.put("stopping", "Stopping conditions include Final:, max steps, malformed output, and repeated actions.");
        explanations.put("failures", "Common failures are wrong tool selection, invalid input, weak observations, and no final answer.");
        explanations.put("infinite_loop", "Infinite loops are controlled with max steps and repeated-action detection.");
        return explanations.getOrDefault(concept.trim().toLowerCase(Locale.ROOT),
            "Failure: unknown concept. Try agent, llm_tools, manual_loop, react, stopping, failures, or infinite_loop.");
    }

    private record AgentDecision(String thought,
                                 Optional<String> finalAnswer,
                                 Optional<String> action,
                                 Optional<String> actionInput) {
    }

    public record AgentConcepts(String agent,
                                String llmPlusTools,
                                String manualAgentLoop,
                                List<String> reasonActObserveLoop,
                                String reactPattern,
                                String multiStepReasoning,
                                String agentStoppingCondition,
                                String toolSelection,
                                List<String> agentFailureCases,
                                String infiniteLoopProblem) {
    }

    public record AgentRun(String goal,
                           String status,
                           int maxSteps,
                           int stepsUsed,
                           String stoppingCondition,
                           String answer,
                           List<AgentStep> trace) {

        static AgentRun completed(String goal, int maxSteps, List<AgentStep> trace, String answer) {
            return new AgentRun(goal, "completed", maxSteps, trace.size(), "Final answer emitted", answer, trace);
        }

        static AgentRun failed(String goal, int maxSteps, List<AgentStep> trace, String answer) {
            return new AgentRun(goal, "failed", maxSteps, trace.size(), answer, answer, trace);
        }
    }

    public record AgentStep(int step,
                            String thought,
                            String action,
                            String actionInput,
                            String observation,
                            String rawModelOutput,
                            boolean finalStep) {

        static AgentStep finalStep(int step, String thought, String answer, String rawModelOutput) {
            return new AgentStep(step, thought, "final", "", answer, rawModelOutput, true);
        }

        static AgentStep failed(int step, String thought, String observation, String rawModelOutput) {
            return new AgentStep(step, thought, "stop", "", observation, rawModelOutput, true);
        }
    }
}
