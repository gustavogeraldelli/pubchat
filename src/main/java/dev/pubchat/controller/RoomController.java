package dev.pubchat.controller;

import dev.pubchat.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pubchat")
public class RoomController {

    private final RoomRepository repository;

    public RoomController(RoomRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/rooms")
    public String createRoom() {
        var room = UUID.randomUUID().toString();
        repository.addRoom(room);
        return room;
    }

    @GetMapping("/rooms/{room}/available")
    public ResponseEntity<Void> existsRoom(@PathVariable String room) {
        if (repository.existsRoom(room))
            return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }

}
