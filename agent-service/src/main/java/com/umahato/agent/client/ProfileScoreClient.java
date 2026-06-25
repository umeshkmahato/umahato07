package com.umahato.agent.client;

import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProfileScoreClient {

    private final RestTemplate restTemplate;
    private final String externalScoreUrl;
    private final AtomicReference<String> lastCallSource = new AtomicReference<>("external");

    public ProfileScoreClient(
            RestTemplate restTemplate,
            @Value("${app.external.profile-score-url:http://localhost:8090/profile-score/{userId}}") String externalScoreUrl) {
        this.restTemplate = restTemplate;
        this.externalScoreUrl = externalScoreUrl;
    }

    @CircuitBreaker(name = "profileScore", fallbackMethod = "fallbackScore")
    public int fetchScore(Long userId) {
        Integer score = restTemplate.getForObject(externalScoreUrl, Integer.class, userId);
        if (score == null) {
            throw new IllegalStateException("External score API returned null score");
        }
        lastCallSource.set("external");
        return score;
    }

    public int fallbackScore(Long userId, Throwable throwable) {
        lastCallSource.set("fallback");
        return 50;
    }

    public String getLastCallSource() {
        return lastCallSource.get();
    }
}
