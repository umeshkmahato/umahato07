package com.umahato.agent.orchestrator;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockLanguageModelClient implements LanguageModelClient {

    private final ObjectMapper objectMapper;
    private final String provider;
    private final String model;

    public MockLanguageModelClient(
            ObjectMapper objectMapper,
            @Value("${app.genai.provider:mock}") String provider,
            @Value("${app.genai.model:mock-gpt}") String model) {
        this.objectMapper = objectMapper;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        List<String> steps = new ArrayList<>();
        steps.add("FETCH_USER");
        if (prompt.toLowerCase().contains("score")) {
            steps.add("FETCH_SCORE");
        }
        steps.add("UPDATE_SESSION");
        steps.add("PUBLISH_EVENT");

        String reasoning = "Using provider=" + provider + ", model=" + model
                + " (mocked) to resolve user context, optional score, session persistence, and event emission.";
        PlanPayload payload = new PlanPayload(reasoning, steps);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize mock LLM output", ex);
        }
    }

    private record PlanPayload(String reasoning, List<String> steps) {
    }
}
