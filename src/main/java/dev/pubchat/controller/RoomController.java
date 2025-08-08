package dev.pubchat.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/pubchat")
public class RoomController {

    @PostMapping("/rooms")
    public String createRoom() {
        return UUID.randomUUID().toString();
    }

}
