package com.training.websocketdemo.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RAW WebSocket Handler — demonstrates the low-level WebSocket API.
 *
 * <p><b>Key concept:</b> At this level there is NO sub-protocol (no STOMP,
 * no JSON envelope). The server receives a raw text frame and echoes it
 * back to ALL connected clients — a simple broadcast chat.
 *
 * <h3>How it works</h3>
 * <pre>
 *   Browser ──connect──▶  /raw-ws  ──▶  RawWebSocketHandler
 *   Browser ──text frame──▶  server  ──▶  broadcast to all sessions
 * </pre>
 *
 * <h3>Session lifecycle callbacks</h3>
 * <ul>
 *   <li>{@link #afterConnectionEstablished} — called once per new client</li>
 *   <li>{@link #handleTextMessage} — called for every text frame received</li>
 *   <li>{@link #handleTransportError} — called on I/O errors</li>
 *   <li>{@link #afterConnectionClosed} — called when client disconnects</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> {@code CopyOnWriteArrayList} is used for the
 * session list because multiple threads can connect/disconnect concurrently.
 */
@Slf4j
@Component
public class RawWebSocketHandler extends TextWebSocketHandler {

    /**
     * Thread-safe list of all active WebSocket sessions.
     * CopyOnWriteArrayList: reads are lock-free; writes copy the array.
     * Perfect for lists that are rarely modified but frequently iterated.
     */
    private final CopyOnWriteArrayList<WebSocketSession> sessions =
            new CopyOnWriteArrayList<>();

    // ------------------------------------------------------------------ //
    //  Connection lifecycle                                                //
    // ------------------------------------------------------------------ //

    /**
     * Called by Spring when a new WebSocket handshake completes successfully.
     * The HTTP Upgrade has already happened; we now hold a persistent TCP
     * connection represented by {@code session}.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("[RAW WS] Client connected: {} | total sessions: {}",
                session.getId(), sessions.size());

        // Send a welcome message to the new client only
        session.sendMessage(new TextMessage(
                "Connected to Raw WebSocket! Session ID: " + session.getId()));
    }

    // ------------------------------------------------------------------ //
    //  Message handling                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Called every time a text frame arrives.
     *
     * <p>This implementation simply broadcasts the message to every connected
     * client, prefixing the sender's session ID so recipients can tell who
     * sent it. In a real app you would parse the payload (e.g. JSON) and
     * apply business logic here.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("[RAW WS] Received from {}: {}", session.getId(), payload);

        String broadcast = "[" + session.getId().substring(0, 8) + "]: " + payload;

        // Broadcast to ALL active sessions (including the sender)
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(broadcast));
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Error & close handling                                              //
    // ------------------------------------------------------------------ //

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[RAW WS] Transport error for session {}: {}",
                session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessions.remove(session);
        log.info("[RAW WS] Client disconnected: {} | reason: {} | total sessions: {}",
                session.getId(), closeStatus.getReason(), sessions.size());
    }

    /** Returns the number of currently connected raw-WS clients. */
    public int getActiveSessionCount() {
        return (int) sessions.stream().filter(WebSocketSession::isOpen).count();
    }
}
