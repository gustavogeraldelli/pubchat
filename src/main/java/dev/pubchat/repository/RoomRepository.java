package dev.pubchat.repository;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomRepository {

    private final Set<String> rooms = ConcurrentHashMap.newKeySet();

    public void add(String room) {
        rooms.add(room);
    }

    public boolean exists(String room) {
        return rooms.contains(room);
    }

    public Set<String> getAll() {
        return Set.copyOf(rooms);
    }

}
