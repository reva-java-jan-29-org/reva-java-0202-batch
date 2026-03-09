# 09 — Interview Questions: WebSocket & STOMP

---

## Core WebSocket

**Q1: What is WebSocket and how does it differ from HTTP?**

WebSocket is a **full-duplex communication protocol** over a single TCP connection.
HTTP is **half-duplex request/response** — the client always initiates. WebSocket
allows either side to send data at any time after the initial handshake. The connection
stays open for the duration of the session, eliminating the overhead of repeated
HTTP connection setups.

---

**Q2: Explain the WebSocket handshake.**

1. Client sends an HTTP GET with `Upgrade: websocket` and `Connection: Upgrade` headers
2. Server responds with HTTP 101 (Switching Protocols) and `Sec-WebSocket-Accept`
3. The TCP connection now speaks the WebSocket framing protocol, not HTTP
4. Both sides can send frames at any time

The `Sec-WebSocket-Key` / `Sec-WebSocket-Accept` exchange prevents caching proxies
from replaying old HTTP responses as WebSocket upgrades.

---

**Q3: When would you choose WebSocket over Server-Sent Events (SSE)?**

| Scenario | Choose |
|----------|--------|
| Client needs to send data to server (chat, gaming, collaborative editing) | WebSocket |
| Server push only (notifications, live feeds) | SSE |
| Need to pass through restrictive proxies easily | SSE (plain HTTP) |
| Full-duplex required | WebSocket |

SSE is simpler and works over plain HTTP/2. WebSocket gives true bidirectional.

---

**Q4: What is a WebSocket frame?**

The basic unit of WebSocket data transmission. A frame contains:
- Opcode (Text=0x1, Binary=0x2, Close=0x8, Ping=0x9, Pong=0xA)
- FIN bit (is this the last fragment?)
- Masking key (client→server frames are always masked)
- Payload length
- Payload data

---

**Q5: How many concurrent WebSocket connections can a server handle?**

Each connection holds one TCP socket (file descriptor). Typical limits:
- OS file descriptor limit (`ulimit -n`, default 1024; set to 65535+)
- Available memory (~50KB per idle connection)
- A tuned server can handle 50,000–100,000+ concurrent connections
- For massive scale, use an external broker relay so app servers don't hold all connections

---

## STOMP Protocol

**Q6: What is STOMP and why use it instead of raw WebSocket?**

STOMP (Simple Text Oriented Messaging Protocol) is a sub-protocol running inside
WebSocket. Raw WebSocket is a byte pipe — you must invent message routing, pub/sub,
error frames, heartbeats, etc. STOMP provides these out of the box:
- Named destinations (topics/queues)
- Standard commands (SEND, SUBSCRIBE, ACK)
- Header-based metadata
- Heartbeat negotiation
- Client library support (STOMP.js, StompJS)

---

**Q7: Describe the STOMP frame format.**

```
COMMAND\n
header1:value1\n
header2:value2\n
\n
body (optional)
\0 (NULL byte)
```

The COMMAND is one of: CONNECT, CONNECTED, SEND, SUBSCRIBE, UNSUBSCRIBE,
MESSAGE, ACK, NACK, BEGIN, COMMIT, ABORT, DISCONNECT, RECEIPT, ERROR.

---

**Q8: What are STOMP heartbeats and why are they important?**

Heartbeats detect "ghost connections" — TCP connections that appear alive but
the other side has crashed (network interruption, OS crash). Negotiated during
CONNECT/CONNECTED:

```
Client: heart-beat:4000,4000  (can send every 4s, expects every 4s)
Server: heart-beat:0,4000     (won't send, expects client every 4s)
Result: client sends heartbeat every 4s; server does not
```

Without heartbeats, a dead connection might not be detected until a write fails,
which could be minutes later.

---

**Q9: Explain STOMP destination prefixes in Spring.**

| Prefix | Meaning |
|--------|---------|
| `/app` | Routes to `@MessageMapping` controller methods. You add business logic here. |
| `/topic` | Straight to broker (fan-out). All subscribers receive each message. |
| `/queue` | Straight to broker (point-to-point in full broker; fan-out in simple broker). |
| `/user` | User-specific routing. `convertAndSendToUser("alice", "/queue/x", msg)` → `/user/alice/queue/x`. |

---

## Spring-Specific

**Q10: What does `@EnableWebSocketMessageBroker` do?**

It activates Spring's STOMP message broker infrastructure:
- Registers `SubProtocolWebSocketHandler` to handle WebSocket sessions
- Creates `SimpAnnotationMethodMessageHandler` to dispatch to `@MessageMapping`
- Starts the configured broker (simple or relay)
- Creates `SimpMessagingTemplate` bean
- Sets up `clientInboundChannel`, `brokerChannel`, `clientOutboundChannel`

---

**Q11: What is the difference between `@SendTo` and `@SendToUser`?**

| Annotation | Target |
|-----------|--------|
| `@SendTo("/topic/public")` | All subscribers of `/topic/public` (broadcast) |
| `@SendToUser("/queue/reply")` | Only the user who sent the triggering message |

`@SendToUser` is equivalent to:
```java
messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/reply", payload);
```

---

**Q12: How do you send a message to a specific user in Spring STOMP?**

```java
// In any Spring bean:
messagingTemplate.convertAndSendToUser(
    "alice",          // username (Principal.getName())
    "/queue/private", // destination (without /user prefix)
    message           // payload
);
```

Configure the user destination prefix in `StompWebSocketConfig`:
```java
registry.setUserDestinationPrefix("/user");
```

Client subscribes to `/user/queue/private` and Spring personalizes it to
the current user's session.

---

**Q13: What is SockJS and when do you need it?**

SockJS is a JavaScript client library + server protocol that:
1. Tries native WebSocket first
2. Falls back to HTTP transports (xhr-streaming, xhr-polling) if WebSocket fails

You need it when:
- Users are on corporate networks with proxies that block WebSocket
- You want maximum compatibility across network environments

Enable it in Spring: `.withSockJS()` on the endpoint registration.

---

**Q14: What is the difference between the simple broker and a broker relay?**

| Simple Broker | Broker Relay |
|---------------|-------------|
| In-memory, single server | External broker (RabbitMQ, ActiveMQ) |
| Lost on server restart | Persistent (durable queues) |
| No cross-server messaging | Works across all app instances |
| Zero infrastructure setup | Requires broker installation |
| Good for demos / single-node | Required for production clustering |

---

**Q15: How do you handle WebSocket connection lifecycle events in Spring?**

Use `@EventListener` on Spring application events:

```java
@EventListener
public void onConnect(SessionConnectedEvent event) {
    // new STOMP session established
}

@EventListener
public void onDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = (String) accessor.getSessionAttributes().get("username");
    // broadcast leave message, clean up resources
}
```

These events are fired by Spring's STOMP infrastructure for every WebSocket
session that connects or disconnects.

---

**Q16: How do you secure WebSocket endpoints?**

Two layers:
1. **HTTP handshake level** — Spring Security's `HttpSecurity` controls who can
   connect to the endpoint URL
2. **STOMP message level** — `AbstractSecurityWebSocketMessageBrokerConfigurer`
   controls which destinations require which roles

```java
// Handshake level
.requestMatchers("/stomp-ws/**").authenticated()

// Message level
messages.simpDestMatchers("/app/**").hasRole("USER")
        .simpSubscribeDestMatchers("/topic/admin/**").hasRole("ADMIN")
```

Also use a `HandshakeInterceptor` to copy HTTP session / JWT claims into
WebSocket session attributes for later use.
