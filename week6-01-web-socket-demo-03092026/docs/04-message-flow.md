# 04 — End-to-End Message Flow

This document traces the complete journey of a chat message from browser to all
connected clients.

---

## 1. Connection Setup

```
Browser (Alice)                   Spring Boot Server
     │                                    │
     │──── HTTP GET /stomp-ws/info ───────►│  SockJS negotiation
     │◄─── 200 OK (capabilities JSON) ────│
     │                                    │
     │──── HTTP GET /stomp-ws/…/websocket ►│  WebSocket upgrade
     │◄─── 101 Switching Protocols ───────│
     │   [WebSocket connection open]       │
     │                                    │
     │──── STOMP CONNECT ─────────────────►│
     │     heart-beat:10000,10000          │
     │                                    │
     │◄─── STOMP CONNECTED ───────────────│
     │     heart-beat:0,10000             │
     │                                    │
     │──── STOMP SUBSCRIBE ───────────────►│  subscribe to public topic
     │     destination:/topic/public       │
     │     id:sub-0                        │
     │                                    │
     │──── STOMP SEND (JOIN) ─────────────►│  announce presence
     │     destination:/app/chat.addUser   │
     │     {"sender":"Alice","type":"JOIN"}│
```

---

## 2. Message Channel Architecture

Spring's STOMP support uses a **three-channel pipeline** internally:

```
                          ┌─────────────────────────────────────────┐
                          │         Spring Messaging Infrastructure  │
                          │                                           │
Client SEND               │  clientInboundChannel                    │
    ──────────────────────►  (inbound from WebSocket)                │
                          │          │                               │
                          │          ▼                               │
                          │  SimpAnnotationMethodMessageHandler       │
                          │  (matches @MessageMapping, invokes method)│
                          │          │                               │
                          │          ▼                               │
                          │  brokerChannel                           │
                          │  (messages destined for broker)          │
                          │          │                               │
                          │          ▼                               │
                          │  SimpleBrokerMessageHandler              │
                          │  (in-memory pub/sub broker)              │
                          │          │                               │
                          │          ▼                               │
                          │  clientOutboundChannel                   │
                          │  (outbound to WebSocket sessions)        │
                          └─────────────────────────────────────────┘
                                      │
                          ────────────►  MESSAGE frame → all subscribers
```

---

## 3. A Chat Message Step by Step

### Alice sends "Hello!"

**Step 1: Client sends STOMP SEND frame**

```
SEND
destination:/app/chat.sendMessage
content-type:application/json
content-length:48

{"sender":"Alice","content":"Hello!","type":"CHAT"}
```

**Step 2: Spring receives the WebSocket frame**
- `SubProtocolWebSocketHandler` unwraps the WebSocket frame into a Spring `Message<byte[]>`

**Step 3: Routed to clientInboundChannel**
- The destination starts with `/app` → not going straight to broker
- `SimpAnnotationMethodMessageHandler` scans `@MessageMapping` methods

**Step 4: `ChatController.sendMessage()` is invoked**
```java
@MessageMapping("/chat.sendMessage")
@SendTo("/topic/public")
public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
    return chatMessage;  // same message, could be transformed here
}
```
- `@Payload ChatMessage` → Spring uses Jackson to deserialize the JSON body
- The method returns the `ChatMessage` object

**Step 5: `@SendTo` instructs the broker**
- Spring serializes the return value back to JSON
- Publishes the message to `/topic/public` on the brokerChannel

**Step 6: Broker fans out**
- `SimpleBrokerMessageHandler` looks up all sessions subscribed to `/topic/public`
- Queues a MESSAGE frame for each subscriber on the clientOutboundChannel

**Step 7: Each subscriber receives the MESSAGE frame**

```
MESSAGE
subscription:sub-0
destination:/topic/public
content-type:application/json
message-id:abc-123

{"sender":"Alice","content":"Hello!","type":"CHAT"}
```

**Step 8: Browser receives and renders**
```javascript
stompClient.subscribe('/topic/public', function(message) {
    showMessage(JSON.parse(message.body));
});
```

---

## 4. JOIN flow

```
Alice browser                   Spring                        All subscribers
     │                             │                                │
     │── SEND /app/chat.addUser ──►│                                │
     │   {"sender":"Alice",         │                                │
     │    "type":"JOIN"}           │                                │
     │                             │                                │
     │                    ChatController.addUser()                  │
     │                    stores "Alice" in session attrs           │
     │                    returns JOIN ChatMessage                  │
     │                             │                                │
     │                             │── /topic/public MESSAGE ──────►│
     │                             │   "Alice joined the chat!"     │
```

---

## 5. LEAVE flow (triggered by disconnect event)

When Alice closes the tab:
1. TCP connection drops
2. Spring detects closed WebSocket session
3. `SessionDisconnectEvent` is published
4. `WebSocketEventListener.handleWebSocketDisconnectListener()` fires
5. Reads "Alice" from the session attributes stored during JOIN
6. Calls `messagingTemplate.convertAndSend("/topic/public", leaveMessage)`
7. All remaining subscribers receive the LEAVE message

```
Alice disconnects                Spring                        All subscribers
     │                             │                                │
     │── [TCP close] ─────────────►│                                │
     │                  SessionDisconnectEvent published             │
     │                  WebSocketEventListener fires                 │
     │                  reads username="Alice" from session          │
     │                             │                                │
     │                             │── /topic/public MESSAGE ──────►│
     │                             │   "Alice left the chat."       │
```

---

## 6. Message Serialization

Spring uses `MappingJackson2MessageConverter` (auto-configured) to:
- Deserialize incoming JSON payload → Java object (at `@Payload`)
- Serialize Java return value → JSON (at `@SendTo` / `convertAndSend`)

No extra configuration needed. If Jackson is on the classpath (it is via
`spring-boot-starter-web`), Spring wires it automatically.
