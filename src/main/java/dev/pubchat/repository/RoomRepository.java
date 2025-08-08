package dev.pubchat.repository;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RoomRepository {

    private final List<String> rooms = new ArrayList<>();

    public void addRoom(String room) {
        rooms.add(room);
    }

    public boolean existsRoom(String room) {
        return rooms.contains(room);
    }

}
