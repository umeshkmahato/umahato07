package com.umahato.agent.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserAgentEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAgentEventConsumer.class);

    @KafkaListener(topics = "${app.kafka.topic:user-agent-events}", groupId = "${spring.application.name}-consumer")
    public void onEvent(String message) {
        LOGGER.info("Consumed user-agent event: {}", message);
    }
}
