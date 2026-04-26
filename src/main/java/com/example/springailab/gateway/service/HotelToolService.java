package com.example.springailab.gateway.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class HotelToolService {

    @Tool(description = "Do something")
    public String doSomething() {
        return "Did something";
    }
}
