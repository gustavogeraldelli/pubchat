package dev.pubchat.model;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {

    private final String id;
    private final Instant createdAt;
    private volatile Instant lastActivityAt;
    private final Set<String> participants = ConcurrentHashMap.newKeySet();

    public Room(String id) {
        this(id, Instant.now());
    }

    public Room(String id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastActivityAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public Set<String> getParticipants() {
        return Set.copyOf(participants);
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public void addParticipant(String nickname) {
        participants.add(nickname);
        touch();
    }
}
