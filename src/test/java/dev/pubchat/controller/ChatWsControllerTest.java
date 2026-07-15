package dev.pubchat.controller;

import dev.pubchat.dto.ChatErrorResponse;
import dev.pubchat.dto.ChatMessageRequest;
import dev.pubchat.dto.ChatMessageResponse;
import dev.pubchat.dto.JoinRoomRequest;
import dev.pubchat.model.MessageType;
import dev.pubchat.repository.RoomRepository;
import dev.pubchat.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatWsControllerTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RoomRepository repository = new RoomRepository();
    private final RateLimitService rateLimitService = new RateLimitService();
    private final ChatWsController controller = new ChatWsController(messagingTemplate, repository, rateLimitService);

    @Test
    void shouldStoreSessionButNotPublishJoinWhenJoiningGlobalRoom() {
        SimpMessageHeaderAccessor headers = headers("session-1");

        controller.joinRoom("global", new JoinRoomRequest("Gus"), headers);

        assertThat(headers.getSessionAttributes())
                .containsEntry(ChatWsController.SESSION_ROOM_ID, "global")
                .containsEntry(ChatWsController.SESSION_NICKNAME, "Gus");
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/rooms/global"), any(Object.class));
    }

    @Test
    void shouldStoreSessionAndPublishJoinWhenJoiningPrivateRoom() {
        repository.add("room-1");
        SimpMessageHeaderAccessor headers = headers("session-1");

        controller.joinRoom("room-1", new JoinRoomRequest("Gus"), headers);

        assertThat(repository.hasParticipant("room-1", "Gus")).isTrue();
        assertThat(headers.getSessionAttributes())
                .containsEntry(ChatWsController.SESSION_ROOM_ID, "room-1")
                .containsEntry(ChatWsController.SESSION_NICKNAME, "Gus");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/room-1"),
                argThat((Object payload) -> isResponse(payload, "Gus", MessageType.JOIN))
        );
    }

    @Test
    void shouldSendPrivateErrorWhenNicknameAlreadyExistsInPrivateRoom() {
        repository.add("room-1");
        repository.addParticipant("room-1", "Gus");
        SimpMessageHeaderAccessor headers = headers("session-1");

        controller.joinRoom("room-1", new JoinRoomRequest("Gus"), headers);

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/rooms/room-1"), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq("session-1"),
                eq("/queue/errors"),
                argThat(payload -> isError(payload, "Nickname already in use in this room.", "room-1", "join")),
                anyMap()
        );
    }

    @Test
    void shouldUseNicknameFromSessionWhenSendingMessage() {
        SimpMessageHeaderAccessor headers = headers("session-1");
        headers.getSessionAttributes().put(ChatWsController.SESSION_ROOM_ID, "global");
        headers.getSessionAttributes().put(ChatWsController.SESSION_NICKNAME, "Gus");

        controller.sendMessage("global", new ChatMessageRequest("hello"), headers);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/global"),
                argThat((Object payload) -> isResponse(payload, "Gus", MessageType.CHAT))
        );
    }

    @Test
    void shouldSendPrivateErrorWhenMessageRateLimitIsExceeded() {
        SimpMessageHeaderAccessor headers = headers("session-rate-limit");
        headers.getSessionAttributes().put(ChatWsController.SESSION_ROOM_ID, "global");
        headers.getSessionAttributes().put(ChatWsController.SESSION_NICKNAME, "Gus");

        for (int i = 0; i < 11; i++)
            controller.sendMessage("global", new ChatMessageRequest("hello " + i), headers);

        verify(messagingTemplate, times(10)).convertAndSend(
                eq("/topic/rooms/global"),
                argThat((Object payload) -> isResponse(payload, "Gus", MessageType.CHAT))
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq("session-rate-limit"),
                eq("/queue/errors"),
                argThat(payload -> isError(payload, "You are sending messages too quickly.", "global", "message")),
                anyMap()
        );
    }

    @Test
    void shouldPublishLeaveForPrivateRoom() {
        repository.add("room-1");
        repository.addParticipant("room-1", "Gus");
        SimpMessageHeaderAccessor headers = headers("session-1");
        headers.getSessionAttributes().put(ChatWsController.SESSION_ROOM_ID, "room-1");
        headers.getSessionAttributes().put(ChatWsController.SESSION_NICKNAME, "Gus");

        controller.leaveRoom("room-1", headers);

        assertThat(repository.hasParticipant("room-1", "Gus")).isFalse();
        assertThat(headers.getSessionAttributes())
                .doesNotContainKey(ChatWsController.SESSION_ROOM_ID)
                .doesNotContainKey(ChatWsController.SESSION_NICKNAME);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/room-1"),
                argThat((Object payload) -> isResponse(payload, "Gus", MessageType.LEAVE))
        );
    }

    private SimpMessageHeaderAccessor headers(String sessionId) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId(sessionId);
        headers.setSessionAttributes(new HashMap<>());
        return headers;
    }

    private boolean isResponse(Object payload, String sender, MessageType type) {
        return payload instanceof ChatMessageResponse response
                && sender.equals(response.sender())
                && type == response.type();
    }

    private boolean isError(Object payload, String message, String roomId, String action) {
        return payload instanceof ChatErrorResponse response
                && message.equals(response.message())
                && roomId.equals(response.roomId())
                && action.equals(response.action());
    }
}
