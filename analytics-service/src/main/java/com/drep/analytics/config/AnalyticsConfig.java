package com.drep.analytics.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AnalyticsProperties.class, AuthProperties.class})
public class AnalyticsConfig {
}
