package dev.pubchat.controller;

import dev.pubchat.model.Message;
import dev.pubchat.repository.RoomRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository repository;

    public ChatController(SimpMessagingTemplate messagingTemplate,  RoomRepository repository) {
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
    }

    @MessageMapping("/room/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, Message message) {
        if (!roomId.equals("global") && !repository.existsRoom(roomId))
            return;
        String topic = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(topic, message);
    }
}
