# Module 4 — Eureka Service Registry: Dynamic Service Discovery

**Type:** Hands-on Implementation
**Duration:** ~1.5 hours
**Prerequisites:** Module 3 complete — Config Server running and validated on port 8888
**Goal:** Implement, start, and fully validate the Eureka Service Registry; understand the dashboard, registration lifecycle, and how all future services will use it

---

## Learning Objectives

By the end of this module, participants will be able to:

1. Explain the service discovery problem and why hard-coded addresses don't scale
2. Distinguish between client-side and server-side service discovery
3. Describe the Eureka registration lifecycle — register, heartbeat, renew, evict
4. Explain why the Eureka Server must NOT register with itself
5. Write the two files needed to run a Eureka Server
6. Navigate the Eureka dashboard and interpret every section
7. Explain Eureka's self-preservation mode and when it activates
8. Describe how all future services in this project will register as Eureka clients

---

## Recap — Where We Are

```
✅ Config Server  :8888  — Running, serving properties from config-repo/
⬜ Eureka Server  :8761  — This module
⬜ User Service   :8081
⬜ Product Service :8082
⬜ Order Service  :8083
⬜ API Gateway    :8080
```

The Config Server is running. When we start the Eureka Server in this module, the Config Server will automatically register with Eureka because it has `spring-cloud-starter-netflix-eureka-client` on its classpath. We will watch this happen in real time on the Eureka dashboard.

---

## Part 1 — The Service Discovery Problem

### 1.1 Why Hard-Coded Addresses Fail

In a simple two-service system, you might hard-code the target address:

```java
// Order Service calling Product Service — the naive approach
String url = "http://localhost:8082/api/products/" + productId;
ProductDto product = restTemplate.getForObject(url, ProductDto.class);
```

This works on a developer's laptop. It breaks in every real environment:

| Scenario | What Breaks |
|---|---|
| Product Service is deployed to a different port | `localhost:8082` is wrong |
| Product Service runs on a different server | `localhost` is wrong |
| Product Service scales to 3 instances | Which instance do you call? |
| Product Service crashes and restarts on a new port | Address is stale |
| Product Service moves to a container (Docker/K8s) | Dynamic IP assigned at runtime |

In production systems, services start and stop constantly. Addresses change. Instances scale up and down. Hard-coded addresses are a maintenance nightmare.

### 1.2 The Solution — A Service Registry

A **Service Registry** is a dedicated server that acts as the phone book of your microservices system:

```
When a service STARTS:
  "Hi Eureka, I am 'product-service', I am at 192.168.1.45:8082. Register me."
  Eureka stores this → { "product-service": ["192.168.1.45:8082"] }

When a service NEEDS TO CALL another:
  "Hi Eureka, where is 'product-service'?"
  Eureka responds → ["192.168.1.45:8082"]
  Caller picks one and calls it

When a service STOPS or CRASHES:
  Eureka detects missed heartbeats → removes it from registry
  Next lookup returns empty or remaining healthy instances
```

No hard-coded addresses. No stale configuration. Dynamic, self-updating.

### 1.3 Client-Side vs Server-Side Discovery

There are two patterns for implementing service discovery:

**Client-Side Discovery (Netflix Eureka — what we use):**

```
Order Service                 Eureka Registry             Product Service
      │                            │                            │
      │── Where is product-svc? ──►│                            │
      │◄── 192.168.1.45:8082 ──────│                            │
      │                            │                            │
      │── GET /api/products/1 ─────────────────────────────────►│
      │◄── ProductDto ─────────────────────────────────────────│
```

The caller (Order Service) queries Eureka, selects an instance, and calls it directly. Load balancing is done **in the caller** (Spring Cloud LoadBalancer).

**Server-Side Discovery (AWS ALB, Kubernetes, NGINX):**

```
Order Service            Load Balancer / Service Mesh        Product Service
      │                            │                            │
      │── GET /api/products/1 ────►│                            │
      │                            │── (queries registry) ─────►│
      │                            │◄── ProductDto ─────────────│
      │◄── ProductDto ─────────────│
```

The caller doesn't know or care about Eureka — it just calls a fixed load balancer address. The load balancer handles discovery and routing internally.

