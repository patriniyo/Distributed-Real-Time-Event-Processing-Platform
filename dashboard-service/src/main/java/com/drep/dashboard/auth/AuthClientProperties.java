package com.drep.dashboard.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "drep.auth")
public class AuthClientProperties {

    private boolean enabled = true;
    private String analyticsUrl = "http://localhost:8100";
    private String internalToken = "internal-token";
    private String legacyAdminApiKey = "admin-key";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAnalyticsUrl() {
        return analyticsUrl;
    }

    public void setAnalyticsUrl(String analyticsUrl) {
        this.analyticsUrl = analyticsUrl;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public String getLegacyAdminApiKey() {
        return legacyAdminApiKey;
    }

    public void setLegacyAdminApiKey(String legacyAdminApiKey) {
        this.legacyAdminApiKey = legacyAdminApiKey;
    }
}
