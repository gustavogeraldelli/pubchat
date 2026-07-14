# PubChat

Real-time chat application built with Spring Boot, WebSocket, STOMP, and a lightweight HTML/CSS/JavaScript frontend.

The application supports a public global chat, temporary private rooms, typing indicators, session-based sender identification, and private error messages for invalid WebSocket actions.

## Features

- Public global chat
- Temporary private rooms
- Room creation and validation through REST
- WebSocket/STOMP chat messages
- Typing indicator in private rooms
- Join and leave events for private rooms
- Duplicate nickname prevention per private room
- Session-scoped error messages through `/user/queue/errors`
- Automatic cleanup of inactive private rooms
- Behavior-focused tests for repository, REST, and WebSocket flows

## Stack

- Java 21
- Spring Boot
- Spring Web
- Spring WebSocket
- STOMP
- SockJS
- Stomp.js
- HTML, CSS, JavaScript
- JUnit 5, AssertJ, Mockito, MockMvc

## Running

```bash
./mvnw spring-boot:run
```

Application:

```text
http://localhost:8080
```

## Testing

```bash
./mvnw test
```

Current coverage includes:

- room storage and metadata
- inactive room cleanup
- participant tracking
- duplicate nickname rejection
- REST room creation and validation
- WebSocket join, message, leave, and private error behavior

## API and Messaging

REST is used for private room management:

| Method | Route | Description |
| --- | --- | --- |
| `POST` | `/api/rooms` | Creates a private room |
| `GET` | `/api/rooms/{roomId}/available` | Checks whether a private room exists |

WebSocket/STOMP is used for real-time room activity:

| Type | Destination | Description |
| --- | --- | --- |
| connect | `/ws` | Opens the WebSocket/STOMP connection |
| send | `/app/chat/rooms/{roomId}/join` | Joins a room |
| send | `/app/chat/rooms/{roomId}/messages` | Sends a chat message |
| send | `/app/chat/rooms/{roomId}/typing` | Sends a typing event |
| send | `/app/chat/rooms/{roomId}/leave` | Leaves the current room |
| subscribe | `/topic/rooms/{roomId}` | Receives room messages and events |
| subscribe | `/user/queue/errors` | Receives private errors for the current session |

`RoomRepository` keeps private room state, metadata, and participants in memory.

## Notes

- The sender is not trusted from the message payload. The backend resolves the nickname from the WebSocket session.
- The global room is not stored in `RoomRepository`.
- Join/leave presence events are published only for private rooms.
- Private validation errors are sent to the current WebSocket session, not to the room topic.

## Limitations

- In-memory state only
- No message persistence
- No authentication
- No rate limiting
- Not designed for multiple application instances
