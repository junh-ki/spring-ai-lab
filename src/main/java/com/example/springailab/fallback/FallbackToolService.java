package com.example.springailab.fallback;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class FallbackToolService {

    @Tool(description = "Call this when the user is vague. Provide a list of possible topics based on their keywords.")
    public String suggestTopics(final String userKeywords) {
        return "I found these related topics: " // In a real app, this might search a FAQ vector store
            + "1. Login Problems "
            + "2. Fraud Reporting "
            + "3. Mobile App Bugs. "
            + "Did you mean one of these?";
    }
}
