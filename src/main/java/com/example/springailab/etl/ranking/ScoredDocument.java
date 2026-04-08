package com.example.springailab.etl.ranking;

import org.springframework.ai.document.Document;

public record ScoredDocument(Document document,
                             double relevanceScore) implements Comparable<ScoredDocument> {

    @Override
    public int compareTo(final ScoredDocument scoredDocument) {
        return Double.compare(
            scoredDocument.relevanceScore,
            this.relevanceScore
        ); // Sort descending by score
    }
}
