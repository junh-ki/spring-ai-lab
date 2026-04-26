package com.example.springailab.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayToolService {

    private final FlightAgent flightAgent;
    private final HotelAgent hotelAgent;

    @Tool(description = "Consult the Flight Specialist. Use this for queries about airplane tickets, airports, or flight status.")
    public String consultFlightAgent(@ToolParam(description = "The user's full request") final String request) {
        log.info("Gateway delegating to Flight Agent...");
        return this.flightAgent.process(request);
    }

    @Tool(description = "Consult the Hotel Specialist. Use this for accommodation, room bookings, or check-in questions.")
    public String consultHotelAgent(@ToolParam(description = "The user's full request") final String request) {
        log.info("Gateway delegating to Hotel Agent...");
        return this.hotelAgent.process(request);
    }
}
