# 01 — WebSocket Fundamentals

## 1. The Problem With HTTP for Real-Time

HTTP was designed for a request/response model. Every interaction requires the
client to initiate. For real-time features this means one of:

| Technique | How | Problem |
|-----------|-----|---------|
| **Short polling** | Client `GET /messages` every 2 seconds | Wasted requests even when nothing changed |
| **Long polling** | Client sends request; server holds it open until there's data, then responds | Complex server state; high connection count |
| **Server-Sent Events (SSE)** | Persistent HTTP stream, server → client only | One direction only |
| **WebSocket** | Single TCP connection, full-duplex | The right tool for interactive, bidirectional real-time |

---

## 2. How a WebSocket Connection is Established

WebSocket reuses the HTTP port (80/443) but upgrades the protocol.

### Step 1 — Client sends HTTP Upgrade request

```
GET /raw-ws HTTP/1.1
Host: localhost:8080
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
Sec-WebSocket-Extensions: permessage-deflate
```

- `Upgrade: websocket` — asks server to switch protocols
- `Sec-WebSocket-Key` — random base64 value; server uses it to prove it understood the request (not just a plain HTTP cache response)

### Step 2 — Server responds with 101 Switching Protocols

```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

- Status `101` means "I'm switching protocols as you requested"
- After this response, the TCP connection is **no longer HTTP** — it speaks WebSocket framing

### Step 3 — Full-duplex communication

Both sides can now send at any time without waiting for a request.

```
Client ─── "Hello server" ──────────► Server
Client ◄─── "Hello client" ─────────  Server
Server ──── "New message!" ──────────► Client  (server-initiated push)
```

---

## 3. WebSocket Frames

Data is sent in **frames** (not HTTP requests). A frame has:

| Field | Description |
|-------|-------------|
| FIN bit | Is this the last fragment? |
| Opcode | Text (0x1), Binary (0x2), Close (0x8), Ping (0x9), Pong (0xA) |
| Mask bit | Client frames are always masked; server frames are not |
| Payload length | 7-bit, 16-bit, or 64-bit |
| Masking key | 4 bytes (client → server only) |
| Payload | The actual data |

In Spring you rarely deal with frames directly — Spring wraps them in
`TextMessage`, `BinaryMessage`, etc.

---

## 4. Raw WebSocket in Spring — Key Classes

```
WebSocketHandler  (interface)
  └── AbstractWebSocketHandler  (partial implementation)
        └── TextWebSocketHandler  (extends AbstractWebSocketHandler,
                                   overrides handleTextMessage)
```

### Registering a handler

```java
// In a @Configuration class implementing WebSocketConfigurer
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(myHandler, "/my-endpoint")
            .setAllowedOrigins("*");
}
```

### Handler lifecycle callbacks

```java
// Called once per new TCP connection (after HTTP→WS upgrade)
void afterConnectionEstablished(WebSocketSession session)

// Called for every text frame received from this client
void handleTextMessage(WebSocketSession session, TextMessage message)

// Called when the connection closes normally
void afterConnectionClosed(WebSocketSession session, CloseStatus status)

// Called on I/O errors
void handleTransportError(WebSocketSession session, Throwable exception)
```

### Sending messages

```java
session.sendMessage(new TextMessage("Hello!"));

// To broadcast to all sessions:
for (WebSocketSession s : sessions) {
    if (s.isOpen()) {
        s.sendMessage(new TextMessage(payload));
    }
}
```

---

## 5. Limitations of Raw WebSocket

Raw WebSocket gives you a byte pipe. You have to invent everything on top:

- **Message format** — JSON? XML? Custom binary?
- **Routing** — how do different messages go to different handlers?
- **Pub/Sub** — who subscribes to what?
- **Acknowledgements** — did the message arrive?
- **Heartbeats** — detect dead connections
- **Error handling** — what does an error frame look like?

This is why most applications use **STOMP** (see next doc).
