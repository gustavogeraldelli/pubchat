package dev.pubchat.controller;

import dev.pubchat.model.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, Message message) {
        String topic = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(topic, message);
    }
}
