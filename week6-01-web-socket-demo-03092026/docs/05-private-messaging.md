# 05 — Private / User-Specific Messaging

---

## 1. The Challenge

In a broadcast system, every subscriber to `/topic/public` sees every message.
For private messages (DMs, notifications, order updates) you need to target
a **single user**.

Spring STOMP solves this with **user destinations**.

---

## 2. User Destination Prefix

Configured in `StompWebSocketConfig`:

```java
registry.setUserDestinationPrefix("/user");
```

This prefix triggers special routing logic:

| What you write | What Spring resolves to |
|----------------|------------------------|
| Server sends to user "alice" at `/queue/private` | `/user/alice/queue/private` |
| Client subscribes to `/user/queue/private` | Spring auto-personalizes to `/user/{sessionId}/queue/private` |

---

## 3. Server-Side: Sending to a Specific User

```java
// Inject SimpMessagingTemplate
private final SimpMessagingTemplate messagingTemplate;

// Send to user "alice"
messagingTemplate.convertAndSendToUser(
    "alice",            // username (Principal name)
    "/queue/private",   // user destination (without /user prefix)
    message             // payload (serialized to JSON automatically)
);
```

Spring internally maps this to `/user/alice/queue/private`. It looks up all
WebSocket sessions authenticated as "alice" and delivers the message to each.

---

## 4. Client-Side: Subscribing to Private Messages

```javascript
// The client subscribes to /user/queue/private
// Spring personalizes it to /user/{my-session-id}/queue/private
stompClient.subscribe('/user/queue/private', function(message) {
    const msg = JSON.parse(message.body);
    showPrivateMessage(msg);
});
```

Spring knows which session belongs to which user so it routes correctly.

---

## 5. How Spring Identifies Users

Without authentication, Spring uses the **WebSocket session ID** as the
user identifier. This means two tabs in the same browser are considered
different users.

With Spring Security:
```java
// WebSocket session is tied to the authenticated Principal
messagingTemplate.convertAndSendToUser(
    principal.getName(),  // e.g. "alice" from JWT/session
    "/queue/private",
    message
);
```

---

## 6. Demo Implementation (ChatController)

```java
@MessageMapping("/chat.private")
public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
    String content = chatMessage.getContent();

    // Expected format: "@targetUser actual message"
    if (content.startsWith("@")) {
        int spaceIndex = content.indexOf(' ');
        String targetUser = content.substring(1, spaceIndex);
        String actualContent = content.substring(spaceIndex + 1);

        ChatMessage privateMsg = ChatMessage.builder()
                .sender(chatMessage.getSender())
                .content("[Private] " + actualContent)
                .type(ChatMessage.MessageType.CHAT)
                .build();

        // Route to target user's private queue
        messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", privateMsg);

        // Confirmation back to sender
        messagingTemplate.convertAndSendToUser(
                chatMessage.getSender(), "/queue/private",
                ChatMessage.builder()
                        .sender("System")
                        .content("Message sent to " + targetUser)
                        .build()
        );
    }
}
```

---

## 7. Full Private Message Flow

```
Alice browser                   Spring                      Bob browser
     │                             │                              │
     │                             │  Bob subscribes to           │
     │                             │◄── /user/queue/private ──────│
     │                             │                              │
     │── SEND /app/chat.private ──►│                              │
     │   {"sender":"Alice",         │                              │
     │    "content":"@Bob Hi!",     │                              │
     │    "type":"CHAT"}           │                              │
     │                             │                              │
     │                    ChatController.sendPrivateMessage()      │
     │                    parses targetUser="Bob"                  │
     │                    content="Hi!"                           │
     │                             │                              │
     │                             │── /user/Bob/queue/private ───►│
     │                             │   "[Private] Hi!"             │
     │                             │                              │
     │◄── /user/Alice/queue/private│                              │
     │   "Message sent to Bob"     │                              │
```

---

## 8. `@SendToUser` Annotation Alternative

Instead of using `SimpMessagingTemplate`, you can use `@SendToUser` on a
`@MessageMapping` method to reply to the message originator:

```java
@MessageMapping("/chat.ack")
@SendToUser("/queue/ack")      // ← sends only back to the sender
public AckMessage acknowledge(@Payload ChatMessage msg) {
    return new AckMessage("Received: " + msg.getContent());
}
```

This is equivalent to:
```java
messagingTemplate.convertAndSendToUser(
    principal.getName(), "/queue/ack", new AckMessage(...)
);
```

The key difference:
- `@SendToUser` → sends to the **originator** of the current message
- `convertAndSendToUser(username, …)` → sends to **any named user**
