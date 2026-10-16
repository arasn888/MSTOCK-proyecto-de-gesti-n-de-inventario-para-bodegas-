package com.mstock.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mstock.jwt")
public record JwtProperties(String secret, long expiracionMinutos) {
}