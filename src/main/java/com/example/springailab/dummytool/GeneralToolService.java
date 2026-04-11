package com.example.springailab.dummytool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class GeneralToolService extends DummyToolService {

    @Tool(description = "Generic operation")
    public String runOperation(@ToolParam(description = "Generic operation") final String operation) {
        return "Ran the generic operation: "+ operation;
    }
}
