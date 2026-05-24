package com.drep.ingestion.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String tenantId;
    private final int limitRps;

    public RateLimitExceededException(String tenantId, int limitRps) {
        super("Rate limit exceeded for tenant: " + tenantId);
        this.tenantId = tenantId;
        this.limitRps = limitRps;
    }

    public String getTenantId() {
        return tenantId;
    }

    public int getLimitRps() {
        return limitRps;
    }
}
