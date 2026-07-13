package dev.pubchat.controller;

import dev.pubchat.dto.ChatMessageRequest;
import dev.pubchat.dto.ChatMessageResponse;
import dev.pubchat.dto.TypingStatusRequest;
import dev.pubchat.model.Message;
import dev.pubchat.repository.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@MessageMapping("/chat/rooms")
public class ChatWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository repository;

    public ChatWsController(SimpMessagingTemplate messagingTemplate,  RoomRepository repository) {
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
    }

    @MessageMapping("/{roomId}/messages")
    public void sendMessage(@DestinationVariable String roomId, @Valid ChatMessageRequest request) {
        if (!roomId.equals("global") && !repository.exists(roomId))
            return;
        if (!roomId.equals("global"))
            repository.touch(roomId);
        Message message = Message.chat(request.sender().trim(), roomId, request.message().trim());
        String topic = "/topic/rooms/" + roomId;
        messagingTemplate.convertAndSend(topic, ChatMessageResponse.from(message));
    }

    @MessageMapping("/{roomId}/typing")
    public void sendTypingStatus(@DestinationVariable String roomId, @Valid TypingStatusRequest request) {
        if (roomId.equals("global") || !repository.exists(roomId))
            return;
        repository.touch(roomId);
        Message message = Message.typing(request.sender().trim(), roomId);
        String topic = "/topic/rooms/" + roomId;
        messagingTemplate.convertAndSend(topic, ChatMessageResponse.from(message));
    }
}
