package com.example.springailab.chat;

public class MissingChatOutputException extends RuntimeException {

    public MissingChatOutputException() {
        super("Failed to generate chat output from the target model");
    }
}
