package com.example.springailab.gateway.service;

public interface DomainAgent {

    String getName();
    String process(final String userRequest);
}
