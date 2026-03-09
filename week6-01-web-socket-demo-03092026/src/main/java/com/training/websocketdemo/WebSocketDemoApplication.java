package com.training.websocketdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the WebSocket + STOMP demo application.
 *
 * <p>Two WebSocket layers are demonstrated:
 * <ol>
 *   <li><b>Raw WebSocket</b> — low-level API via {@code WebSocketHandler}.
 *       No sub-protocol; you control the full message format.
 *       Endpoint: {@code ws://localhost:8080/raw-ws}</li>
 *   <li><b>STOMP over WebSocket</b> — higher-level pub/sub messaging using
 *       the STOMP protocol (Simple Text Oriented Messaging Protocol).
 *       Endpoint: {@code ws://localhost:8080/stomp-ws} (or SockJS fallback
 *       at {@code http://localhost:8080/stomp-ws}).</li>
 * </ol>
 *
 * <p>Open {@code http://localhost:8080} in a browser to use the chat UI.
 */
@SpringBootApplication
public class WebSocketDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketDemoApplication.class, args);
    }
}
