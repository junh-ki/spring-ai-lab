package com.example.springailab.diagnostic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticToolService {

    @Tool(description = "Run a slow diagnostic report.")
    public String runDiagnostic() {
        try {
            return CompletableFuture.supplyAsync(this::heavyWork)
                .get(5, TimeUnit.SECONDS);
        } catch (final TimeoutException timeoutException) {
            return "Error: The diagnostic tool timed out after 5 seconds. "
                + "This usually means the system is under load. "
                + "Please ask the user if they want to try a 'Quick Scan' instead.";
        } catch (final Exception exception) {
            return "Error: " + exception.getMessage();
        }
    }

    /**
     * Simulate a heavy diagnostic work
     */
    private String heavyWork() {
        return StringUtils.EMPTY;
    }
}
