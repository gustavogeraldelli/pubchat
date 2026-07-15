package dev.pubchat.controller;

import dev.pubchat.repository.RoomRepository;
import dev.pubchat.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository repository;
    private final RateLimitService rateLimitService;

    public RoomController(RoomRepository repository, RateLimitService rateLimitService) {
        this.repository = repository;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<String> createRoom(HttpServletRequest request) {
        if (!rateLimitService.allowRoomCreation(clientIp(request)))
            return ResponseEntity.status(429).body("Too many rooms created. Try again later.");

        var room = UUID.randomUUID().toString();
        repository.add(room);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{room}/available")
    public ResponseEntity<Void> existsRoom(@PathVariable String room) {
        if (repository.exists(room))
            return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank())
            return forwardedFor.split(",")[0].trim();

        return request.getRemoteAddr();
    }

}
