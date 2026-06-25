package com.umahato.common.dto;

public record AgentQueryResponse(
        String sessionId,
        Long userId,
        String answer,
        String source,
        AgentExecutionPlanDto plan
) {
}
