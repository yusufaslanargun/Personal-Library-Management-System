package com.example.plms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plms.external")
public class ExternalProviderProperties {
    private Provider googleBooks = new Provider();
    private Provider openLibrary = new Provider();
    private Provider omdb = new Provider();

    public Provider getGoogleBooks() {
        return googleBooks;
    }

    public void setGoogleBooks(Provider googleBooks) {
        this.googleBooks = googleBooks;
    }

    public Provider getOmdb() {
        return omdb;
    }

    public void setOmdb(Provider omdb) {
        this.omdb = omdb;
    }

    public Provider getOpenLibrary() {
        return openLibrary;
    }

    public void setOpenLibrary(Provider openLibrary) {
        this.openLibrary = openLibrary;
    }

    public static class Provider {
        private String baseUrl;
        private String apiKey;
        private int timeoutMs = 4000;
        private int rateLimitMs = 200;
        private boolean mock;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getRateLimitMs() {
            return rateLimitMs;
        }

        public void setRateLimitMs(int rateLimitMs) {
            this.rateLimitMs = rateLimitMs;
        }

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }
    }
}
