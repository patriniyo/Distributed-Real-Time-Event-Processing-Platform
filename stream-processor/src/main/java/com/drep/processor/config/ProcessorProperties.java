package com.drep.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "drep.processor")
public class ProcessorProperties {

    private List<String> failEventTypes = new ArrayList<>();

    public List<String> getFailEventTypes() {
        return failEventTypes;
    }

    public void setFailEventTypes(List<String> failEventTypes) {
        this.failEventTypes = failEventTypes;
    }
}