| | Client-Side (Eureka) | Server-Side (K8s) |
|---|---|---|
| Where is registry queried? | In the calling service | In the load balancer |
| Load balancing | In the client | In the infrastructure |
| Language coupling | Java-centric (Ribbon/LoadBalancer) | Language-agnostic |
| Complexity | Higher in service code | Higher in infrastructure |
| Used in | Spring Cloud, Netflix stack | Kubernetes, AWS, Azure |

Our project uses client-side discovery with Eureka because it integrates naturally with Spring Cloud Gateway and OpenFeign.

---

## Part 2 — The Eureka Lifecycle in Detail

Understanding the Eureka lifecycle helps you diagnose problems when services don't appear in the registry or disappear unexpectedly.

### 2.1 Registration

When a Eureka client (any service with `spring-cloud-starter-netflix-eureka-client`) starts:

1. It creates an `InstanceInfo` object describing itself:
   - Application name (`spring.application.name`)
   - IP address or hostname
   - Port
   - Health check URL (`/actuator/health`)
   - Status (`STARTING` → `UP`)

2. It sends a `POST` to `http://localhost:8761/eureka/apps/{appName}` with this info

3. Eureka stores it in an in-memory registry (`ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>`)

4. The instance appears in the dashboard and is returned in discovery responses

### 2.2 Heartbeat (Renewal)

After registration, the client sends a heartbeat every **30 seconds** by default:

```
PUT http://localhost:8761/eureka/apps/{appName}/{instanceId}
```

This tells Eureka: "I am still alive." Each heartbeat resets the instance's expiry timer.

Configuration:
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 30   # How often to send heartbeat (default: 30)
    lease-expiration-duration-in-seconds: 90 # Eureka removes instance after this many
                                              # seconds of no heartbeats (default: 90)
```

### 2.3 Deregistration

When a service shuts down gracefully (e.g. Ctrl+C on the terminal), it sends:
```
DELETE http://localhost:8761/eureka/apps/{appName}/{instanceId}
```

Eureka immediately removes it from the registry. Services are removed without waiting for the heartbeat timeout.

If a service crashes (ungraceful shutdown), the heartbeat stops. After `lease-expiration-duration-in-seconds` (default: 90 seconds), Eureka marks the instance as expired and removes it.

### 2.4 Client-Side Registry Cache

Eureka clients **do not query Eureka on every request**. They maintain a local cache:

```
Eureka Client (in Order Service)
  └── Local registry cache (updated every 30 seconds)
        └── { "product-service": ["192.168.1.45:8082", "192.168.1.46:8082"] }

When Feign calls product-service:
  → Reads from local cache (no network call to Eureka)
  → Spring Cloud LoadBalancer picks one instance (round-robin)
  → Calls that instance directly
```

This cache means there is up to a 30-second delay before service changes (new instance up, old instance down) are visible to callers. In development this sometimes causes confusion — a service appears down but callers still route to it for 30 more seconds.

### 2.5 Self-Preservation Mode

Eureka has a safety feature called **self-preservation mode**. The idea:

> If Eureka suddenly stops receiving heartbeats from many services at once, it might be a **network partition** (Eureka's network is having issues) rather than all services actually being down. In that case, Eureka should NOT evict them — they might still be serving traffic on their own network segment.

When more than **15% of expected heartbeats are missed** in a single minute, Eureka enters self-preservation mode:

```
EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT.
RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE.
```

You will see this message in the dashboard (red banner) during local development when:
- You start Eureka but no services have registered yet
- You stop services abruptly
- You restart services frequently during development

**In development this is noise.** We disable it with:
```yaml
eureka:
  server:
    wait-time-in-ms-when-sync-empty: 0
```

This eliminates the initial delay Eureka imposes when first starting (it waits for peer synchronisation, which doesn't apply in a single-node local setup).

---

## Part 3 — File Structure to Create

```
eureka-server/
├── pom.xml                                          ← already done in Module 2
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── ecommerce/
        │           └── eureka/
        │               └── EurekaServerApplication.java    ← Step 1
        └── resources/
            └── application.yml                             ← Step 2
```

Two files — same pattern as the Config Server. This simplicity is intentional. Infrastructure services are thin — they exist to provide a platform, not to contain business logic.

---

## Part 4 — Step-by-Step Implementation

### Step 1 — Create the Main Application Class

```
eureka-server/src/main/java/com/ecommerce/eureka/EurekaServerApplication.java
```

```java
package com.ecommerce.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer             // ← Activates the Eureka Server auto-configuration
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**What `@EnableEurekaServer` activates:**

