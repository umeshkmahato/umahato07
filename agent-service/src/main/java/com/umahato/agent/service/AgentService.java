package com.umahato.agent.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umahato.agent.client.ProfileScoreClient;
import com.umahato.agent.client.UserServiceClient;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String agentEventsTopic;
    private final Duration sessionTtl = Duration.ofMinutes(30);

    public AgentService(UserServiceClient userServiceClient,
                        ProfileScoreClient profileScoreClient,
                        StringRedisTemplate stringRedisTemplate,
                        KafkaTemplate<String, String> kafkaTemplate,
                        ObjectMapper objectMapper,
                        @Value("${app.kafka.topic:user-agent-events}") String agentEventsTopic) {
        this.userServiceClient = userServiceClient;
        this.profileScoreClient = profileScoreClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.agentEventsTopic = agentEventsTopic;
    }

    public AgentQueryResponse query(AgentQueryRequest request) {
        UserProfileDto user = userServiceClient.getUserById(request.userId());
        String sessionKey = "agent-session:" + request.sessionId();
        String previousPrompt = stringRedisTemplate.opsForValue().get(sessionKey);

        int profileScore = profileScoreClient.fetchScore(request.userId());
        String answer = buildAnswer(request.query(), user, profileScore, previousPrompt);

        stringRedisTemplate.opsForValue().set(sessionKey, request.query(), sessionTtl);
        publishEvent(request, "SUCCESS", profileScoreClient.getLastCallSource());

        return new AgentQueryResponse(
                request.sessionId(),
                request.userId(),
                answer,
                profileScoreClient.getLastCallSource());
    }

    private String buildAnswer(String query, UserProfileDto user, int score, String previousPrompt) {
        String previousContext = previousPrompt == null ? "none" : previousPrompt;
        return "Processed query='" + query + "' for user='" + user.name()
                + "', score=" + score + ", previousSessionPrompt=" + previousContext;
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
