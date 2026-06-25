package com.umahato.agent.orchestrator;

import java.util.List;

public record AgentExecutionPlan(
        String reasoning,
        List<AgentTool> steps
) {
}
