package com.example.springailab.room;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class RoomBookingToolService {

    @Tool(description = "Book a meeting. Date format: YYYY-MM-DD.")
    public String bookRoom(final String date,
                           final String time) {
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "Error: Invalid date format '" + date + "'. "
                + "You MUST use YYYY-MM-DD (e.g., 2024-12-31). "
                + "Please reformat and try again.";
        }
        if (time.startsWith("02")) {
            return "Error: The office is closed at " + time + ". "
                + "We are only open from 09:00 to 17:00.";
        }
        return "Success: Room booked for " + date + " at " + time;
    }
}
