package com.umahato.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AgentQueryRequest(
        @NotBlank(message = "sessionId is required")
        String sessionId,
        @NotNull(message = "userId is required")
        Long userId,
        @NotBlank(message = "query is required")
        String query
) {
}
