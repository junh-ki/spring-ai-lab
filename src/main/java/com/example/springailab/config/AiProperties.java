package com.example.springailab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat")
public record AiProperties(
    int memoryRetrieveSize,
    double creativeTemperature,
    double strictTemperature,
    String persona
) {}
