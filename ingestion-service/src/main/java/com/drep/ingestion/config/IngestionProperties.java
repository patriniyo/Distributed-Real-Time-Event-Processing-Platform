package com.drep.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {

    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private int defaultRateLimitRps = 1000;
    private Map<String, Integer> tenantRateLimits = new HashMap<>();
    private Map<String, String> apiKeys = new HashMap<>();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public int getDefaultRateLimitRps() {
        return defaultRateLimitRps;
    }

    public void setDefaultRateLimitRps(int defaultRateLimitRps) {
        this.defaultRateLimitRps = defaultRateLimitRps;
    }

    public Map<String, Integer> getTenantRateLimits() {
        return tenantRateLimits;
    }

    public void setTenantRateLimits(Map<String, Integer> tenantRateLimits) {
        this.tenantRateLimits = tenantRateLimits;
    }

    public Map<String, String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Map<String, String> apiKeys) {
        this.apiKeys = apiKeys;
    }

    public static class Jwt {
        private String secret;
        private String issuer = "drep-ingestion";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Admin {
        private String apiKey = "admin-key";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
