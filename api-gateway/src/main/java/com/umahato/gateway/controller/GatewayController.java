package com.umahato.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {

    @GetMapping("/internal/gateway/health")
    public String health() {
        return "ok";
    }
}
