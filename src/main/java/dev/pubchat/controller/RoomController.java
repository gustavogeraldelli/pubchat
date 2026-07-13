package dev.pubchat.controller;

import dev.pubchat.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository repository;

    public RoomController(RoomRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public String createRoom() {
        var room = UUID.randomUUID().toString();
        repository.add(room);
        return room;
    }

    @GetMapping("/{room}/available")
    public ResponseEntity<Void> existsRoom(@PathVariable String room) {
        if (repository.exists(room))
            return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }

}
