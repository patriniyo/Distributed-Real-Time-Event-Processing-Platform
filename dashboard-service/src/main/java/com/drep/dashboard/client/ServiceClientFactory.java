package com.drep.dashboard.client;

import com.drep.dashboard.config.DashboardProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ServiceClientFactory {

    private final DashboardProperties properties;
    private final RestClient.Builder restClientBuilder;

    public ServiceClientFactory(DashboardProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public RestClient analyticsClient() {
        return client(properties.getServices().getAnalytics());
    }

    public RestClient streamProcessorClient() {
        return client(properties.getServices().getStreamProcessor());
    }

    public RestClient ingestionClient() {
        return client(properties.getServices().getIngestion());
    }

    private RestClient client(String baseUrl) {
        return restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-Key", properties.getApiKey())
                .build();
    }
}
