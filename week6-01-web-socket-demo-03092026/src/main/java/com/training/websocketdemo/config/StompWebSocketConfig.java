package com.training.websocketdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for <b>STOMP over WebSocket</b> — the recommended, production-
 * grade way to use WebSocket in Spring applications.
 *
 * <h2>Why STOMP?</h2>
 * Raw WebSocket gives you a byte pipe — that's it.  You have to invent your
 * own message format, routing, error handling, heartbeats, etc.
 * <p>
 * STOMP (Simple Text Oriented Messaging Protocol) adds a thin layer on top:
 * <ul>
 *   <li>Named <b>destinations</b> (like message topics / queues)</li>
 *   <li>SUBSCRIBE / SEND / ACK commands</li>
 *   <li>Header-based metadata (content-type, correlation-id, …)</li>
 *   <li>Built-in heartbeat negotiation</li>
 * </ul>
 *
 * <h2>@EnableWebSocketMessageBroker</h2>
 * Enables a WebSocket message-handling infrastructure backed by a message
 * broker.  Spring wires together:
 * <pre>
 *   Client  ←──WebSocket/SockJS──→  Spring  ←──in-memory broker──→  all subscribers
 * </pre>
 *
 * <h2>Message flow</h2>
 * <pre>
 *   1. Client connects to  ws://localhost:8080/stomp-ws
 *   2. Client sends a STOMP SUBSCRIBE frame to /topic/public
 *   3. Client sends a STOMP SEND frame to   /app/chat.sendMessage
 *   4. Spring routes /app/** to @MessageMapping methods in controllers
 *   5. Controller uses @SendTo("/topic/public")  to publish the reply
 *   6. In-memory broker fans out the reply to all /topic/public subscribers
 * </pre>
 *
 * <h2>Destination prefixes</h2>
 * <ul>
 *   <li>{@code /app}   — goes to @MessageMapping controllers (application logic)</li>
 *   <li>{@code /topic} — goes directly to the broker (public topics, fan-out)</li>
 *   <li>{@code /queue} — goes directly to the broker (private queues, point-to-point)</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // ------------------------------------------------------------------ //
    //  Step 1: Register the STOMP handshake endpoint                      //
    // ------------------------------------------------------------------ //

    /**
     * Registers the WebSocket (or SockJS) handshake endpoint.
     *
     * <p>Clients connect here first to upgrade HTTP → WebSocket, then
     * communicate using the STOMP sub-protocol.
     *
     * <p><b>SockJS fallback:</b> {@code withSockJS()} makes Spring serve
     * an additional HTTP-polling fallback for environments where WebSocket
     * is blocked (corporate proxies, old browsers). The client library
     * (SockJS.js) automatically negotiates the best available transport.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/stomp-ws")          // WebSocket endpoint path
            .setAllowedOriginPatterns("*")     // CORS — restrict in production
            .withSockJS();                     // enable SockJS fallback transports
    }

    // ------------------------------------------------------------------ //
    //  Step 2: Configure the message broker                               //
    // ------------------------------------------------------------------ //

    /**
     * Configures routing between client destinations and the in-memory
     * message broker.
     *
     * <p><b>enableSimpleBroker</b> — starts an in-process broker that handles
     * subscription management and message delivery for the given prefixes.
     * For production you can replace this with a full broker relay
     * (RabbitMQ, ActiveMQ) via {@code enableStompBrokerRelay()}.
     *
     * <p><b>setApplicationDestinationPrefixes</b> — messages sent to
     * destinations starting with {@code /app} are routed to
     * {@code @MessageMapping} methods BEFORE hitting the broker.
     * This is where you add business logic, validation, enrichment, etc.
     *
     * <p><b>setUserDestinationPrefix</b> — enables user-specific messaging.
     * A client can subscribe to {@code /user/queue/reply} and the server
     * can send to {@code /user/{username}/queue/reply}.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker handles /topic (fan-out) and /queue (point-to-point)
        registry.enableSimpleBroker("/topic", "/queue");

        // Application prefix — STOMP SEND to /app/foo → @MessageMapping("/foo")
        registry.setApplicationDestinationPrefixes("/app");

        // User-specific destination prefix (server → specific user)
        registry.setUserDestinationPrefix("/user");
    }
}
