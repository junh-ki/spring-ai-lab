package com.example.springailab.dummytool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class SqlToolService extends DummyToolService {

    @Tool(description = "Run SQL")
    public String runSQL(@ToolParam(description = "SQL string") final String sql) {
        return "Ran SQL: "+ sql;
    }
}