Behind the scenes, this single annotation triggers auto-configuration that:
- Starts the Eureka Server's in-memory registry (`PeerAwareInstanceRegistry`)
- Registers the Eureka REST API endpoints (`/eureka/apps/**`) that clients POST registrations to
- Serves the HTML dashboard at `/`
- Starts the eviction timer that checks for missed heartbeats
- Configures peer replication (for multi-node Eureka clusters — not needed for local dev)

### Step 2 — Write the application.yml

```
eureka-server/src/main/resources/application.yml
```

```yaml
# ── Server Configuration ──────────────────────────────────────────────
server:
  port: 8761
  # 8761 is the Eureka Server convention.
  # Eureka clients look for the server at this port by default.
  # Changing it requires updating defaultZone in every client service.

# ── Spring Application ────────────────────────────────────────────────
spring:
  application:
    name: eureka-server

# ── Eureka Configuration ──────────────────────────────────────────────
eureka:
  instance:
    hostname: localhost
    # The hostname this Eureka server advertises to peers.
    # In a multi-node Eureka cluster, this would be the server's actual FQDN.
    # For local development, localhost is correct.

  client:
    # ── THE MOST IMPORTANT CONFIG FOR EUREKA SERVER ──
    #
    # By default, ANY application with eureka-client on the classpath
    # will try to register itself with Eureka and fetch the registry.
    #
    # The Eureka Server has the eureka-client library on its own classpath
    # (it uses it internally). Without these two lines, the Eureka Server
    # would try to register with ITSELF — and fail, because it is the server.
    #
    register-with-eureka: false     # Do NOT register this server as a service instance
    fetch-registry: false           # Do NOT fetch the registry (we ARE the registry)

    service-url:
      # This is the URL Eureka uses for peer replication in a cluster.
      # For single-node local dev, it points to itself — which is fine
      # because register-with-eureka: false prevents a registration loop.
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/

  server:
    # In a multi-node Eureka cluster, Eureka waits for peer sync before
    # serving requests. In single-node local dev there are no peers,
    # so Eureka would wait unnecessarily for 5 minutes.
    # Setting this to 0 eliminates that startup delay.
    wait-time-in-ms-when-sync-empty: 0

# ── Actuator ──────────────────────────────────────────────────────────
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

**Why `register-with-eureka: false` is critical:**

The `spring-cloud-starter-netflix-eureka-server` dependency brings in the Eureka client libraries as a transitive dependency (the server uses client code to replicate between peers). Without `register-with-eureka: false`, the Eureka Server would:

1. Start the Eureka Server
2. Immediately try to register itself as a client with itself
3. That registration call would hit a server that is not yet fully initialised
4. This creates a race condition with confusing errors on startup

```
Without register-with-eureka: false:

Eureka Server starting
    │
    ├── Starts the server registry (port 8761)
    │
    └── Tries to register ITSELF as a client
            │
            └── POST http://localhost:8761/eureka/apps/eureka-server
                    │
                    └── Server not ready yet → Connection refused or error
                         → Retry loop → Confusing log spam
```

With `register-with-eureka: false`, step 2 is skipped entirely.

---

## Part 5 — Starting Eureka

### 5.1 Startup Sequence (Both Services)

At this point you should have Config Server already running. Open a new terminal for Eureka:

**Terminal 1 (already running):**
```bash
cd config-server && mvn spring-boot:run
# Should already be up on port 8888
```

**Terminal 2 (new):**
```bash
cd ~/microservices-mini-project/eureka-server
mvn spring-boot:run
```

### 5.2 What to Look for in the Eureka Startup Logs

```log
INFO  EurekaServerApplication - Starting EurekaServerApplication

INFO  EurekaServerContext - Initialized server context
INFO  PeerAwareInstanceRegistryImpl - Got 0 instances from neighboring DS node
INFO  PeerAwareInstanceRegistryImpl - Renew threshold is: 0
INFO  PeerAwareInstanceRegistryImpl - Changing status to UP

