package dev.pubchat.dto;

import dev.pubchat.model.Message;
import dev.pubchat.model.MessageType;

import java.time.Instant;

public record ChatMessageResponse(
        String sender,
        String message,
        MessageType type,
        Instant sentAt
) {
    public static ChatMessageResponse from(Message message) {
        return new ChatMessageResponse(
                message.sender(),
                message.message(),
                message.type(),
                message.sentAt()
        );
    }
}
