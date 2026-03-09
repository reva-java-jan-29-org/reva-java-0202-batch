# WebSocket + STOMP Demo — Overview

> Spring Boot 3.4.1 · week6-01-web-socket-demo-03092026

## What this project covers

| # | Topic |
|---|-------|
| 1 | HTTP polling vs WebSocket — when and why |
| 2 | Raw WebSocket API in Spring |
| 3 | STOMP protocol — frames, destinations, commands |
| 4 | Spring STOMP configuration (`@EnableWebSocketMessageBroker`) |
| 5 | `@MessageMapping` controllers |
| 6 | Topic-based pub/sub (broadcast) |
| 7 | User-specific / private messaging |
| 8 | SockJS fallback transport |
| 9 | WebSocket lifecycle events |

---

## Running the project

```bash
cd week6-01-web-socket-demo-03092026
mvn spring-boot:run
# open http://localhost:8080
```

Open the URL in **two different browser tabs** to see real-time messaging between sessions.

---

## Endpoints

| Type | URL | Description |
|------|-----|-------------|
| HTTP | `GET /` | Chat UI (served from `static/index.html`) |
| WebSocket | `ws://localhost:8080/raw-ws` | Raw WebSocket — no protocol, plain text echo broadcast |
| WebSocket | `ws://localhost:8080/stomp-ws` | STOMP over WebSocket (native WS) |
| HTTP/SockJS | `http://localhost:8080/stomp-ws` | SockJS fallback endpoint |

---

## STOMP Destinations

| Direction | Destination | Description |
|-----------|-------------|-------------|
| Client → Server (via `/app`) | `/app/chat.sendMessage` | Send a public chat message |
| Client → Server (via `/app`) | `/app/chat.addUser` | Announce joining the room |
| Client → Server (via `/app`) | `/app/chat.private` | Send a private message (`@username text`) |
| Server → Client (broker) | `/topic/public` | Broadcast channel — all connected users |
| Server → Client (user) | `/user/queue/private` | Private channel per user |

---

## Package Structure

```
src/main/java/com/training/websocketdemo/
├── WebSocketDemoApplication.java       ← @SpringBootApplication entry point
│
├── config/
│   ├── RawWebSocketConfig.java         ← registers /raw-ws endpoint
│   └── StompWebSocketConfig.java       ← configures STOMP broker + /stomp-ws
│
├── handler/
│   └── RawWebSocketHandler.java        ← low-level TextWebSocketHandler
│
├── model/
│   └── ChatMessage.java                ← STOMP message payload (CHAT/JOIN/LEAVE)
│
└── controller/
    ├── ChatController.java             ← @MessageMapping STOMP handlers
    └── WebSocketEventListener.java     ← connect/disconnect lifecycle events

src/main/resources/
├── application.properties
└── static/
    └── index.html                      ← browser chat UI (SockJS + STOMP.js)

docs/
├── 00-overview.md          ← this file
├── 01-websocket-fundamentals.md
├── 02-stomp-protocol.md
├── 03-spring-config.md
├── 04-message-flow.md
├── 05-private-messaging.md
├── 06-sockjs.md
├── 07-production-tips.md
└── 09-interview-questions.md
```

---

## Docs Navigation

| File | Topic |
|------|-------|
| [01-websocket-fundamentals.md](01-websocket-fundamentals.md) | HTTP vs WebSocket, handshake, frames |
| [02-stomp-protocol.md](02-stomp-protocol.md) | STOMP commands, frames, heartbeat |
| [03-spring-config.md](03-spring-config.md) | Spring annotations deep-dive |
| [04-message-flow.md](04-message-flow.md) | End-to-end message journey |
| [05-private-messaging.md](05-private-messaging.md) | User-specific destinations |
| [06-sockjs.md](06-sockjs.md) | SockJS fallback explained |
| [07-production-tips.md](07-production-tips.md) | Scaling, security, external broker |
| [09-interview-questions.md](09-interview-questions.md) | Q&A for interviews |
