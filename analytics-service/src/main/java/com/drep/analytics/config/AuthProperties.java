package com.drep.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "drep.auth")
public class AuthProperties {

    private boolean enabled = true;
    private String legacyAdminApiKey = "admin-key";
    private String internalToken = "internal-token";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLegacyAdminApiKey() {
        return legacyAdminApiKey;
    }

    public void setLegacyAdminApiKey(String legacyAdminApiKey) {
        this.legacyAdminApiKey = legacyAdminApiKey;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
