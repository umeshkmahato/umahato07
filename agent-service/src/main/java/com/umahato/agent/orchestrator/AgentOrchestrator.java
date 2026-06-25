package com.umahato.agent.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umahato.common.dto.AgentQueryRequest;
import org.springframework.stereotype.Component;

@Component
public class AgentOrchestrator {

    private final LanguageModelClient languageModelClient;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(LanguageModelClient languageModelClient, ObjectMapper objectMapper) {
        this.languageModelClient = languageModelClient;
        this.objectMapper = objectMapper;
    }

    public AgentExecutionPlan createPlan(AgentQueryRequest request, String previousContext) {
        String prompt = buildPrompt(request, previousContext);
        String modelOutput = languageModelClient.generate(prompt);
        return parsePlan(modelOutput);
    }

    private String buildPrompt(AgentQueryRequest request, String previousContext) {
        String context = previousContext == null ? "none" : previousContext;
        return """
                Build a tool plan for this query.
                sessionId=%s
                userId=%s
                query=%s
                previousContext=%s
                Allowed tools: FETCH_USER, FETCH_SCORE, UPDATE_SESSION, PUBLISH_EVENT
                Return JSON with fields: reasoning, steps.
                """.formatted(request.sessionId(), request.userId(), request.query(), context);
    }

    private AgentExecutionPlan parsePlan(String modelOutput) {
        try {
            Map<String, Object> payload = objectMapper.readValue(modelOutput, new TypeReference<>() {
            });
            String reasoning = String.valueOf(payload.getOrDefault("reasoning", "No reasoning provided by model."));
            Object rawSteps = payload.get("steps");
            if (!(rawSteps instanceof List<?> rawStepList)) {
                return fallbackPlan("Model output missing steps.");
            }
            List<AgentTool> steps = new ArrayList<>();
            for (Object rawStep : rawStepList) {
                steps.add(AgentTool.valueOf(String.valueOf(rawStep)));
            }
            return new AgentExecutionPlan(reasoning, steps);
        } catch (Exception ex) {
            return fallbackPlan("Model output parsing failed; using fallback plan.");
        }
    }

    private AgentExecutionPlan fallbackPlan(String reasoning) {
        return new AgentExecutionPlan(reasoning, List.of(
                AgentTool.FETCH_USER,
                AgentTool.FETCH_SCORE,
                AgentTool.UPDATE_SESSION,
                AgentTool.PUBLISH_EVENT));
    }
}
