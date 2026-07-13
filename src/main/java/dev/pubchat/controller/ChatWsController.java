package dev.pubchat.controller;

import dev.pubchat.dto.ChatMessageRequest;
import dev.pubchat.dto.ChatMessageResponse;
import dev.pubchat.dto.JoinRoomRequest;
import dev.pubchat.dto.TypingStatusRequest;
import dev.pubchat.model.Message;
import dev.pubchat.repository.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Controller
@Validated
@MessageMapping("/chat/rooms")
public class ChatWsController {

    public static final String SESSION_ROOM_ID = "roomId";
    public static final String SESSION_NICKNAME = "nickname";
    private static final String GLOBAL_ROOM = "global";

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository repository;

    public ChatWsController(SimpMessagingTemplate messagingTemplate,  RoomRepository repository) {
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
    }

    @MessageMapping("/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, @Valid JoinRoomRequest request,
                         SimpMessageHeaderAccessor headers) {
        String nickname = request.nickname().trim();
        if (!GLOBAL_ROOM.equals(roomId) && !repository.exists(roomId))
            return;

        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        if (sessionAttributes == null)
            return;

        String currentRoomId = (String) sessionAttributes.get(SESSION_ROOM_ID);
        String currentNickname = (String) sessionAttributes.get(SESSION_NICKNAME);
        boolean rejoiningSameSession = roomId.equals(currentRoomId) && nickname.equals(currentNickname);

        if (rejoiningSameSession) {
            if (!GLOBAL_ROOM.equals(roomId))
                repository.touch(roomId);
            return;
        }

        if (!GLOBAL_ROOM.equals(roomId) && repository.hasParticipant(roomId, nickname) && !rejoiningSameSession)
            return;

        leaveCurrentRoom(sessionAttributes);

        if (!GLOBAL_ROOM.equals(roomId))
            repository.addParticipant(roomId, nickname);

        sessionAttributes.put(SESSION_ROOM_ID, roomId);
        sessionAttributes.put(SESSION_NICKNAME, nickname);

        messagingTemplate.convertAndSend(topicFor(roomId), ChatMessageResponse.from(Message.join(nickname, roomId)));
    }

    @MessageMapping("/{roomId}/messages")
    public void sendMessage(@DestinationVariable String roomId, @Valid ChatMessageRequest request,
                            SimpMessageHeaderAccessor headers) {
        String nickname = nicknameFrom(headers);
        if (nickname == null || !isRoomAvailable(roomId) || !isSessionInRoom(headers, roomId))
            return;

        if (!GLOBAL_ROOM.equals(roomId))
            repository.touch(roomId);

        Message message = Message.chat(nickname, roomId, request.message().trim());
        messagingTemplate.convertAndSend(topicFor(roomId), ChatMessageResponse.from(message));
    }

    @MessageMapping("/{roomId}/typing")
    public void sendTypingStatus(@DestinationVariable String roomId, @Valid TypingStatusRequest request,
                                 SimpMessageHeaderAccessor headers) {
        String nickname = nicknameFrom(headers);
        if (nickname == null || GLOBAL_ROOM.equals(roomId) || !isRoomAvailable(roomId) || !isSessionInRoom(headers, roomId))
            return;

        repository.touch(roomId);
        Message message = Message.typing(nickname, roomId);
        messagingTemplate.convertAndSend(topicFor(roomId), ChatMessageResponse.from(message));
    }

    @MessageMapping("/{roomId}/leave")
    public void leaveRoom(@DestinationVariable String roomId, SimpMessageHeaderAccessor headers) {
        if (!isSessionInRoom(headers, roomId))
            return;

        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        if (sessionAttributes == null)
            return;

        publishLeave(sessionAttributes);
        sessionAttributes.remove(SESSION_ROOM_ID);
        sessionAttributes.remove(SESSION_NICKNAME);
    }

    private boolean isSessionInRoom(SimpMessageHeaderAccessor headers, String roomId) {
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        return sessionAttributes != null && roomId.equals(sessionAttributes.get(SESSION_ROOM_ID));
    }

    private boolean isRoomAvailable(String roomId) {
        return GLOBAL_ROOM.equals(roomId) || repository.exists(roomId);
    }

    private String nicknameFrom(SimpMessageHeaderAccessor headers) {
        Map<String, Object> sessionAttributes = headers.getSessionAttributes();
        if (sessionAttributes == null)
            return null;
        return (String) sessionAttributes.get(SESSION_NICKNAME);
    }

    private void leaveCurrentRoom(Map<String, Object> sessionAttributes) {
        if (sessionAttributes.containsKey(SESSION_ROOM_ID) && sessionAttributes.containsKey(SESSION_NICKNAME))
            publishLeave(sessionAttributes);
    }

    private void publishLeave(Map<String, Object> sessionAttributes) {
        String roomId = (String) sessionAttributes.get(SESSION_ROOM_ID);
        String nickname = (String) sessionAttributes.get(SESSION_NICKNAME);

        if (roomId == null || nickname == null)
            return;

        if (!GLOBAL_ROOM.equals(roomId))
            repository.removeParticipant(roomId, nickname);

        messagingTemplate.convertAndSend(topicFor(roomId), ChatMessageResponse.from(Message.leave(nickname, roomId)));
    }

    private String topicFor(String roomId) {
        return "/topic/rooms/" + roomId;
    }
}
