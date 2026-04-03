package com.example.springailab.search;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchHelper {

    private final VectorStore vectorStore;

    public List<Document> dynamicSearch(final String query,
                                        final String user,
                                        final List<String> categories) {
        final FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        final Filter.Expression filter = filterExpressionBuilder
            .and(
                filterExpressionBuilder.eq("author", user),
                filterExpressionBuilder.in("category", categories.toArray(new Object[0]))
            )
            .build();
        return this.vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .filterExpression(filter)
                .build()
        );
    }
}