INFO  EurekaServerApplication - Started EurekaServerApplication in 6.3 seconds
```

Key lines to notice:
- `Got 0 instances from neighboring DS node` — No peer Eureka servers (single-node setup — expected)
- `Changing status to UP` — Eureka is open for business

### 5.3 Watch Config Server Register

Switch to **Terminal 1** (Config Server logs). Within 30 seconds of Eureka starting, you should see:

```log
INFO  DiscoveryClient - Getting all instance registry info from the eureka server
INFO  DiscoveryClient - The response status is 200
INFO  DiscoveryClient - Starting heartbeats
INFO  EurekaAutoServiceRegistration - Updating port to 8888
```

The Config Server has registered itself with Eureka. Now switch to the Eureka dashboard to see it.

---

## Part 6 — The Eureka Dashboard

### 6.1 Opening the Dashboard

Navigate to `http://localhost:8761` in your browser.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  Spring Eureka                                               System Status   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  General Info                                                                │
│  ─────────────────────────────────────────────────────────────────────────  │
│  total-avail-memory          512mb                                           │
│  environment                 test                                            │
│  num-of-cpus                 8                                               │
│  current-memory-usage        128mb (25%)                                     │
│  server-uptime               00:02                                           │
│  registered-replicas                                                         │
│  unavailable-replicas                                                        │
│  available-replicas                                                          │
│                                                                              │
│  DS Replicas                                                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│  localhost                                                                   │
│                                                                              │
│  Instances currently registered with Eureka                                  │
│  ─────────────────────────────────────────────────────────────────────────  │
│  Application    AMIs  Availability Zones  Status                             │
│  ──────────────────────────────────────────────────────────────────────────  │
│  CONFIG-SERVER  n/a   (1)                 UP (1) - localhost:config-server   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Reading the Dashboard — Section by Section

**General Info section:**

| Field | What It Tells You |
|---|---|
| `total-avail-memory` | JVM heap available to Eureka — monitor for memory pressure |
| `environment` | Spring environment (`test` in dev — harmless label) |
| `num-of-cpus` | Host CPU count |
| `server-uptime` | How long Eureka has been running |
| `registered-replicas` | Other Eureka servers in a cluster (empty in single-node) |
| `available-replicas` | Healthy peer Eureka nodes (empty in single-node) |

**Instances section — the most important part:**

| Column | Meaning |
|---|---|
| `Application` | The `spring.application.name` in UPPERCASE |
| `AMIs` | AWS AMI ID (n/a in local dev) |
| `Availability Zones` | Number of instances in each zone, shown as `(count)` |
| `Status` | `UP (count) - hostname:service-name:port` |

**Reading the Status column:**

```
UP (1) - localhost:config-server:8888
│         │                     │
│         │                     └── Port the service is on
│         └── Hostname or IP of the instance
└── Status and instance count
```

**Status values:**

| Status | Meaning |
|---|---|
| `UP` | Healthy, accepting traffic |
| `DOWN` | Registered but health check is failing |
| `STARTING` | Service is starting up, not ready yet |
| `OUT_OF_SERVICE` | Manually taken out of rotation |
| `UNKNOWN` | Status cannot be determined |

### 6.3 The Instance Link

Clicking the `localhost:config-server:8888` link takes you to the service's `/actuator/info` endpoint (if configured) or home page. This lets you directly access any registered service from the Eureka dashboard — useful for debugging.

### 6.4 The Red Banner — Self-Preservation Warning

If you see a red banner at the top:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN         │
│ THEY'RE NOT. RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES     │
│ ARE NOT BEING EXPIRED JUST TO BE SAFE.                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

**This is normal in local development** with few services. Self-preservation activates because the number of received heartbeats is below the 85% threshold. As more services register, this banner typically disappears. It can be suppressed in dev with:

```yaml
eureka:
  server:
    enable-self-preservation: false  # Development only — never disable in production
```

We leave it enabled (the default) in our project because it is correct production behaviour and good to understand.

---

## Part 7 — The Eureka REST API

The Eureka dashboard is a visual wrapper around a full REST API. Understanding this API helps with debugging.

### 7.1 Query All Registered Instances

```bash
curl -H "Accept: application/json" http://localhost:8761/eureka/apps | python3 -m json.tool
```

Response structure:
```json
{
  "applications": {
    "versions__delta": "1",
    "apps__hashcode": "UP_1_",
    "application": [
      {
        "name": "CONFIG-SERVER",
        "instance": [
          {
            "instanceId": "localhost:config-server:8888",
            "hostName": "localhost",
            "app": "CONFIG-SERVER",
            "ipAddr": "127.0.0.1",
            "status": "UP",
            "port": { "$": 8888, "@enabled": "true" },
            "healthCheckUrl": "http://localhost:8888/actuator/health",
            "lastUpdatedTimestamp": "1709294400000",
            "lastDirtyTimestamp": "1709294395000"
          }
        ]
      }
    ]
  }
}
```

