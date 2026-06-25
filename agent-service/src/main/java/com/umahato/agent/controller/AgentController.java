package com.umahato.agent.controller;

import com.umahato.agent.service.AgentService;
import com.umahato.common.dto.AgentQueryRequest;
import com.umahato.common.dto.AgentQueryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/query")
    public AgentQueryResponse query(@Valid @RequestBody AgentQueryRequest request) {
        return agentService.query(request);
    }
}
