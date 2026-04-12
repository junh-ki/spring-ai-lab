package com.example.springailab.search;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UniversalSearchToolService {

    private final JiraSearchService jiraSearchService;
    private final WikiSearchService wikiSearchService;

    @Tool(description = """
        The primary search engine for the company.
        Use this for ANY information retrieval request.
        The system will automatically route the query to the correct database (Jira, Wiki, or Web).
        """)
    public String smartSearch(@ToolParam(description = "The search query") final String query,
                              @ToolParam(description = "The domain (optional): 'CODE', 'DOCS', or 'GENERAL'") final String domain) {
        if ("CODE".equalsIgnoreCase(domain)) {
            return this.jiraSearchService.search(query);
        } else if ("DOCS".equalsIgnoreCase(domain)) {
            return this.wikiSearchService.search(query);
        }
        return "Found in Jira: " + this.jiraSearchService.search(query)
            + "\nFound in Wiki: " + this.wikiSearchService.search(query);
    }
}
