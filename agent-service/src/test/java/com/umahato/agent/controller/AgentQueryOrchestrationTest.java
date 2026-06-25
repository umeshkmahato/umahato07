package com.umahato.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umahato.agent.client.ProfileScoreClient;
import com.umahato.agent.client.UserServiceClient;
import com.umahato.agent.orchestrator.AgentOrchestrator;
import com.umahato.agent.orchestrator.MockLanguageModelClient;
import com.umahato.agent.service.AgentService;
import com.umahato.common.dto.UserProfileDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentQueryOrchestrationTest {

    @Test
    void shouldRunAgentQueryWithPlanAndReturnStructuredResponse() throws Exception {
        UserServiceClient userServiceClient = Mockito.mock(UserServiceClient.class);
        ProfileScoreClient profileScoreClient = Mockito.mock(ProfileScoreClient.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(userServiceClient.getUserById(101L)).thenReturn(
                new UserProfileDto(101L, "alice@example.com", "Alice", "{\"theme\":\"dark\"}"));
        when(profileScoreClient.fetchScore(101L)).thenReturn(88);
        when(profileScoreClient.getLastCallSource()).thenReturn("external");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("last-query");
        when(kafkaTemplate.send(anyString(), eq("101"), anyString())).thenReturn(null);

        MockLanguageModelClient languageModelClient = new MockLanguageModelClient(objectMapper, "mock", "mock-gpt");
        AgentOrchestrator orchestrator = new AgentOrchestrator(languageModelClient, objectMapper);
        AgentService service = new AgentService(
                userServiceClient,
                profileScoreClient,
                orchestrator,
                redisTemplate,
                kafkaTemplate,
                objectMapper,
                "user-agent-events");
        AgentController controller = new AgentController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/agent/query")
                        .contentType("application/json")
                        .content("""
                                {
                                  "sessionId":"sess-101",
                                  "userId":101,
                                  "query":"summarize profile with score"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-101"))
                .andExpect(jsonPath("$.source").value("external"))
                .andExpect(jsonPath("$.plan.steps[0]").value("FETCH_USER"))
                .andExpect(jsonPath("$.plan.steps[1]").value("FETCH_SCORE"));
    }
}
