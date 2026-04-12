package com.example.springailab.project;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class ProjectToolService {

    @Tool(description = """
        Queries JIRA for high-level project management tasks, agile sprints, and ticket tracking.
        Use this when the user asks about 'deadlines', 'tickets', or 'product managers'.
        Do NOT use this for code-specific questions.
        """)
    public String searchJira(final String query) {
        return "Jira results for: " + query;
    }

    @Tool(description = """
        Queries GITHUB for low-level source code, pull requests, and commit histroy.
        Use this ONLY when the user mentions 'code', 'bugs', 'commits', or 'PRs'.
        """)
    public String searchGithub(final String query) {
        return "GitHub results for: " + query;
    }
}
