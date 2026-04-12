package com.example.springailab.weather;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class RobustWeatherService {

    private final RestClient restClient;

    public RobustWeatherService(final RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.weather-provider.com")
            .build();
    }

    @Tool(description = "Get the weather forecast for a city.")
    public String getWeather(@ToolParam(description = "City name") final String city) {
        log.info("Fetching weather for: {}", city);
        try {
            return callExternalApi(city);
        } catch (final HttpClientErrorException httpClientErrorException) {
            // Case 1: The AI hallucinated a city or made a typo. We give a hint so the AI can correct itself.
            return String.format(
                "Error: The city '%s' was not found in the database. Please check the spelling or try a nearby major city.",
                city
            );
        } catch (final ResourceAccessException resourceAccessException) {
            // Case 2: Network timeout. We tell the AI this is temporary.
            return "Error: The weather service is currently unreachable (Network Timeout). Please apologize to the user and try again later.";
        } catch (final Exception exception) {
            // Case 3: Unknown failure. We log the real stack trace for devs, but hide it from the AI.
            log.error("Unexpected tool failure", exception);
            return "Error: An internal system error occurred. Do not retry this specific request.";
        }
    }

    /**
     * Simulate the API call logic
     */
    private String callExternalApi(final String city) {
        if ("Atlantis".equalsIgnoreCase(city)) {
            throw new HttpClientErrorException(
                HttpStatus.NOT_FOUND,
                "City not found"
            );
        }
        return "22°C and Sunny";
    }
}
