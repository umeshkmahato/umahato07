package com.umahato.agent.client;

import com.umahato.common.dto.UserProfileDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserProfileDto getUserById(Long userId) {
        UserProfileDto response = restTemplate.getForObject("http://user-service/users/{id}", UserProfileDto.class, userId);
        if (response == null) {
            throw new IllegalStateException("User-service returned null payload for id=" + userId);
        }
        return response;
    }
}
