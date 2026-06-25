package com.umahato.common.dto;

public record AgentQueryRequest(
        String sessionId,
        Long userId,
        String query
) {
}
