package com.training.websocketdemo.controller;

import com.training.websocketdemo.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP message handler for the chat application.
 *
 * <p>This class handles messages arriving from WebSocket clients over STOMP.
 * Notice it is annotated {@code @Controller} (NOT {@code @RestController}) —
 * these methods do NOT handle HTTP requests; they handle STOMP frames.
 *
 * <h2>@MessageMapping vs @RequestMapping</h2>
 * <pre>
 *   @RequestMapping("/path")  ← handles HTTP GET/POST/... requests
 *   @MessageMapping("/path")  ← handles STOMP SEND frames to /app/path
 * </pre>
 *
 * <h2>@SendTo</h2>
 * The return value of a {@code @MessageMapping} method is automatically
 * published to the specified destination by the message broker.
 * Every client subscribed to that destination receives the message.
 *
 * <h2>SimpMessagingTemplate</h2>
 * Used for programmatic (imperative) sending — you call
 * {@code messagingTemplate.convertAndSend()} or
 * {@code messagingTemplate.convertAndSendToUser()} from anywhere in the
 * application (e.g. a scheduled task, a REST endpoint, a service).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    /**
     * Spring-managed messaging template.
     * Lets us push messages to destinations from any bean, not just inside
     * @MessageMapping methods.
     */
    private final SimpMessagingTemplate messagingTemplate;

    // ------------------------------------------------------------------ //
    //  1. Broadcast chat message to ALL subscribers                       //
    // ------------------------------------------------------------------ //

    /**
     * Handles a user sending a chat message to the public room.
     *
     * <h3>STOMP frame the client sends</h3>
     * <pre>
     *   SEND
     *   destination:/app/chat.sendMessage
     *   content-type:application/json
     *
     *   {"sender":"Alice","content":"Hello!","type":"CHAT"}
     * </pre>
     *
     * <h3>What happens</h3>
     * <ol>
     *   <li>Spring deserializes the JSON payload into a {@link ChatMessage}</li>
     *   <li>This method returns the same message (you could transform it)</li>
     *   <li>{@code @SendTo} tells the broker to publish the return value to
     *       {@code /topic/public}</li>
     *   <li>All clients subscribed to {@code /topic/public} receive it</li>
     * </ol>
     *
     * @param chatMessage the message payload deserialized from JSON
     * @return the same message, broadcast to /topic/public
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        log.info("[STOMP] Message from {}: {}", chatMessage.getSender(), chatMessage.getContent());
        return chatMessage;
    }

    // ------------------------------------------------------------------ //
    //  2. User join notification                                           //
    // ------------------------------------------------------------------ //

    /**
     * Handles a user joining the chat room.
     *
     * <p>{@code SimpMessageHeaderAccessor} gives access to the STOMP session
     * attributes (e.g. the HTTP session attributes copied at handshake time).
     * Here we store the username so we can use it when the client disconnects.
     *
     * <h3>STOMP frame the client sends</h3>
     * <pre>
     *   SEND
     *   destination:/app/chat.addUser
     *
     *   {"sender":"Alice","type":"JOIN"}
     * </pre>
     *
     * @param chatMessage message with type=JOIN
     * @param headerAccessor access to the STOMP session
     * @return a JOIN notification broadcast to all subscribers
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Store username in the WebSocket session — useful for disconnect events
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());

        log.info("[STOMP] User joined: {}", chatMessage.getSender());

        // Build and return a JOIN announcement
        return ChatMessage.builder()
                .sender(chatMessage.getSender())
                .content(chatMessage.getSender() + " joined the chat!")
                .type(ChatMessage.MessageType.JOIN)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  3. Private / user-specific message                                 //
    // ------------------------------------------------------------------ //

    /**
     * Sends a private message to a specific user.
     *
     * <p>{@code convertAndSendToUser} routes the message to
     * {@code /user/{targetUser}/queue/private} — only the client
     * subscribed to that destination receives it.
     *
     * <p>This method is also reachable via a regular HTTP POST so you can
     * test it easily with curl or Postman without a WebSocket client.
     *
     * <h3>STOMP frame the client sends</h3>
     * <pre>
     *   SEND
     *   destination:/app/chat.private
     *
     *   {"sender":"Alice","content":"@Bob Hi privately!","type":"CHAT"}
     * </pre>
     *
     * <p>The content field is expected to start with "@username " to
     * indicate the recipient.
     */
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        String content = chatMessage.getContent();

        // Simple parsing: expect "@targetUser actual message"
        if (content != null && content.startsWith("@")) {
            int spaceIndex = content.indexOf(' ');
            if (spaceIndex > 1) {
                String targetUser = content.substring(1, spaceIndex);
                String actualContent = content.substring(spaceIndex + 1);

                ChatMessage privateMsg = ChatMessage.builder()
                        .sender(chatMessage.getSender())
                        .content("[Private] " + actualContent)
                        .type(ChatMessage.MessageType.CHAT)
                        .build();

                log.info("[STOMP] Private message from {} to {}: {}",
                        chatMessage.getSender(), targetUser, actualContent);

                // Route to /user/{targetUser}/queue/private
                messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", privateMsg);

                // Also send confirmation back to sender
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getSender(), "/queue/private",
                        ChatMessage.builder()
                                .sender("System")
                                .content("Private message sent to " + targetUser)
                                .type(ChatMessage.MessageType.CHAT)
                                .build()
                );
                return;
            }
        }

        // Fallback: treat as a regular message if format is wrong
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }
}
