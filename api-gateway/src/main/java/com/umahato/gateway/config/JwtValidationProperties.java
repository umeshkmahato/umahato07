package com.umahato.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtValidationProperties(
        String issuer,
        String audience,
        String secret
) {
}
