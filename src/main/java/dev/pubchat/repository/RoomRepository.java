package dev.pubchat.repository;

import dev.pubchat.model.Room;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RoomRepository {

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    public void add(String roomId) {
        rooms.putIfAbsent(roomId, new Room(roomId));
    }

    void add(Room room) {
        rooms.putIfAbsent(room.getId(), room);
    }

    public boolean exists(String roomId) {
        return rooms.containsKey(roomId);
    }

    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public void touch(String roomId) {
        findById(roomId).ifPresent(Room::touch);
    }
    public void deleteInactiveSince(Instant cutoff) {
        rooms.entrySet().removeIf(entry -> entry.getValue().getLastActivityAt().isBefore(cutoff));
    }

    public Set<String> getAll() {
        return Set.copyOf(rooms.keySet());
    }

}
