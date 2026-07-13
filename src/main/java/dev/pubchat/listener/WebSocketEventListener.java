package dev.pubchat.listener;

import dev.pubchat.controller.ChatWsController;
import dev.pubchat.dto.ChatMessageResponse;
import dev.pubchat.model.Message;
import dev.pubchat.repository.RoomRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final String GLOBAL_ROOM = "global";

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository repository;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, RoomRepository repository) {
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();

        if (sessionAttributes == null)
            return;

        String roomId = (String) sessionAttributes.get(ChatWsController.SESSION_ROOM_ID);
        String nickname = (String) sessionAttributes.get(ChatWsController.SESSION_NICKNAME);

        if (roomId == null || nickname == null)
            return;

        if (!GLOBAL_ROOM.equals(roomId))
            repository.removeParticipant(roomId, nickname);

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId,
                ChatMessageResponse.from(Message.leave(nickname, roomId))
        );
    }
}
