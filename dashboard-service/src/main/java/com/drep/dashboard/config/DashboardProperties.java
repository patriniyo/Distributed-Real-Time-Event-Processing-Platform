package com.drep.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "drep.dashboard")
public class DashboardProperties {

    private String apiKey = "admin-key";
    private Services services = new Services();
    private WebSocket webSocket = new WebSocket();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public static class Services {
        private String ingestion = "http://localhost:8098";
        private String streamProcessor = "http://localhost:8099";
        private String analytics = "http://localhost:8100";

        public String getIngestion() {
            return ingestion;
        }

        public void setIngestion(String ingestion) {
            this.ingestion = ingestion;
        }

        public String getStreamProcessor() {
            return streamProcessor;
        }

        public void setStreamProcessor(String streamProcessor) {
            this.streamProcessor = streamProcessor;
        }

        public String getAnalytics() {
            return analytics;
        }

        public void setAnalytics(String analytics) {
            this.analytics = analytics;
        }
    }

    public static class WebSocket {
        private int maxConnections = 1000;

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
    }
}
