package com.training.websocketdemo.config;

import com.training.websocketdemo.handler.RawWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuration for the <b>raw WebSocket</b> endpoint.
 *
 * <h3>Annotations explained</h3>
 * <ul>
 *   <li>{@code @EnableWebSocket} — activates Spring's raw WebSocket support
 *       (NOT the STOMP broker; that is handled by a separate config).</li>
 *   <li>{@code WebSocketConfigurer} — the interface we implement to register
 *       endpoint paths → handler mappings.</li>
 * </ul>
 *
 * <h3>Endpoint</h3>
 * <pre>
 *   ws://localhost:8080/raw-ws
 * </pre>
 *
 * <h3>CORS</h3>
 * {@code setAllowedOrigins("*")} permits connections from any origin.
 * In production, restrict this to your frontend's domain.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final RawWebSocketHandler rawWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            // Register our handler at /raw-ws
            .addHandler(rawWebSocketHandler, "/raw-ws")
            // Allow all origins so the browser demo page works without CORS errors
            .setAllowedOrigins("*");

        /*
         * NOTE: We do NOT add a SockJS fallback here intentionally.
         * SockJS is shown in the STOMP config below. Raw WebSocket
         * requires a browser that truly supports WebSocket (all modern browsers do).
         */
    }
}
