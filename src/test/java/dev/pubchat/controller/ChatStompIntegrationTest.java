package dev.pubchat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pubchat.dto.ChatErrorResponse;
import dev.pubchat.dto.ChatMessageResponse;
import dev.pubchat.model.MessageType;
import dev.pubchat.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatStompIntegrationTest {

    private static final int TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private RoomRepository repository;

    @Test
    void shouldSendChatMessageToSubscribedPrivateRoom() throws Exception {
        repository.add("room-stomp-chat");
        StompSession session = connect();
        try {
            BlockingQueue<ChatMessageResponse> receivedMessages = new LinkedBlockingQueue<>();

            session.subscribe("/topic/rooms/room-stomp-chat", queueFrameHandler(ChatMessageResponse.class, receivedMessages));
            session.send("/app/chat/rooms/room-stomp-chat/join", Map.of("nickname", "Gus"));

            ChatMessageResponse joinResponse = receivedMessages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(joinResponse).isNotNull();
            assertThat(joinResponse.sender()).isEqualTo("Gus");
            assertThat(joinResponse.type()).isEqualTo(MessageType.JOIN);

            session.send("/app/chat/rooms/room-stomp-chat/messages", Map.of("message", "hello from stomp"));
            ChatMessageResponse response = receivedMessages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(response).isNotNull();
            assertThat(response.sender()).isEqualTo("Gus");
            assertThat(response.message()).isEqualTo("hello from stomp");
            assertThat(response.type()).isEqualTo(MessageType.CHAT);
        }
        finally {
            session.disconnect();
        }
    }

    @Test
    void shouldSendPrivateErrorWhenJoiningPrivateRoomWithDuplicateNickname() throws Exception {
        repository.add("room-stomp-duplicate");
        repository.addParticipant("room-stomp-duplicate", "Gus");
        StompSession session = connect();
        try {
            CompletableFuture<ChatErrorResponse> receivedError = new CompletableFuture<>();

            session.subscribe("/user/queue/errors", frameHandler(ChatErrorResponse.class, receivedError));
            session.send("/app/chat/rooms/room-stomp-duplicate/join", Map.of("nickname", "Gus"));

            ChatErrorResponse response = receivedError.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(response.message()).isEqualTo("Nickname already in use in this room.");
            assertThat(response.roomId()).isEqualTo("room-stomp-duplicate");
            assertThat(response.action()).isEqualTo("join");
        }
        finally {
            session.disconnect();
        }
    }

    private StompSession connect() throws Exception {
        Transport webSocketTransport = new WebSocketTransport(new StandardWebSocketClient());
        SockJsClient sockJsClient = new SockJsClient(List.of(webSocketTransport));
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        stompClient.setMessageConverter(converter);

        return stompClient
                .connectAsync("http://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private <T> StompFrameHandler frameHandler(Class<T> payloadType, CompletableFuture<T> future) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                future.complete(payloadType.cast(payload));
            }
        };
    }

    private <T> StompFrameHandler queueFrameHandler(Class<T> payloadType, BlockingQueue<T> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(payloadType.cast(payload));
            }
        };
    }
}
