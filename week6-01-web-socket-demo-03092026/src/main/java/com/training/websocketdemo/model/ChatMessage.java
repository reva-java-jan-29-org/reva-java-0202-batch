package com.training.websocketdemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single chat message transported over STOMP.
 *
 * <p>This object is automatically serialized/deserialized to JSON by
 * Spring's built-in Jackson {@code MessageConverter}. You do NOT need
 * any extra configuration for this — Spring does it automatically when
 * {@code spring-boot-starter-websocket} is on the classpath.
 *
 * <h3>Message Types</h3>
 * <ul>
 *   <li>{@link MessageType#CHAT} — normal user message</li>
 *   <li>{@link MessageType#JOIN} — user joined the chat room</li>
 *   <li>{@link MessageType#LEAVE} — user left the chat room</li>
 * </ul>
 *
 * <h3>Wire format (JSON over STOMP)</h3>
 * <pre>
 * {
 *   "sender":  "Alice",
 *   "content": "Hello everyone!",
 *   "type":    "CHAT"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** Who sent the message. */
    private String sender;

    /** The text body of the message. */
    private String content;

    /** One of CHAT / JOIN / LEAVE. */
    private MessageType type;

    /** Enum to categorize message intent. */
    public enum MessageType {
        CHAT,   // regular message
        JOIN,   // user just connected
        LEAVE   // user disconnecting
    }
}
