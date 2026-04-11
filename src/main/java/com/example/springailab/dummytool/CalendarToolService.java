package com.example.springailab.dummytool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class CalendarToolService extends DummyToolService {

    @Tool(description = "Schedule a meeting")
    public String scheduleMeeting(@ToolParam(description = "DateTime string") final String dateTime) {
        return "Meeting scheduled for "+ dateTime;
    }
}
