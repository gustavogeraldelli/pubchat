package dev.pubchat.job;

import dev.pubchat.repository.RoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RoomCleanupJob {

    private final RoomRepository roomRepository;

    public RoomCleanupJob(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Scheduled(fixedRateString = "${pubchat.rooms.cleanup-rate-ms:60000}")
    public void removeInactiveRooms() {
        Duration maxIdleTime = Duration.ofMinutes(30);
        roomRepository.deleteInactiveSince(Instant.now().minus(maxIdleTime));
    }
}
