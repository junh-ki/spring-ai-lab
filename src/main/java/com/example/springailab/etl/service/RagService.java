package com.example.springailab.etl.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagService {

    private final HydeRetrieverService hydeRetrieverService;
    private final ChatClient chatClient;

    public String answer(final String userQuery) {
        // 1. Retrieve using HyDE strategy
        final List<Document> relevantDocuments = this.hydeRetrieverService.search(userQuery);
        // 2. Construct the Context String
        final String context = relevantDocuments.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));
        // 3. Final Generation (Standard RAG), we feed the REAL documents + ORIGINAL query
        return this.chatClient.prompt()
            .system(promptSystemSpec ->
                promptSystemSpec
                    .text("""
                        You are an assistant. Answer the user's question using ONLY the context provided below.
                        
                        CONTEXT:
                        {context}
                        """)
                    .param("context", context)
            )
            .user(userQuery)
            .call()
            .content();
    }
}
