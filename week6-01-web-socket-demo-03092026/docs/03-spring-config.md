# 03 — Spring WebSocket Configuration Deep-Dive

---

## 1. Two Separate Configurations

This project has **two distinct WebSocket configurations** — each handles a
different layer:

```
RawWebSocketConfig        → @EnableWebSocket
                            implements WebSocketConfigurer
                            → low-level: registers TextWebSocketHandler

StompWebSocketConfig      → @EnableWebSocketMessageBroker
                            implements WebSocketMessageBrokerConfigurer
                            → high-level: configures STOMP broker + @MessageMapping
```

You can have both in the same application. They are independent infrastructure.

---

## 2. Raw WebSocket Configuration

```java
@Configuration
@EnableWebSocket                        // ← activates raw WebSocket support
public class RawWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(rawWebSocketHandler, "/raw-ws")   // path → handler
            .setAllowedOrigins("*");                       // CORS
    }
}
```

### Key points

- `@EnableWebSocket` registers Spring's `WebSocketHandlerMapping` bean
- `WebSocketConfigurer.registerWebSocketHandlers()` is called during context startup
- You can register multiple handlers at different paths
- `.setAllowedOrigins()` controls which origins can upgrade to WebSocket (CORS equivalent)

---

## 3. STOMP Configuration

```java
@Configuration
@EnableWebSocketMessageBroker           // ← activates STOMP infrastructure
public class StompWebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    // --- Part A: Handshake endpoint ---
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/stomp-ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    // --- Part B: Broker routing ---
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
```

---

## 4. What `@EnableWebSocketMessageBroker` Wires Up

When you add this annotation, Spring registers the following beans automatically:

| Bean | Role |
|------|------|
| `SubProtocolWebSocketHandler` | Bridges raw WebSocket frames to STOMP messages |
| `SimpleBrokerMessageHandler` | In-memory pub/sub broker |
| `SimpAnnotationMethodMessageHandler` | Dispatches STOMP messages to `@MessageMapping` |
| `UserDestinationMessageHandler` | Routes `/user/…` destinations |
| `SimpMessagingTemplate` | Injected into your beans for sending messages programmatically |
| `DefaultHandshakeHandler` | Performs the HTTP → WebSocket upgrade |

---

## 5. `configureMessageBroker` — Routing Rules

```java
registry.enableSimpleBroker("/topic", "/queue");
```
- Starts the in-memory broker
- Any destination starting with `/topic` or `/queue` goes straight to the broker
- The broker fans the message out to all subscribers of that destination
- **Does NOT pass through any `@MessageMapping` method**

```java
registry.setApplicationDestinationPrefixes("/app");
```
- Destinations starting with `/app` are routed to `@MessageMapping` methods
- Spring strips the prefix before matching: `/app/chat.sendMessage` → `@MessageMapping("/chat.sendMessage")`
- This is where you apply business logic before publishing to the broker

```java
registry.setUserDestinationPrefix("/user");
```
- Enables server-to-user messaging
- On the server: `messagingTemplate.convertAndSendToUser("alice", "/queue/private", msg)`
  → routes to `/user/alice/queue/private`
- On the client: subscribe to `/user/queue/private` (Spring automatically personalizes it)

---

## 6. `registerStompEndpoints` — The Handshake Endpoint

```java
registry.addEndpoint("/stomp-ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
```

This registers TWO things:
1. A native WebSocket endpoint at `ws://localhost:8080/stomp-ws`
2. A SockJS endpoint at `http://localhost:8080/stomp-ws` (multiple fallback URLs under this path)

### SockJS URL hierarchy (Spring serves all of these):

```
/stomp-ws/info              ← SockJS info request (capabilities check)
/stomp-ws/{server}/{session}/websocket   ← WebSocket transport
/stomp-ws/{server}/{session}/xhr         ← XHR streaming transport
/stomp-ws/{server}/{session}/xhr-polling ← XHR polling transport
/stomp-ws/{server}/{session}/eventsource ← EventSource transport
/stomp-ws/{server}/{session}/htmlfile    ← IFrame transport (IE)
```

---

## 7. `@MessageMapping` Method Signatures

Spring supports a rich set of parameter types for `@MessageMapping` methods:

```java
@MessageMapping("/example")
public ReturnType handle(
    @Payload MyDto payload,               // deserialized message body
    @Header("custom-header") String h,    // specific STOMP header
    @Headers Map<String, Object> headers, // all headers
    Principal principal,                  // authenticated user
    SimpMessageHeaderAccessor accessor,   // full header access + session attrs
    @DestinationVariable("id") Long id    // path variable from destination
) { ... }
```

### Return value options

| Annotation | Behavior |
|-----------|----------|
| `@SendTo("/topic/foo")` | Broadcast return value to that destination |
| `@SendToUser("/queue/bar")` | Send to the originating user's destination |
| _(none)_ | Return value is ignored |

---

## 8. SimpMessagingTemplate — Programmatic Sending

Inject `SimpMessagingTemplate` wherever you need to push messages outside
a `@MessageMapping` (e.g. from a scheduled task or REST endpoint):

```java
@Autowired
SimpMessagingTemplate messagingTemplate;

// Broadcast to all /topic/public subscribers
messagingTemplate.convertAndSend("/topic/public", new ChatMessage(...));

// Send to a specific user (subscribes to /user/queue/inbox)
messagingTemplate.convertAndSendToUser("alice", "/queue/inbox", new ChatMessage(...));

// With custom headers
MessageHeaders headers = new SimpMessageHeaderBuilder()
        .setContentType(MediaType.APPLICATION_JSON)
        .build();
messagingTemplate.convertAndSend("/topic/public", payload, headers);
```
