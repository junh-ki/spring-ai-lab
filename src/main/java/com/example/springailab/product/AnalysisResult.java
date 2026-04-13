package com.example.springailab.product;

import java.util.List;

public record AnalysisResult(String summary,
                             int sentimentScore,
                             List<String> features,
                             boolean isSafe) {}
