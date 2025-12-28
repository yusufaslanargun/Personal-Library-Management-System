package com.example.plms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plms.auth")
public class AuthProperties {
    private String jwtSecret;
    private int tokenTtlHours = 12;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getTokenTtlHours() {
        return tokenTtlHours;
    }

    public void setTokenTtlHours(int tokenTtlHours) {
        this.tokenTtlHours = tokenTtlHours;
    }
}
