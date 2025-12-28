package com.example.plms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plms.sync")
public class SyncProperties {
    private boolean enabled;
    private String endpoint;
    private String apiKey;
    private int retryMax = 3;
    private int timeoutMs = 4000;
    private boolean flushOnShutdown = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getRetryMax() {
        return retryMax;
    }

    public void setRetryMax(int retryMax) {
        this.retryMax = retryMax;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isFlushOnShutdown() {
        return flushOnShutdown;
    }

    public void setFlushOnShutdown(boolean flushOnShutdown) {
        this.flushOnShutdown = flushOnShutdown;
    }
}
