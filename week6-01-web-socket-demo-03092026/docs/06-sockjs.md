# 06 — SockJS Fallback

---

## 1. The Problem SockJS Solves

WebSocket requires a **persistent TCP connection** that passes through proxies,
load balancers, and firewalls without buffering or interference.

Many corporate environments break WebSocket:
- Transparent HTTP proxies that don't understand `101 Switching Protocols`
- Load balancers that terminate long connections
- Firewalls that block non-standard HTTP traffic

**SockJS** provides a JavaScript client + server protocol that:
1. Tries native WebSocket first
2. Falls back to HTTP-based transports if WebSocket fails
3. Presents the **same API** to your application code regardless of transport

---

## 2. SockJS Transport Priority

```
SockJS tries in order:
  1. WebSocket               ← best: true full-duplex
  2. xhr-streaming           ← server pushes via chunked HTTP response
  3. xdr-streaming           ← IE cross-domain streaming
  4. iframe-eventsource      ← via hidden iframe + EventSource
  5. iframe-htmlfile         ← IE ActiveX-based streaming
  6. xhr-polling             ← request/response polling (worst case)
  7. xdr-polling             ← IE cross-domain polling
  8. iframe-xhr-polling      ← polling via hidden iframe
  9. jsonp-polling           ← JSONP (very legacy)
```

In practice, WebSocket succeeds in modern environments. SockJS is a safety net.

---

## 3. Enabling SockJS on the Server

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/stomp-ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();        // ← this one call enables all fallback transports
}
```

Spring Boot automatically serves the SockJS JavaScript library at:
```
/stomp-ws/sockjs.js
/stomp-ws/sockjs.min.js
```

---

## 4. SockJS Client Code

```html
<!-- Include SockJS and STOMP.js -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>

<script>
  // SockJS creates the transport — NOT new WebSocket()
  const socket = new SockJS('/stomp-ws');

  // STOMP wraps SockJS
  const stompClient = Stomp.over(socket);

  stompClient.connect({}, function(frame) {
    // From here, identical to native WebSocket + STOMP
    stompClient.subscribe('/topic/public', handler);
  });
</script>
```

The key difference from raw WebSocket:
```javascript
// Raw WebSocket:
const ws = new WebSocket('ws://localhost:8080/raw-ws');

// SockJS (starts with http, not ws):
const socket = new SockJS('http://localhost:8080/stomp-ws');
```

SockJS uses `http://` / `https://` URLs, not `ws://` / `wss://`. The library
handles the upgrade internally.

---

## 5. SockJS Info Request

Before connecting, SockJS makes a GET request to gather server capabilities:

```
GET /stomp-ws/info

Response:
{
  "entropy": 123456789,
  "origins": ["*:*"],
  "cookie_needed": false,
  "websocket": true          ← tells client if WS is available
}
```

If `"websocket": false`, SockJS skips WebSocket and goes straight to HTTP
fallbacks.

---

## 6. SockJS URL Pattern

SockJS uses a specific URL structure for its sessions:

```
/stomp-ws/{server_id}/{session_id}/{transport}

Example:
/stomp-ws/000/4xkjhf8a/websocket
/stomp-ws/000/4xkjhf8a/xhr_streaming
/stomp-ws/000/4xkjhf8a/xhr
```

- `server_id` — random 3-digit number (for load balancing hints)
- `session_id` — unique per SockJS session
- `transport` — the transport type being used

---

## 7. When to Use vs Skip SockJS

**Use SockJS when:**
- Users may be on corporate networks with restrictive proxies
- You need maximum browser compatibility
- You don't control the network environment

**Skip SockJS (native WebSocket only) when:**
- You control the network (internal app, known environment)
- You need the absolute lowest latency (SockJS adds framing overhead)
- You're building a mobile app (not a browser)
- You're using raw WebSocket (not STOMP)

```java
// Without SockJS fallback:
registry.addEndpoint("/stomp-ws").setAllowedOriginPatterns("*");
// ← no .withSockJS()

// Client then uses:
const ws = new WebSocket('ws://localhost:8080/stomp-ws');
const stompClient = Stomp.over(ws);
```
