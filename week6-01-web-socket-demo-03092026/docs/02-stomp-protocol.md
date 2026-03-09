# 02 — STOMP Protocol

> **STOMP** = Simple Text Oriented Messaging Protocol

STOMP is a **sub-protocol** that runs *inside* a WebSocket connection (or plain TCP).
It defines a text-based frame format and a small set of commands that give you
pub/sub messaging without needing a full JMS/AMQP broker.

---

## 1. STOMP Frame Format

Every STOMP message is called a **frame**. A frame has three parts:

```
COMMAND\n
header1:value1\n
header2:value2\n
\n                  ← blank line separates headers from body
body (optional)
\0                  ← NULL byte terminates the frame
```

Example:

```
SEND
destination:/app/chat.sendMessage
content-type:application/json
content-length:48

{"sender":"Alice","content":"Hello!","type":"CHAT"}
```

---

## 2. STOMP Commands Reference

### Client → Server commands

| Command | Purpose |
|---------|---------|
| `CONNECT` | Open a STOMP session (authenticate, negotiate heartbeat) |
| `SEND` | Publish a message to a destination |
| `SUBSCRIBE` | Register interest in a destination |
| `UNSUBSCRIBE` | Cancel a subscription |
| `ACK` | Acknowledge receipt of a message (if ack mode is set) |
| `NACK` | Negative acknowledgement |
| `BEGIN` | Start a transaction |
| `COMMIT` | Commit a transaction |
| `ABORT` | Roll back a transaction |
| `DISCONNECT` | Gracefully close the STOMP session |

### Server → Client commands

| Command | Purpose |
|---------|---------|
| `CONNECTED` | Server acks a successful CONNECT |
| `MESSAGE` | Deliver a message to a subscriber |
| `RECEIPT` | Ack a client frame that had a `receipt` header |
| `ERROR` | Signal an error (server closes connection after this) |

---

## 3. STOMP Session Lifecycle

```
Client                                    Server
  │                                          │
  │──── CONNECT ────────────────────────────►│
  │     login:alice                          │
  │     passcode:secret                      │
  │     heart-beat:4000,4000                 │
  │                                          │
  │◄─── CONNECTED ──────────────────────────│
  │     version:1.2                          │
  │     heart-beat:0,4000                    │
  │                                          │
  │──── SUBSCRIBE ──────────────────────────►│
  │     id:sub-0                             │
  │     destination:/topic/public            │
  │                                          │
  │──── SEND ───────────────────────────────►│
  │     destination:/app/chat.sendMessage    │
  │     {"sender":"Alice","content":"Hi!"}   │
  │                                          │
  │◄─── MESSAGE ────────────────────────────│
  │     subscription:sub-0                   │
  │     destination:/topic/public            │
  │     {"sender":"Alice","content":"Hi!"}   │
  │                                          │
  │──── DISCONNECT ─────────────────────────►│
  │     receipt:77                           │
  │                                          │
  │◄─── RECEIPT ────────────────────────────│
  │     receipt-id:77                        │
  │                                          │
```

---

## 4. Destinations

Destinations are named channels — think of them like message topics or queues.
The naming convention in Spring:

| Prefix | Routed to | Example |
|--------|-----------|---------|
| `/app/…` | `@MessageMapping` controller method | `/app/chat.sendMessage` |
| `/topic/…` | In-memory broker (fan-out to all subscribers) | `/topic/public` |
| `/queue/…` | In-memory broker (point-to-point) | `/queue/notifications` |
| `/user/…` | User-specific routing | `/user/queue/private` |

> `/topic` vs `/queue` in Spring's simple broker: both broadcast to all subscribers.
> The distinction matters when using an **external broker** (RabbitMQ/ActiveMQ)
> where topics fan-out and queues compete (only one consumer gets each message).

---

## 5. Heartbeat

Heartbeat prevents "ghost connections" — connections that look alive but the
other side has crashed (pulled the cable, OS crash, etc.).

Negotiated in the CONNECT / CONNECTED exchange:

```
Client CONNECT:
  heart-beat:4000,4000
  ↑ "I can send every 4000ms, I expect every 4000ms"

Server CONNECTED:
  heart-beat:0,4000
  ↑ "I won't send heartbeats (0), but I'll expect yours every 4000ms"

Final agreed rates:
  Client → Server: max(4000, 4000) = 4000ms
  Server → Client: max(4000, 0)    = 4000ms (but server said 0, so disabled)
```

Spring Boot configuration:

```properties
# application.properties
websocket.stomp.heartbeat.send=10000      # ms between server-sent heartbeats
websocket.stomp.heartbeat.receive=10000   # ms we expect between client heartbeats
```

---

## 6. STOMP 1.2 vs Earlier Versions

STOMP 1.2 (the current version) adds:
- `\r\n` line endings in addition to `\n`
- `ACK` / `NACK` with message ID instead of subscription ID
- Improved UTF-8 support

Spring Boot uses STOMP 1.2 by default.

---

## 7. STOMP vs Other Protocols

| Protocol | Transport | Model | Use case |
|----------|-----------|-------|----------|
| STOMP | WebSocket / TCP | Pub/Sub + Point-to-point | Web apps, lightweight messaging |
| AMQP | TCP | Full broker protocol | Enterprise messaging (RabbitMQ) |
| MQTT | TCP / WebSocket | Pub/Sub (lightweight) | IoT, mobile |
| GraphQL Subscriptions | WebSocket | Event streams | API subscriptions |
| SSE | HTTP | Server push (one-way) | News feeds, notifications |

STOMP is ideal for **browser ↔ server** real-time because:
- All major browsers support WebSocket
- STOMP.js is a tiny client library
- Spring Boot makes configuration trivial
