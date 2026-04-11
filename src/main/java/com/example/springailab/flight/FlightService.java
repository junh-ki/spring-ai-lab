package com.example.springailab.flight;

import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class FlightService {

    private static final Set<FlightInfo> STATIC_FLIGHT_PERSISTENCE = Set.of(
        new FlightInfo("BA123", "Toronto", 450.00),
        new FlightInfo("AF456", "Berlin", 410.50),
        new FlightInfo("CA312", "Toronto", 463.70)
    );

    @Tool(description = "Search for available flights by destination")
    public List<FlightInfo> findFlights(@ToolParam(description = "Target city") final String destination) {
        return STATIC_FLIGHT_PERSISTENCE.stream()
            .filter(flightInfo -> destination.equals(flightInfo.destination()))
            .toList();
    }
}
