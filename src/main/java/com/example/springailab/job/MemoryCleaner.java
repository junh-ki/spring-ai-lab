package com.example.springailab.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryCleaner {

    private final ChatMemory chatMemory;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void evictOldSessions() {
        if (this.chatMemory == null) {
            return;
        }
        log.info("Running memory eviction...");
        // TODO: Custom logic to iterate keys and remove stale conversation would go here.
        //   * Note: Standard InMemoryChatMemory doesn't expose 'keySet()' publicly,
        //           so creating a wrapper class is often the best approach for production usage.
    }
}
