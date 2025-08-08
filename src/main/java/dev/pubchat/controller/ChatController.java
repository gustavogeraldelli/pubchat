package dev.pubchat.controller;

import dev.pubchat.model.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @MessageMapping("/chat")
    @SendTo("/topic/global")
    public Message sendMessage(Message message) {
        return message;
    }
}
