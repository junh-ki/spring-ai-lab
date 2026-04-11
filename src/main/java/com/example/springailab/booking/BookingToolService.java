package com.example.springailab.booking;

import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookingToolService {

    private static final Set<BookingInfo> STATIC_BOOKING_PERSISTENCE = Set.of(
        new BookingInfo(12345, "CONFIRMED", "Flight 101 to Tokyo"),
        new BookingInfo(99999, "PENDING", "Payment incomplete")
    );
    private record BookingInfo(Integer id,
                               String status,
                               String detail) {
        private String infoDetail() {
            return status() + " - " + detail();
        }
    }

    /**
     * The description tells the LLM "When" to use this.
     */
    @Tool(description = "Get the status of a specific booking by its ID. Returns 'CONFIRMED', 'PENDING', or 'MISSING'.")
    public String getBookingStatus(@ToolParam(description = "The 5-digit booking reference ID") final Integer bookingId) {
        log.info("AI requesting status for: {}", bookingId);
        return findBookingInfo(bookingId)
            .map(BookingInfo::infoDetail)
            .orElseGet(() -> "MISSING - No record found");
    }

    /**
     * In a real app, you should query a JPA Repository here.
     */
    private Optional<BookingInfo> findBookingInfo(final Integer bookingId) {
        return STATIC_BOOKING_PERSISTENCE.stream()
            .filter(bookingInfo -> bookingId.equals(bookingInfo.id()))
            .findAny();
    }
}
