## About PubChat
Study project to explore and practice real-time communication using Spring Boot and WebSocket. The main goal was to learn the architecture behind a chat application, including the use of message brokers, dynamic topics, and basic state management.

As a study project, several simplifications were made, but the core concept was successfully implemented.

![](/readme-assets/use.gif)

## Technologies
- Spring Web
- Spring WebSocket
- STOMP
- In-Memory Message Broker
- HTML, CSS e JavaScript for the basic frontend
- SockJS e Stomp.js

## Features
- Global chat: Upon connecting, the user joins a public room where all connected users can chat
- Private rooms: Users can generate a unique ID for a private room via a REST endpoint, allowing others to connect using that ID
- Room validation: The backend prevents users from joining rooms with arbitrary or invalid IDs
- 'User is typing' indicator in private rooms

## Limitations
- In-memory room repository and in-memory message broker
- No message persistence
- No spam protection (messages and room creation)
- No mechanism to clean up inactive rooms