### 7.2 Query a Specific Service

```bash
# All instances of a specific service
curl -H "Accept: application/json" http://localhost:8761/eureka/apps/CONFIG-SERVER | python3 -m json.tool
```

### 7.3 Query the Registry After Adding More Services

Bookmark this command. You will run it repeatedly throughout the training to verify services are registered:

```bash
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
apps = data.get('applications', {}).get('application', [])
if not apps:
    print('No services registered yet')
else:
    for app in apps:
        name = app['name']
        instances = app['instance'] if isinstance(app['instance'], list) else [app['instance']]
        for inst in instances:
            print(f\"{name:30} {inst['status']:10} {inst['instanceId']}\")
"
```

Expected output (after Config Server registered):
```
CONFIG-SERVER                  UP         localhost:config-server:8888
```

After all services are running (end of Module 8), the output will be:
```
CONFIG-SERVER                  UP         localhost:config-server:8888
USER-SERVICE                   UP         localhost:user-service:8081
PRODUCT-SERVICE                UP         localhost:product-service:8082
ORDER-SERVICE                  UP         localhost:order-service:8083
API-GATEWAY                    UP         localhost:api-gateway:8080
```

---

## Part 8 — How Future Services Will Register as Clients

Every business service (User, Product, Order) and the API Gateway will have this in their `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

And this in their `application.yml`:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true     # default — can be omitted
    fetch-registry: true           # default — can be omitted
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
```

**`prefer-ip-address: true`** — registers the IP address instead of the hostname. On local development machines, hostname resolution can be unreliable. IP is always reachable.

**`instance-id: ${spring.application.name}:${server.port}`** — gives each instance a unique, human-readable ID. Without this, Eureka auto-generates an ID (e.g. `john-macbook.local:user-service:8081`) which includes the machine hostname and can be unpredictable.

### 8.1 What Happens When a Client Service Registers

```
1. Service starts
2. Spring Boot auto-configuration detects eureka-client on classpath
3. Creates DiscoveryClient bean
4. DiscoveryClient reads eureka.client.service-url.defaultZone
5. POSTs registration: POST http://localhost:8761/eureka/apps/USER-SERVICE
   Body: { instanceId, hostName, ipAddr, port, status: "STARTING", healthCheckUrl, ... }
6. Eureka stores registration
7. Service finishes startup, changes status to UP
8. PUTs status update: PUT http://localhost:8761/eureka/apps/USER-SERVICE/{instanceId}/status?value=UP
9. Starts heartbeat thread (every 30 seconds)
10. Dashboard shows: USER-SERVICE  UP (1)  localhost:user-service:8081
```

---

## Part 9 — Eureka in a Production Cluster (For Awareness)

In production, running a single Eureka node is a single point of failure. If Eureka goes down, existing services can still call each other (they have cached registries) but new services cannot register and changes are not propagated.

Production Eureka is typically deployed as a **cluster of 2–3 nodes** that replicate to each other:

```yaml
# eureka-server-1/application.yml
eureka:
  instance:
    hostname: eureka1.company.com
  client:
    register-with-eureka: true   # Register WITH peers (not yourself)
    fetch-registry: true
    service-url:
      defaultZone: http://eureka2.company.com:8761/eureka/,
                   http://eureka3.company.com:8761/eureka/
```

Each Eureka node registers with all other nodes. If one goes down, the others continue serving the registry. This is beyond scope for this training but important to know for production deployments.

---

## Part 10 — Validation Checkpoint

### 10.1 Eureka Dashboard Validation

```
□ http://localhost:8761 shows the Eureka dashboard
□ CONFIG-SERVER appears in "Instances currently registered" with status UP
□ Status shows: UP (1) - localhost:config-server:8888
□ Clicking the instance link opens http://localhost:8888/actuator/info or similar
```

### 10.2 REST API Validation

```bash
# Should return JSON with CONFIG-SERVER registered
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
```

```
□ Response contains "CONFIG-SERVER" application
□ Instance status is "UP"
□ instanceId shows: localhost:config-server:8888
□ healthCheckUrl shows: http://localhost:8888/actuator/health
```

### 10.3 Actuator Validation

