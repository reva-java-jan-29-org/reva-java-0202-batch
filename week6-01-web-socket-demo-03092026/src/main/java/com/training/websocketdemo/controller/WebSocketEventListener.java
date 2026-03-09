package com.training.websocketdemo.controller;

import com.training.websocketdemo.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket lifecycle events published by the Spring framework.
 *
 * <h2>Why do we need this?</h2>
 * When a client disconnects (closes the browser tab, loses network), there is
 * no explicit STOMP UNSUBSCRIBE or SEND frame — the TCP connection just closes.
 * Spring publishes {@link SessionDisconnectEvent} so we can react and broadcast
 * a LEAVE notification to the rest of the chat room.
 *
 * <h2>@EventListener</h2>
 * Spring's standard application event mechanism. No extra configuration needed —
 * just annotate any method with {@code @EventListener} and declare the event
 * type as the parameter.
 *
 * <h2>Events covered</h2>
 * <ul>
 *   <li>{@link SessionConnectedEvent} — a new STOMP CONNECT frame was processed
 *       (client fully connected)</li>
 *   <li>{@link SessionDisconnectEvent} — a WebSocket session closed for any reason
 *       (client sent DISCONNECT, or TCP connection dropped)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Logs every new STOMP connection.
     *
     * <p>At this point the client has completed the WebSocket handshake
     * AND sent a STOMP CONNECT frame. The session is fully usable.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("[EVENT] New WebSocket connection established");
    }

    /**
     * Broadcasts a LEAVE message when a client disconnects.
     *
     * <p>{@link StompHeaderAccessor} wraps the Spring Message that triggered
     * the event and gives us easy access to STOMP headers and session
     * attributes — including the {@code username} we stored in
     * {@link ChatController#addUser}.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Retrieve the username stored during the JOIN event
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            log.info("[EVENT] User disconnected: {}", username);

            ChatMessage leaveMessage = ChatMessage.builder()
                    .sender(username)
                    .content(username + " left the chat.")
                    .type(ChatMessage.MessageType.LEAVE)
                    .build();

            // Broadcast the LEAVE message so all remaining clients can show it
            messagingTemplate.convertAndSend("/topic/public", leaveMessage);
        }
    }
}
