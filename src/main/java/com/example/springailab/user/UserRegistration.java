package com.example.springailab.user;

import jakarta.validation.constraints.Email;

public record UserRegistration(String username,
                               @Email String email,
                               int age) {}
