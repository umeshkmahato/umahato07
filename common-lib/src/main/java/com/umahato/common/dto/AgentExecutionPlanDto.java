package com.umahato.common.dto;

import java.util.List;

public record AgentExecutionPlanDto(
        String reasoning,
        List<String> steps
) {
}
