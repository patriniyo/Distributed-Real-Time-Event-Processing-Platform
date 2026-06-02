package com.drep.processor.config;

import com.drep.processor.auth.AuthClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthClientProperties.class)
public class ProcessorConfig {
}