```bash
# Eureka Server health
curl http://localhost:8761/actuator/health
# Expected: {"status":"UP"}
```

```
□ Eureka actuator health returns UP
□ Config Server actuator health still returns UP (still running from Module 3)
```

### 10.4 Log Validation

```
□ Eureka Server logs show "Started EurekaServerApplication"
□ Config Server logs show heartbeat messages: "DiscoveryClient - Getting all instance registry info"
□ No ERROR level log entries in either service (WARN for self-preservation is acceptable)
```

---

## Part 11 — Common Mistakes

### Mistake 1 — Eureka Server Also Registers Itself

**Symptom:** `EUREKA-SERVER` appears in the instance list of the dashboard.

**Cause:** Missing or incorrect configuration:
```yaml
eureka:
  client:
    register-with-eureka: false   # This line is missing
    fetch-registry: false         # This line is missing
```

**Fix:** Add both lines exactly as shown above. Restart Eureka.

---

### Mistake 2 — Services Not Appearing After Starting

**Symptom:** You started User Service but it doesn't show in the Eureka dashboard.

**Cause A — Wrong Eureka URL in the client service:**
```yaml
# Wrong:
eureka.client.service-url.defaultZone: http://localhost:8762/eureka/
# Fix:
eureka.client.service-url.defaultZone: http://localhost:8761/eureka/
```

**Cause B — Service is still starting.** Eureka registration happens after the service is fully up. Give it 10–15 seconds after seeing "Started XxxApplication".

**Cause C — Initial registration delay.** Spring Cloud Netflix has a 40-second initial registration delay by default (a safety feature inherited from Netflix internals). In development, reduce this:
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 5      # Heartbeat every 5s (default: 30)
    lease-expiration-duration-in-seconds: 15  # Evict after 15s of silence (default: 90)
  client:
    initial-instance-info-replication-interval-seconds: 5  # First registration delay (default: 40)
    registry-fetch-interval-seconds: 5        # Cache refresh (default: 30)
```

---

### Mistake 3 — Services Disappear from Dashboard Shortly After Appearing

**Symptom:** USER-SERVICE shows UP, then disappears 90 seconds later.

**Cause:** The service's heartbeat is not reaching Eureka. Usually because:
- The service crashed after startup
- A network/firewall issue (unlikely on localhost)
- Eureka's self-preservation mode is interfering (check the red banner)

**Fix:** Check the service's logs for errors after startup. Ensure the service process is still running.

---

### Mistake 4 — Dashboard Shows `DOWN` Instead of `UP`

**Symptom:** Service is registered but status is `DOWN`.

**Cause:** The service's `/actuator/health` endpoint is returning a non-200 status. This usually means the service's database connection is failing or some other dependency is unhealthy.

**Fix:** Call the service's health endpoint directly:
```bash
curl http://localhost:8081/actuator/health
```
Read the response — it will name which health indicator is failing (database, disk, etc.).

---

## Module 4 — Summary

| What We Built | Key Learning |
|---|---|
| `EurekaServerApplication.java` with `@EnableEurekaServer` | One annotation — Eureka Server + dashboard + REST API |
| `register-with-eureka: false` + `fetch-registry: false` | Prevents the server from trying to register with itself |
| `wait-time-in-ms-when-sync-empty: 0` | Eliminates the initial 5-minute peer sync wait in single-node dev |
| Dashboard at `http://localhost:8761` | Live view of all registered services and their health |
| Eureka REST API | Full programmatic access to the registry for tooling and debugging |
| Heartbeat lifecycle | Register → heartbeat every 30s → evict after 90s of silence |
| Self-preservation mode | Safety feature that prevents mass eviction during network partitions |

---

## What's Next — Module 5

With both infrastructure services running, we build the **first business service: User Service**.

This is where the real work begins. In Module 5 we will:
1. Define the `User` JPA entity and MySQL schema
2. Create the `UserRepository` with Spring Data JPA
3. Implement `BCryptPasswordEncoder` for password hashing
4. Build `JwtUtil` to generate and validate JWT tokens
5. Write the `UserService` with register and login logic
6. Expose `POST /api/users/register` and `POST /api/users/login` REST endpoints
7. Configure Spring Security to permit all requests (gateway handles auth)
8. Connect to Config Server for datasource config
9. Validate registration and login with cURL

After Module 5, we will have a fully working authentication service.

---

*End of Module 4*
