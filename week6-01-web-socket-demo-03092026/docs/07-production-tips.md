# 07 — Production Tips & Advanced Patterns

---

## 1. Replacing the Simple Broker with RabbitMQ

The in-memory simple broker is fine for single-server demos. In production
you want an **external broker** so:
- Multiple application instances share subscriptions
- Messages survive server restarts
- Better performance for high message volume

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    // Replace enableSimpleBroker with:
    registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("rabbitmq.internal")
            .setRelayPort(61613)          // RabbitMQ STOMP plugin port
            .setClientLogin("guest")
            .setClientPasscode("guest")
            .setSystemLogin("guest")
            .setSystemPasscode("guest");

    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

RabbitMQ setup:
```bash
rabbitmq-plugins enable rabbitmq_stomp
```

---

## 2. Securing WebSocket with Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/stomp-ws/**").permitAll()  // handshake endpoint
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
```

For STOMP-level authorization (authorize which destinations specific roles can access):

```java
@Configuration
public class WebSocketSecurityConfig
        extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpSubscribeDestMatchers("/user/**", "/topic/public").authenticated()
            .simpDestMatchers("/app/**").authenticated()
            .anyMessage().denyAll();
    }
}
```

---

## 3. Intercepting the Handshake (Custom Headers)

Use a `HandshakeInterceptor` to read HTTP headers/cookies during the upgrade:

```java
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // Copy HTTP session attributes to WebSocket session attributes
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                attributes.put("sessionId", session.getId());
            }
        }
        return true;  // return false to reject the connection
    }

    @Override
    public void afterHandshake(...) {}
}
```

Register it:

```java
registry.addEndpoint("/stomp-ws")
        .addInterceptors(new AuthHandshakeInterceptor())
        .withSockJS();
```

---

## 4. Scaling Horizontally — Sticky Sessions

When you run multiple app instances behind a load balancer, a user's WebSocket
connection goes to one specific server. But `convertAndSendToUser("alice", …)`
only knows about sessions on the **current** server.

Solutions:
- **Sticky sessions** (session affinity) — load balancer always routes the same
  user to the same server. Simple but limits scale.
- **External broker relay** (RabbitMQ/ActiveMQ) — all servers connect to the same
  broker. Messages are delivered regardless of which server the user is on.
  This is the production-grade solution.

---

## 5. Connection Limits and Thread Tuning

Each WebSocket session holds a TCP connection. A single server can handle:
- ~10,000–50,000 concurrent WebSocket connections (depends on RAM and OS limits)
- Tune `ulimit -n` (file descriptor limit) on Linux

```properties
# Increase thread pool for inbound/outbound channels
spring.websocket.message-size=65536
```

For high-load, configure message channel thread pools:

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8)
            .queueCapacity(100);
}

@Override
public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8);
}
```

---

## 6. Monitoring WebSocket Connections

```java
// REST endpoint to check active connections
@RestController
public class AdminController {

    private final RawWebSocketHandler rawHandler;

    @GetMapping("/admin/ws-stats")
    public Map<String, Object> stats() {
        return Map.of(
            "rawConnections", rawHandler.getActiveSessionCount()
        );
    }
}
```

For STOMP session count, Spring doesn't expose it directly. Track it yourself
via `SessionConnectedEvent` / `SessionDisconnectEvent` counters.

---

## 7. Rate Limiting

Prevent a single client from flooding the server:

```java
@ChannelInterceptor
public class RateLimitInterceptor implements ChannelInterceptor {
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            int count = counters.computeIfAbsent(sessionId, k -> new AtomicInteger())
                                .incrementAndGet();
            if (count > 100) { // 100 messages per window
                throw new MessagingException("Rate limit exceeded");
            }
        }
        return message;
    }
}
```

Register the interceptor:

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new RateLimitInterceptor());
}
```
