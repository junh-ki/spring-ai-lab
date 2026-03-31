package com.example.springailab;

import com.example.springailab.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class SpringAiLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiLabApplication.class, args);
    }
}
