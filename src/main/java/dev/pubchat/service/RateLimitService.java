package dev.pubchat.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitService {

    private static final int MAX_ROOMS_PER_WINDOW = 5;
    private static final int MAX_MESSAGES_PER_WINDOW = 10;
    private static final Duration ROOM_CREATION_WINDOW = Duration.ofMinutes(1);
    private static final Duration MESSAGE_WINDOW = Duration.ofSeconds(10);

    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> roomCreationAttempts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WindowCounter> messageAttempts = new ConcurrentHashMap<>();

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public boolean allowRoomCreation(String clientId) {
        return allow(roomCreationAttempts, clientId, MAX_ROOMS_PER_WINDOW, ROOM_CREATION_WINDOW);
    }

    public boolean allowMessage(String sessionId) {
        return allow(messageAttempts, sessionId, MAX_MESSAGES_PER_WINDOW, MESSAGE_WINDOW);
    }

    private boolean allow(ConcurrentMap<String, WindowCounter> counters, String key, int maxAttempts, Duration window) {
        String counterKey = key == null || key.isBlank() ? "unknown" : key;
        Instant now = clock.instant();
        WindowCounter counter = counters.computeIfAbsent(counterKey, ignored -> new WindowCounter(now));

        synchronized (counter) {
            if (!now.isBefore(counter.windowStartedAt.plus(window))) {
                counter.windowStartedAt = now;
                counter.attempts = 0;
            }

            if (counter.attempts >= maxAttempts)
                return false;

            counter.attempts++;
            return true;
        }
    }

    private static class WindowCounter {
        private Instant windowStartedAt;
        private int attempts;

        private WindowCounter(Instant windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}
