package com.umahato.agent.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umahato.agent.client.ProfileScoreClient;
import com.umahato.agent.client.UserServiceClient;
import com.umahato.agent.orchestrator.AgentExecutionPlan;
import com.umahato.agent.orchestrator.AgentOrchestrator;
import com.umahato.agent.orchestrator.AgentTool;
import com.umahato.common.dto.AgentExecutionPlanDto;
import com.umahato.common.dto.AgentQueryRequest;
import com.umahato.common.dto.AgentQueryResponse;
import com.umahato.common.dto.UserAgentQueriedEvent;
import com.umahato.common.dto.UserProfileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final UserServiceClient userServiceClient;
    private final ProfileScoreClient profileScoreClient;
    private final AgentOrchestrator agentOrchestrator;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String agentEventsTopic;
    private final Duration sessionTtl = Duration.ofMinutes(30);

    public AgentService(UserServiceClient userServiceClient,
                        ProfileScoreClient profileScoreClient,
                        AgentOrchestrator agentOrchestrator,
                        StringRedisTemplate stringRedisTemplate,
                        KafkaTemplate<String, String> kafkaTemplate,
                        ObjectMapper objectMapper,
                        @Value("${app.kafka.topic:user-agent-events}") String agentEventsTopic) {
        this.userServiceClient = userServiceClient;
        this.profileScoreClient = profileScoreClient;
        this.agentOrchestrator = agentOrchestrator;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.agentEventsTopic = agentEventsTopic;
    }

    public AgentQueryResponse query(AgentQueryRequest request) {
        String sessionKey = "agent-session:" + request.sessionId();
        String previousPrompt = stringRedisTemplate.opsForValue().get(sessionKey);
        AgentExecutionPlan plan = agentOrchestrator.createPlan(request, previousPrompt);

        UserProfileDto user = null;
        Integer profileScore = null;
        boolean publishEvent = false;
        List<String> executedSteps = new ArrayList<>();

        for (AgentTool step : plan.steps()) {
            switch (step) {
                case FETCH_USER -> {
                    user = userServiceClient.getUserById(request.userId());
                    executedSteps.add(step.name());
                }
                case FETCH_SCORE -> {
                    profileScore = profileScoreClient.fetchScore(request.userId());
                    executedSteps.add(step.name());
                }
                case UPDATE_SESSION -> {
                    stringRedisTemplate.opsForValue().set(sessionKey, request.query(), sessionTtl);
                    executedSteps.add(step.name());
                }
                case PUBLISH_EVENT -> publishEvent = true;
            }
        }

        if (user == null) {
            user = userServiceClient.getUserById(request.userId());
            executedSteps.add(AgentTool.FETCH_USER.name());
        }

        String answer = buildAnswer(request.query(), user, profileScore, previousPrompt, plan.reasoning());
        if (publishEvent) {
            publishEvent(request, "SUCCESS", profileScoreClient.getLastCallSource());
            executedSteps.add(AgentTool.PUBLISH_EVENT.name());
        }

        return new AgentQueryResponse(
                request.sessionId(),
                request.userId(),
                answer,
                profileScoreClient.getLastCallSource(),
                new AgentExecutionPlanDto(plan.reasoning(), executedSteps));
    }

    private String buildAnswer(String query, UserProfileDto user, Integer score, String previousPrompt, String reasoning) {
        String previousContext = previousPrompt == null ? "none" : previousPrompt;
        String resolvedScore = score == null ? "not-requested" : String.valueOf(score);
        return "Processed query='" + query + "' for user='" + user.name()
                + "', score=" + resolvedScore + ", previousSessionPrompt=" + previousContext
                + ", reasoning=" + reasoning;
    }

    private void publishEvent(AgentQueryRequest request, String status, String source) {
        UserAgentQueriedEvent event = new UserAgentQueriedEvent(
                UUID.randomUUID().toString(),
                "UserAgentQueried",
                Instant.now(),
                request.sessionId(),
                request.userId(),
                request.query(),
                status,
                source);

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(agentEventsTopic, String.valueOf(request.userId()), eventJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka event", ex);
        }
    }
}
