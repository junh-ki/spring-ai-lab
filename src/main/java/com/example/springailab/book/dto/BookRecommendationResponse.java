package com.example.springailab.book.dto;

import java.util.List;

public record BookRecommendationResponse(String summary,
                                         List<BookRecommendation> bookRecommendations) {}
