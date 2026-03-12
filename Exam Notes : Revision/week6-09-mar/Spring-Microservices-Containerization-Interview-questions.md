# Week 6 — Spring Boot, Microservices & Containerization Interview Questions

> 5–8 questions per topic, formatted as Q&A study notes for learning and revision.

---

## TABLE OF CONTENTS

- [Spring Boot](#1-spring-boot)
- [Spring Boot Actuator](#2-spring-boot-actuator)
- [Microservices Overview](#3-microservices-overview)
- [MSA Characteristics, Advantages & Disadvantages](#4-msa-characteristics-advantages--disadvantages)
- [Microservices with Messaging](#5-microservices-with-messaging)
- [Apache Kafka](#6-apache-kafka)
- [Microservice Design Patterns](#7-microservice-design-patterns)
- [Spring Cloud](#8-spring-cloud)
- [Docker](#9-docker)
- [DevOps and CI/CD](#10-devops-and-cicd)

---

# 1. Spring Boot

**Q1. What is Spring Boot and how is it different from the Spring Framework?**
> Spring Framework is a comprehensive Java application framework, but it requires extensive manual configuration (XML or Java-based) to wire components together. **Spring Boot** is built on top of Spring Framework and provides:
> - **Auto-configuration** — automatically configures beans based on what's on the classpath.
> - **Embedded servers** — comes with embedded Tomcat/Jetty/Undertow so you don't need to deploy a WAR.
> - **Starter dependencies** — pre-packaged dependency bundles (e.g., `spring-boot-starter-web`).
> - **Production-ready features** — health checks, metrics, externalized config out of the box.
> The goal: **convention over configuration** — get a working app running with minimal boilerplate.

**Q2. What is auto-configuration in Spring Boot? How does it work?**
> Auto-configuration automatically wires Spring beans based on the libraries present on the classpath. When you add `spring-boot-starter-data-jpa`, Spring Boot detects Hibernate and a DataSource on the classpath and auto-configures a `JpaTransactionManager`, `EntityManagerFactory`, etc.
>
> Internally, `@EnableAutoConfiguration` triggers `AutoConfigurationImportSelector`, which reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and loads matching configuration classes. Each class uses `@ConditionalOnClass`, `@ConditionalOnMissingBean`, etc. to only apply if conditions are met.
>
> You can see what was configured and why with: `--debug` flag → prints an **auto-configuration report**.

**Q3. What is the role of `application.properties` / `application.yml` in Spring Boot?**
> These files externalize configuration so you don't hardcode values in source code. Spring Boot loads them from `src/main/resources/` automatically. Common configurations include:
> - `server.port` — change the embedded server port.
> - `spring.datasource.*` — database connection settings.
> - `spring.jpa.hibernate.ddl-auto` — schema generation strategy.
> - Custom properties accessed via `@Value("${key}")` or `@ConfigurationProperties`.
>
> **Profile-specific files** like `application-dev.properties` are activated with `spring.profiles.active=dev`, allowing environment-specific configs.

**Q4. What are Spring Boot Starters? Give examples.**
> Starters are curated dependency descriptors that pull in everything you need for a feature in one Maven/Gradle dependency. You don't have to hunt for compatible library versions — the starter manages them.
>
> | Starter | What it provides |
> |---|---|
> | `spring-boot-starter-web` | Spring MVC, Tomcat, Jackson |
> | `spring-boot-starter-data-jpa` | Hibernate, Spring Data JPA |
> | `spring-boot-starter-security` | Spring Security |
> | `spring-boot-starter-test` | JUnit, Mockito, AssertJ |
> | `spring-boot-starter-actuator` | Health, metrics, monitoring endpoints |

**Q5. What is `@SpringBootApplication` and what annotations does it combine?**
> `@SpringBootApplication` is a convenience annotation that is equivalent to declaring three annotations together:
> - `@SpringBootConfiguration` — marks the class as a configuration class (specialization of `@Configuration`).
> - `@EnableAutoConfiguration` — enables Spring Boot's auto-configuration mechanism.
> - `@ComponentScan` — scans the current package and sub-packages for Spring-managed components.
>
> Placed on the main class, it triggers the full Spring Boot bootstrap process.

**Q6. How does Spring Boot handle externalized configuration and what is the property resolution order?**
> Spring Boot resolves configuration from multiple sources in a defined priority order (higher overrides lower):
> 1. Command-line arguments (`--server.port=9090`)
> 2. OS environment variables
> 3. `application-{profile}.properties`
> 4. `application.properties`
> 5. `@PropertySource` annotations
> 6. Default values in code
>
> This allows the same artifact (JAR) to run in dev, staging, and production with different configs — a 12-factor app principle.

**Q7. What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?**
> All four are specializations of `@Component` and register the class as a Spring bean. The difference is **semantic and functional**:
> - `@Component` — generic Spring-managed component.
> - `@Service` — marks business logic layer; signals intent to developers.
> - `@Repository` — marks data access layer; additionally enables Spring's **exception translation** (converts DB-specific exceptions to Spring's `DataAccessException`).
> - `@Controller` — marks web layer; works with Spring MVC to map HTTP requests.
> - `@RestController` — `@Controller` + `@ResponseBody`, returns JSON/XML directly.

**Q8. What is the Spring Bean lifecycle?**
> The lifecycle of a Spring bean managed by the IoC container:
> 1. **Instantiation** — container creates the bean instance.
> 2. **Dependency Injection** — fields/constructor/setter dependencies are injected.
> 3. **`@PostConstruct`** — initialization callback method runs.
> 4. **Bean is ready** — used by the application.
> 5. **`@PreDestroy`** — cleanup callback runs when the context is closing.
> 6. **Destruction** — bean is garbage collected.
>
> You can also implement `InitializingBean` / `DisposableBean` interfaces, but `@PostConstruct` / `@PreDestroy` are preferred as they are standard JSR-250 annotations.

---

# 2. Spring Boot Actuator

**Q1. What is Spring Boot Actuator and why is it useful in production?**
> Spring Boot Actuator provides **production-ready operational features** without writing any extra code. It exposes HTTP endpoints (and JMX) to inspect the internal state of a running application.
>
> Common uses:
> - **Health checks** — load balancers and orchestrators (Kubernetes) query `/actuator/health` to know if the app is alive.
> - **Metrics** — CPU, memory, request counts, JVM stats (integrates with Prometheus/Grafana).
> - **Environment info** — see all active properties and their sources.
> - **Loggers** — change log levels at runtime without restart.
>
> Add it: `spring-boot-starter-actuator` dependency.

**Q2. What are the most commonly used Actuator endpoints?**
> | Endpoint | Purpose |
> |---|---|
> | `/actuator/health` | Application health status (UP/DOWN) |
> | `/actuator/info` | App metadata (version, description) |
> | `/actuator/metrics` | JVM, HTTP, datasource metrics |
> | `/actuator/env` | All environment properties |
> | `/actuator/loggers` | View/change log levels at runtime |
> | `/actuator/beans` | All Spring beans in context |
> | `/actuator/mappings` | All `@RequestMapping` routes |
> | `/actuator/threaddump` | Thread state snapshot |
>
> By default, only `/health` and `/info` are exposed over HTTP. Expose others with:
> `management.endpoints.web.exposure.include=*`

**Q3. How do you secure Actuator endpoints so they are not publicly accessible?**
> Exposing all actuator endpoints publicly is a security risk (sensitive env values, bean details, etc.). Strategies:
> 1. **Change the management port** — run actuator on a separate port (`management.server.port=8081`) accessible only inside the internal network.
> 2. **Spring Security** — restrict actuator endpoints to `ADMIN` role only.
> 3. **Expose selectively** — only whitelist specific endpoints:
>    `management.endpoints.web.exposure.include=health,info`
> 4. **Disable sensitive endpoints** — `management.endpoint.env.enabled=false`

**Q4. What is the difference between liveness and readiness probes in Actuator (Spring Boot 2.3+)?**
> Spring Boot 2.3+ introduced two sub-indicators under `/actuator/health` for Kubernetes:
> - **Liveness** (`/actuator/health/liveness`) — Is the app alive? If DOWN, Kubernetes restarts the pod. Indicates internal app state (e.g., deadlock).
> - **Readiness** (`/actuator/health/readiness`) — Is the app ready to serve traffic? If DOWN, Kubernetes stops sending requests to the pod but does not restart it. Indicates readiness to accept requests (e.g., DB not yet connected).
>
> Enable with: `management.health.livenessstate.enabled=true` and `management.health.readinessstate.enabled=true`.

**Q5. How do you create a custom health indicator in Spring Boot Actuator?**
> Implement the `HealthIndicator` interface and register it as a Spring bean:
> ```java
> @Component
> public class ExternalServiceHealthIndicator implements HealthIndicator {
>     @Override
>     public Health health() {
>         boolean serviceUp = checkExternalService(); // your logic
>         if (serviceUp) {
>             return Health.up().withDetail("service", "reachable").build();
>         }
>         return Health.down().withDetail("service", "unreachable").build();
>     }
> }
> ```
> This automatically appears under `/actuator/health` as `externalService`.

**Q6. How does Actuator integrate with Prometheus and Grafana for monitoring?**
> 1. Add `micrometer-registry-prometheus` dependency.
> 2. Expose the `/actuator/prometheus` endpoint.
> 3. Configure Prometheus to scrape this endpoint periodically.
> 4. Connect Grafana to Prometheus as a data source and create dashboards.
>
> Micrometer is the metrics facade in Spring Boot — it abstracts metrics collection and supports multiple backends (Prometheus, Datadog, CloudWatch, etc.) with the same API.

---

# 3. Microservices Overview

**Q1. What are Microservices? How are they different from a Monolithic architecture?**
> **Monolithic architecture** packages the entire application — UI, business logic, data access — into a single deployable unit. It's simple to develop initially but becomes hard to scale and maintain as it grows.
>
> **Microservices architecture (MSA)** decomposes the application into small, independent services, each:
> - Owning a specific business capability (e.g., Order Service, Payment Service).
> - Running in its own process.
> - Communicating via APIs (REST, gRPC) or messaging (Kafka, RabbitMQ).
> - Deployed independently.
>
> | Dimension | Monolith | Microservices |
> |---|---|---|
> | Deployment | Single unit | Independent per service |
> | Scaling | Scale entire app | Scale only the needed service |
> | Tech stack | Single stack | Polyglot possible |
> | Fault isolation | One bug can crash all | Failures are isolated |
> | Complexity | Simple initially | Higher operational complexity |

**Q2. What problem does Microservices architecture solve that Monoliths cannot?**
> As monoliths grow, they suffer from:
> - **Deployment coupling** — changing one feature requires redeploying the entire app.
> - **Scaling inefficiency** — you must scale the entire app even if only one component is under load.
> - **Team coupling** — large teams conflict on the same codebase, slowing delivery.
> - **Technology lock-in** — forced to use one language/framework.
>
> MSA solves this by enabling **independent deployability**, **fine-grained scaling**, **team autonomy**, and **technology flexibility**.

**Q3. How do microservices communicate with each other?**
> Two primary patterns:
> - **Synchronous (request-response)**:
>   - **REST over HTTP** — simple, human-readable, widely supported.
>   - **gRPC** — binary protocol using Protocol Buffers; high performance, strongly typed.
>   - **GraphQL** — flexible query language for APIs.
> - **Asynchronous (event-driven)**:
>   - **Message queues** (RabbitMQ, ActiveMQ) — point-to-point messaging.
>   - **Event streaming** (Apache Kafka) — durable, replayable event log.
>
> Asynchronous communication decouples services — the sender doesn't wait for the receiver, improving resilience.

**Q4. What is the "Single Responsibility Principle" in the context of microservices?**
> In MSA, each microservice should do **one thing and do it well** — aligned to a single business capability (e.g., inventory management, authentication). This maps to the Single Responsibility Principle (SRP) from SOLID.
>
> A useful heuristic: a team should be able to understand, develop, and deploy a service independently. If a service does too many things, it becomes a "distributed monolith" — you get the complexity of both worlds with the benefits of neither.

**Q5. What is the difference between orchestration and choreography in microservices?**
> Both describe how services coordinate in a workflow:
> - **Orchestration** — a central **orchestrator** service tells each service what to do and when (like a conductor). Example: an Order Orchestrator calls Payment → Inventory → Notification in sequence.
>   - Pro: easy to visualize the flow. Con: the orchestrator becomes a bottleneck/single point of failure.
> - **Choreography** — each service **reacts to events** published by other services, with no central controller. Example: Order service emits `OrderPlaced` event → Payment service listens and processes payment → emits `PaymentCompleted` → Inventory service listens and reserves stock.
>   - Pro: loose coupling. Con: the overall flow is hard to trace/debug.

**Q6. What is an API Gateway and why is it needed in Microservices?**
> An **API Gateway** is the single entry point for all client requests. Instead of clients knowing the address of every microservice, they call the gateway which routes requests to the appropriate service.
>
> Responsibilities of an API Gateway:
> - **Routing** — forward requests to the correct service.
> - **Authentication/Authorization** — validate JWT tokens before routing.
> - **Rate limiting** — throttle abusive clients.
> - **Load balancing** — distribute traffic.
> - **SSL termination** — handle HTTPS at the gateway level.
> - **Request aggregation** — combine results from multiple services into one response.
>
> Examples: Spring Cloud Gateway, Kong, AWS API Gateway, Nginx.

---

# 4. MSA Characteristics, Advantages & Disadvantages

**Q1. What are the key characteristics of Microservices Architecture?**
> 1. **Single Business Capability** — each service is organized around a business domain.
> 2. **Independent Deployability** — services are packaged and deployed independently.
> 3. **Decentralized Data Management** — each service owns its own database (no shared DB).
> 4. **Loose Coupling, High Cohesion** — services interact via well-defined APIs; internal changes don't ripple outward.
> 5. **Failure Isolation** — a failing service doesn't bring down the entire system (with proper circuit breakers).
> 6. **Polyglot Technology** — different services can use different languages, frameworks, and databases.
> 7. **Designed for Scale** — individual services scale horizontally based on demand.
> 8. **Automated Deployment (DevOps)** — CI/CD pipelines are essential; manual deployment doesn't scale with many services.

**Q2. What are the main advantages of Microservices?**
> - **Independent deployments** — teams deploy their service without coordinating with others → faster release cycles.
> - **Scalability** — scale only the services under load (e.g., scale the Payment service during sales, not the entire app).
> - **Fault tolerance** — failure in one service is isolated; the rest continue functioning.
> - **Technology flexibility** — use Java for one service, Python for another, Node.js for a third.
> - **Team autonomy** — small, focused teams own and operate their service end-to-end.
> - **Easier to understand** — each service is small and focused on one domain.

**Q3. What are the disadvantages and challenges of Microservices?**
> - **Distributed system complexity** — network failures, latency, partial failures need to be handled explicitly.
> - **Data consistency** — no shared database; distributed transactions are hard (need Saga pattern, eventual consistency).
> - **Operational overhead** — you need container orchestration (Kubernetes), service discovery, centralized logging, distributed tracing.
> - **Testing complexity** — integration and contract testing across services is harder than testing a monolith.
> - **Latency** — inter-service calls over the network are slower than in-process calls.
> - **Service proliferation** — too many fine-grained services creates an unwieldy "nanoservice" anti-pattern.

**Q4. What is "Decentralized Data Management" and why does it matter?**
> In MSA, each service owns its own database — no service accesses another service's database directly. Services only share data through APIs or events.
>
> **Why it matters:**
> - It enforces **loose coupling** at the data layer.
> - Each service can choose the best database for its needs (relational, document, graph, time-series).
> - No single database becomes a bottleneck.
>
> **The tradeoff:** data consistency across services is harder. You give up ACID transactions that span multiple services and instead embrace **eventual consistency** through events and the Saga pattern.

**Q5. What is eventual consistency in the context of Microservices?**
> In a distributed system where each service has its own database, you cannot use a single ACID transaction across services. Instead, services use **eventual consistency** — the system guarantees that, given enough time (and no new updates), all replicas/services will converge to the same state.
>
> Example: When an order is placed, the Order service saves the order (its DB), then publishes an event. The Inventory service processes that event and updates its stock. There's a brief window where the order exists but inventory hasn't updated yet — that's eventual consistency.

---

# 5. Microservices with Messaging

**Q1. What is a Messaging System and why is it used in Microservices?**
> A **messaging system** is infrastructure that allows services to communicate by sending and receiving **messages** through a **message broker**, rather than calling each other directly (synchronous REST).
>
> Why use messaging in MSA?
> - **Decoupling** — the producer doesn't need to know who consumes the message.
> - **Resilience** — if a consumer is down, messages are stored in the broker until it recovers.
> - **Asynchronous processing** — the producer continues without waiting for the consumer to finish.
> - **Load leveling** — the broker acts as a buffer, smoothing traffic spikes.
>
> Examples: **Apache Kafka**, **RabbitMQ**, **ActiveMQ**, **AWS SQS/SNS**.

**Q2. What is the difference between a Message Queue and an Event/Message Stream?**
> | Dimension | Message Queue (e.g., RabbitMQ) | Event Stream (e.g., Kafka) |
> |---|---|---|
> | Delivery | Point-to-point; one consumer gets the message | Pub/Sub; multiple consumers can read the same event |
> | Retention | Message deleted after consumed | Events retained for a configurable period; replayable |
> | Model | Push (broker pushes to consumer) | Pull (consumer polls at its own pace) |
> | Use case | Task distribution, work queues | Event sourcing, audit logs, analytics pipelines |
> | Order | Per-queue ordering | Per-partition ordering |

**Q3. What are the common messaging patterns in Microservices?**
> - **Point-to-Point (P2P)** — one producer sends a message to one queue; one consumer processes it. Used for task queues (e.g., send one email per message).
> - **Publish-Subscribe (Pub/Sub)** — one producer publishes an event to a topic; multiple consumers subscribe and each receives a copy. Used for broadcasting events (e.g., `OrderPlaced` consumed by both Inventory and Notification services).
> - **Request-Reply** — a service sends a message and expects a reply message on a reply queue. Simulates synchronous RPC over async messaging.
> - **Dead Letter Queue (DLQ)** — messages that fail to process are routed to a DLQ for inspection/retry.

**Q4. How does asynchronous messaging improve resilience in a Microservices system?**
> Without messaging, if Service B is down and Service A calls it directly, Service A fails too (tight coupling, cascading failure).
>
> With a message broker:
> 1. Service A publishes a message to the broker and moves on.
> 2. The broker stores the message durably.
> 3. When Service B recovers, it picks up and processes the message.
>
> The system is **resilient to transient failures**. This pattern, combined with idempotent consumers (safe to re-process the same message), makes the overall system self-healing.

**Q5. What is an idempotent consumer and why is it important in messaging?**
> In messaging systems, a message may be delivered **more than once** (at-least-once delivery guarantee). An **idempotent consumer** processes a message in a way that applying it multiple times has the same effect as applying it once.
>
> Example: instead of `UPDATE balance = balance - 100` (not idempotent), use `UPDATE balance = 900 WHERE message_id = 'xyz' AND NOT EXISTS (processed_message WHERE id='xyz')`.
>
> Technique: track processed message IDs in a DB table and skip duplicates.

---

# 6. Apache Kafka

**Q1. What is Apache Kafka and what problem does it solve?**
> Apache Kafka is a **distributed event streaming platform** designed for high-throughput, fault-tolerant, real-time data pipelines. Originally built at LinkedIn, it is now an Apache project widely used for:
> - **Microservice communication** via events.
> - **Real-time analytics** pipelines.
> - **Activity tracking** (user clicks, logs).
> - **Event sourcing** — storing the full history of state changes as events.
>
> Key property: Kafka stores events **durably on disk** and allows consumers to read and replay at their own pace, making it fundamentally different from traditional message queues.

**Q2. Explain Kafka's core architecture: Broker, Topic, Partition.**
> - **Broker** — a Kafka server. A Kafka **cluster** consists of multiple brokers for fault tolerance and scalability.
> - **Topic** — a named, logical category for events (like a database table). Producers write to topics; consumers read from them.
> - **Partition** — a topic is split into ordered, immutable logs called partitions. Each partition is an append-only log of records (events) identified by an **offset**.
>   - Partitions enable **parallelism** — different consumers can read different partitions simultaneously.
>   - Each partition has one **leader** broker and zero or more **replica** brokers for fault tolerance.
>   - **Replication factor** — how many brokers store a copy of each partition.

**Q3. What is the role of a Kafka Producer and how does it work?**
> A **Producer** publishes (writes) events to a Kafka topic. Key behaviors:
> - **Partitioning** — the producer decides which partition to write to:
>   - By **key** — same key always goes to the same partition (ensures ordering for related events).
>   - Round-robin — if no key, events are distributed across partitions.
> - **Acknowledgment (acks)** — controls durability:
>   - `acks=0` — fire and forget (fastest, may lose data).
>   - `acks=1` — leader acknowledges (moderate).
>   - `acks=all` — all replicas acknowledge (slowest, highest durability).
> - **Batching** — producers batch records for efficiency before sending.

**Q4. What is a Kafka Consumer and what is a Consumer Group?**
> A **Consumer** reads events from Kafka topics. Key concepts:
> - **Offset** — each consumer tracks its position in a partition using an offset. Consumers can replay events by resetting offsets.
> - **Consumer Group** — a group of consumers that collectively consume a topic. Kafka assigns each partition to exactly one consumer in the group at a time.
>   - This enables **parallel consumption** — more partitions → more consumers working in parallel.
>   - If a consumer in a group fails, Kafka **rebalances** partitions among remaining consumers.
>   - Two different consumer groups can read the same topic independently (Pub/Sub behavior).

**Q5. What is a Kafka ZooKeeper / KRaft and what does it do?**
> Historically, Kafka relied on **Apache ZooKeeper** to manage cluster metadata: which brokers are alive, leader election for partitions, consumer group coordination.
>
> Since Kafka 2.8 (GA in 3.x), Kafka introduced **KRaft (Kafka Raft)** — a self-managed metadata system that eliminates the ZooKeeper dependency. Benefits of KRaft:
> - Fewer components to operate and monitor.
> - Faster leader election and controller failover.
> - Scales to more partitions without ZooKeeper bottleneck.

**Q6. What is the difference between Kafka and RabbitMQ?**
> | Dimension | Apache Kafka | RabbitMQ |
> |---|---|---|
> | Model | Distributed event log (pull-based) | Message queue/broker (push-based) |
> | Message retention | Retained for configurable duration | Deleted after consumption |
> | Replay | Yes — consumers can rewind and replay | No |
> | Throughput | Very high (millions/sec) | Moderate |
> | Ordering | Per-partition | Per-queue |
> | Use case | Event streaming, audit, analytics | Task queues, RPC, notifications |

**Q7. What guarantees does Kafka provide for message delivery?**
> - **At-most-once** — messages may be lost; never redelivered (producer `acks=0`).
> - **At-least-once** — messages are never lost but may be delivered more than once. This is the default and most common. Requires idempotent consumers.
> - **Exactly-once** — each message is delivered and processed exactly once. Kafka supports this via **idempotent producers** + **transactional API** + **transactional consumers**. Most complex but highest guarantee.

---

# 7. Microservice Design Patterns

**Q1. What is the Saga Pattern and why is it needed in Microservices?**
> In a monolith, a business operation spanning multiple tables uses a single ACID transaction — if anything fails, it all rolls back. In MSA with separate databases per service, a single database transaction is impossible.
>
> The **Saga Pattern** manages distributed transactions as a sequence of **local transactions**, each publishing an event or message to trigger the next step. If a step fails, **compensating transactions** are executed to undo previous steps.
>
> **Two implementations:**
> - **Choreography-based Saga** — each service listens for events and decides when to act. No central coordinator. Good for simple flows.
> - **Orchestration-based Saga** — a Saga orchestrator (a dedicated service or state machine) tells each service what to do and handles failures. Better for complex flows.

**Q2. Walk through a Saga example: Place Order flow.**
> Saga for "Place Order":
> 1. **Order Service** creates order (status: PENDING) → publishes `OrderCreated` event.
> 2. **Payment Service** reserves funds → publishes `PaymentApproved` event.
> 3. **Inventory Service** reserves stock → publishes `StockReserved` event.
> 4. **Order Service** marks order CONFIRMED.
>
> **Failure scenario (Payment fails):**
> 1. **Payment Service** publishes `PaymentFailed`.
> 2. **Order Service** listens and marks order CANCELLED (compensating transaction).
>
> No global lock is held — each step is a local transaction. Eventual consistency is achieved.

**Q3. What is CQRS (Command Query Responsibility Segregation)?**
> **CQRS** separates the model used to **write data** (Command) from the model used to **read data** (Query).
>
> - **Command side** — handles state-changing operations (create, update, delete). Optimized for writes, enforces business rules, emits events.
> - **Query side** — handles read-only operations. Uses a separate, denormalized **read model** optimized for querying (e.g., a materialized view or Elasticsearch index).
>
> **Why CQRS?**
> - Read and write workloads often have very different performance and scalability needs.
> - Complex domains where a single model is hard to optimize for both reads and writes.
>
> CQRS is often combined with **Event Sourcing** — the command side emits events that update the read model.

**Q4. What is Event Sourcing?**
> Instead of storing only the **current state** of an entity in a database, **Event Sourcing** stores the full sequence of **events** that led to the current state.
>
> Example: instead of `account.balance = 500`, you store:
> - `AccountOpened { initial: 0 }`
> - `MoneyDeposited { amount: 700 }`
> - `MoneyWithdrawn { amount: 200 }`
>
> The current state is derived by **replaying** all events. Benefits:
> - Full audit trail out of the box.
> - Ability to replay events to rebuild read models or debug issues.
> - Natural fit with CQRS and messaging.

**Q5. What is the Strangler Fig Pattern in Microservices migration?**
> The **Strangler Fig** pattern is used to incrementally migrate a monolith to microservices without a big-bang rewrite.
>
> Process:
> 1. Route all traffic through a **facade** (e.g., API Gateway) in front of the monolith.
> 2. Gradually extract individual features/modules into new microservices.
> 3. The facade routes requests for extracted features to the new services and the rest to the monolith.
> 4. Over time, the monolith "shrinks" and is eventually retired.
>
> Named after the strangler fig tree that grows around and eventually replaces the host tree.

**Q6. What is the Circuit Breaker Pattern?**
> When a downstream service is slow or failing, calls pile up and threads are blocked — causing cascading failures up the chain. The **Circuit Breaker** pattern prevents this.
>
> States:
> - **Closed** — requests flow normally. Failures are counted.
> - **Open** — failure threshold exceeded; all requests fail immediately (fast fail) without calling the downstream service. Prevents overload.
> - **Half-Open** — after a cooldown period, a few test requests are allowed through. If they succeed, circuit closes; if they fail, it opens again.
>
> Spring Cloud uses **Resilience4j** to implement circuit breakers.

---

# 8. Spring Cloud

**Q1. What is Spring Cloud and what problems does it solve?**
> **Spring Cloud** is a suite of tools built on top of Spring Boot that provides solutions for the most common distributed systems challenges:
>
> | Problem | Spring Cloud Solution |
> |---|---|
> | How do services find each other? | Service discovery (Eureka) |
> | How does traffic enter the system? | API Gateway (Spring Cloud Gateway) |
> | How to call other services easily? | Declarative REST clients (OpenFeign) |
> | How to distribute load? | Client-side load balancing (Spring Cloud LoadBalancer) |
> | How to manage config centrally? | Config Server |
> | How to handle failures? | Circuit breaker (Resilience4j) |

**Q2. What is a Service Discovery in Microservices? How does Eureka work?**
> In a dynamic environment (containers, auto-scaling), service instances start and stop frequently, and their IP/port changes. **Service Discovery** is the mechanism by which services find each other without hardcoded addresses.
>
> **Eureka (Netflix)** works as follows:
> - **Eureka Server** — a registry of all service instances.
> - **Eureka Client** — each microservice registers itself with the server on startup (heartbeat every 30s).
> - When Service A wants to call Service B, it asks Eureka for the available instances of Service B, then picks one and calls it.
>
> This is **client-side discovery** — the client is responsible for load balancing.

**Q3. What is Spring Cloud Gateway and how does it differ from Zuul?**
> **Spring Cloud Gateway** is the modern, non-blocking API gateway built on Spring WebFlux (reactive). It replaces **Netflix Zuul** (blocking, servlet-based).
>
> Features:
> - **Route predicates** — match requests by path, host, method, headers, etc.
> - **Filters** — modify requests/responses (add headers, strip prefixes, rate limit, authenticate).
> - **Reactive** — built on Netty + Project Reactor; handles high concurrency with fewer threads.
>
> Example route:
> ```yaml
> spring:
>   cloud:
>     gateway:
>       routes:
>         - id: order-service
>           uri: lb://ORDER-SERVICE
>           predicates:
>             - Path=/orders/**
>           filters:
>             - StripPrefix=1
> ```

**Q4. What is OpenFeign and how does it simplify inter-service communication?**
> **OpenFeign** (Spring Cloud OpenFeign) is a declarative HTTP client. Instead of writing `RestTemplate` boilerplate code, you declare an interface with annotations — Spring creates the implementation.
>
> Example:
> ```java
> @FeignClient(name = "product-service")
> public interface ProductClient {
>     @GetMapping("/products/{id}")
>     ProductResponse getProduct(@PathVariable Long id);
> }
> ```
> Inject `ProductClient` as a bean and call `getProduct(id)` — OpenFeign handles HTTP, serialization, service discovery, and load balancing automatically.

**Q5. What is the Spring Cloud Config Server?**
> **Spring Cloud Config Server** provides centralized, externalized configuration management for distributed systems. All microservices fetch their configuration from the Config Server at startup instead of having individual `application.properties` files.
>
> - Config files are stored in a **Git repository** (or filesystem, Vault, etc.).
> - Services request `/{application}/{profile}/{label}` from the Config Server.
> - Supports **hot reload** via Spring Cloud Bus + `/actuator/refresh`.
>
> Benefits: single source of truth for config, environment-specific overrides, audit trail via Git history.

**Q6. What is a Circuit Breaker in Spring Cloud? How is Resilience4j configured?**
> See [Circuit Breaker Pattern in section 7](#q6-what-is-the-circuit-breaker-pattern). Spring Cloud integrates **Resilience4j** as the preferred circuit breaker library.
>
> Configuration in `application.yml`:
> ```yaml
> resilience4j:
>   circuitbreaker:
>     instances:
>       paymentService:
>         slidingWindowSize: 10
>         failureRateThreshold: 50
>         waitDurationInOpenState: 10000
> ```
>
> Usage with `@CircuitBreaker`:
> ```java
> @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
> public PaymentResponse callPayment(PaymentRequest req) { ... }
>
> public PaymentResponse paymentFallback(PaymentRequest req, Exception ex) {
>     return PaymentResponse.defaultResponse();
> }
> ```

**Q7. What is a Service Mesh and how is it different from Spring Cloud?**
> A **Service Mesh** (e.g., Istio, Linkerd) adds a **sidecar proxy** (e.g., Envoy) to each service instance. All network traffic between services goes through these proxies, which handle:
> - **mTLS** — automatic mutual TLS between services.
> - **Traffic management** — load balancing, retries, circuit breaking at the infrastructure level.
> - **Observability** — distributed tracing, metrics, access logs — without code changes.
>
> **Spring Cloud vs Service Mesh:**
> - Spring Cloud is **code-level** — features implemented in the application using libraries.
> - Service Mesh is **infrastructure-level** — features implemented in the network layer (sidecar), language-agnostic.
> - They can be **complementary** — use both where appropriate.

---

# 9. Docker

**Q1. What is Docker and what problem does it solve?**
> **Docker** is a platform for building, packaging, and running applications in **containers** — lightweight, portable, self-contained units that include the application and all its dependencies (runtime, libraries, config).
>
> The problem it solves: **"It works on my machine"**. Different environments (dev laptop, CI server, production) have different OS versions, library versions, and configs. Docker packages the app with everything it needs, so it runs identically everywhere.
>
> Key concepts:
> - **Image** — a read-only template built from a `Dockerfile`. The blueprint.
> - **Container** — a running instance of an image. Isolated process with its own filesystem, network, and process space.
> - **Registry** — a repository for images (Docker Hub, ECR, GCR).

**Q2. What is a Dockerfile? Explain the key instructions.**
> A `Dockerfile` is a text file with instructions to build a Docker image layer by layer.
>
> ```dockerfile
> # Base image
> FROM eclipse-temurin:17-jre-alpine
>
> # Set working directory
> WORKDIR /app
>
> # Copy the JAR
> COPY target/myapp.jar app.jar
>
> # Expose port (documentation only)
> EXPOSE 8080
>
> # Command to run
> ENTRYPOINT ["java", "-jar", "app.jar"]
> ```
>
> | Instruction | Purpose |
> |---|---|
> | `FROM` | Base image to start from |
> | `WORKDIR` | Set working directory |
> | `COPY` / `ADD` | Copy files into the image |
> | `RUN` | Execute a command during build (installs, compiles) |
> | `EXPOSE` | Document the port (doesn't publish it) |
> | `ENV` | Set environment variables |
> | `ENTRYPOINT` | Command to run when container starts |
> | `CMD` | Default arguments for ENTRYPOINT |

**Q3. What is the difference between a Docker Image and a Docker Container?**
> - **Image** — a read-only, immutable snapshot built from a Dockerfile. Think of it as a **class** in OOP.
> - **Container** — a running (or stopped) instance of an image. Think of it as an **object** instantiated from the class.
>
> You can run many containers from one image. Each container gets its own writable layer on top of the shared read-only image layers. When the container is deleted, its writable layer is lost (unless volumes are used for persistence).

**Q4. What is Docker Compose and when do you use it?**
> **Docker Compose** is a tool for defining and running **multi-container applications** using a single `docker-compose.yml` file. Ideal for local development environments.
>
> Example — run a Spring Boot app + MySQL together:
> ```yaml
> version: '3.8'
> services:
>   app:
>     build: .
>     ports:
>       - "8080:8080"
>     environment:
>       SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/mydb
>     depends_on:
>       - db
>   db:
>     image: mysql:8
>     environment:
>       MYSQL_ROOT_PASSWORD: Root123
>       MYSQL_DATABASE: mydb
>     volumes:
>       - db-data:/var/lib/mysql
> volumes:
>   db-data:
> ```
> Start everything with: `docker-compose up`

**Q5. What is the difference between `ENTRYPOINT` and `CMD` in a Dockerfile?**
> - `ENTRYPOINT` — defines the **fixed** command that always runs when the container starts. Cannot be easily overridden at runtime.
> - `CMD` — provides **default arguments** to `ENTRYPOINT`, or a default command if no `ENTRYPOINT` is set. Can be overridden by passing arguments to `docker run`.
>
> Example:
> ```dockerfile
> ENTRYPOINT ["java", "-jar", "app.jar"]
> CMD ["--spring.profiles.active=prod"]
> ```
> Override CMD: `docker run myimage --spring.profiles.active=dev`

**Q6. What are Docker Volumes and why are they needed?**
> Containers are **ephemeral** — when a container is removed, all data written inside it is lost. **Volumes** are Docker-managed persistent storage that lives outside the container's lifecycle.
>
> Types:
> - **Named volumes** (`docker volume create my-data`) — managed by Docker, stored in Docker's storage area.
> - **Bind mounts** — map a host directory to a container path. Good for development (live code reloading).
> - **tmpfs mounts** — in-memory only, not persisted.
>
> Use volumes for databases, file uploads, logs — anything that must persist across container restarts.

**Q7. What is the difference between Docker and a Virtual Machine (VM)?**
> | Dimension | Virtual Machine | Docker Container |
> |---|---|---|
> | Isolation | Full OS per VM (hypervisor) | Process-level isolation (shared host kernel) |
> | Size | GBs (includes guest OS) | MBs (shares host OS kernel) |
> | Startup time | Minutes | Seconds |
> | Overhead | High (each VM has its own OS) | Low |
> | Portability | Less portable | Highly portable |
> | Use case | Strong isolation needed | Microservices, CI/CD, cloud-native |

---

# 10. DevOps and CI/CD

**Q1. What is DevOps and what is its core philosophy?**
> **DevOps** is a culture and set of practices that bridge the gap between **Development** (build features) and **Operations** (deploy and run systems). Historically, Dev and Ops were siloed teams that blamed each other — Dev shipped code, Ops struggled to run it.
>
> Core philosophy:
> - **Collaboration** — shared responsibility for the full lifecycle.
> - **Automation** — automate testing, deployment, infrastructure provisioning.
> - **Continuous Improvement** — fast feedback loops, frequent small releases.
> - **Infrastructure as Code (IaC)** — manage infrastructure (servers, networks) using code (Terraform, Ansible).
>
> Goal: **shorten the feedback loop** from code commit to production delivery.

**Q2. What is CI/CD? Explain Continuous Integration, Continuous Delivery, and Continuous Deployment.**
> - **Continuous Integration (CI)** — developers merge code frequently (multiple times a day). Each merge triggers automated **build and tests** to catch integration bugs early. Tools: GitHub Actions, Jenkins, GitLab CI.
> - **Continuous Delivery (CD)** — extends CI by automatically deploying to a **staging environment** after passing all tests. Deploying to production is a **manual approval** step. The code is always in a deployable state.
> - **Continuous Deployment** — goes one step further: every successful pipeline run is automatically deployed to **production** without human approval. Requires high confidence in automated tests.
>
> Flow: `Code Commit → CI Build → Unit Tests → Integration Tests → Deploy to Staging → [Approval] → Deploy to Production`

**Q3. What is a CI/CD pipeline? What stages does it typically include?**
> A CI/CD pipeline is an automated sequence of steps triggered by a code change.
>
> Typical stages:
> 1. **Source** — trigger on git push/pull request.
> 2. **Build** — compile code, resolve dependencies (`mvn package`).
> 3. **Unit Tests** — fast, isolated tests (`mvn test`).
> 4. **Code Quality / Static Analysis** — SonarQube, Checkstyle.
> 5. **Integration Tests** — test with real dependencies (DB, message broker).
> 6. **Build Docker Image** — `docker build`, push to registry.
> 7. **Deploy to Staging** — apply Kubernetes manifests or Docker Compose.
> 8. **Smoke / E2E Tests** — verify the deployed environment.
> 9. **Deploy to Production** — auto or manual gate.
> 10. **Monitoring / Alerts** — observe after deployment.

**Q4. What is GitHub Actions and how does it work?**
> **GitHub Actions** is GitHub's built-in CI/CD platform. Pipelines are defined as **workflows** in YAML files inside `.github/workflows/`.
>
> Key concepts:
> - **Workflow** — the overall automation process defined in a YAML file.
> - **Trigger (on)** — what starts the workflow (push, pull_request, schedule, manual).
> - **Job** — a set of steps that run on the same runner (machine).
> - **Step** — a single task (run a command, use an action).
> - **Action** — a reusable unit (e.g., `actions/checkout@v4`, `actions/setup-java@v4`).
>
> Example:
> ```yaml
> on: [push]
> jobs:
>   build:
>     runs-on: ubuntu-latest
>     steps:
>       - uses: actions/checkout@v4
>       - uses: actions/setup-java@v4
>         with: { java-version: '17' }
>       - run: mvn package
> ```

**Q5. What is Infrastructure as Code (IaC) and what tools implement it?**
> **Infrastructure as Code** means defining and managing infrastructure (servers, networks, databases, load balancers) using **code and configuration files**, rather than manual GUI clicks.
>
> Benefits:
> - **Version controlled** — infra changes are tracked in Git.
> - **Reproducible** — spin up identical environments for dev, staging, prod.
> - **Automated** — provision infra as part of the CI/CD pipeline.
>
> Tools:
> - **Terraform** — declarative, cloud-agnostic (AWS, GCP, Azure).
> - **Ansible** — procedural, configuration management and provisioning.
> - **AWS CloudFormation** — AWS-specific IaC.
> - **Helm** — package manager for Kubernetes manifests.

**Q6. What is Kubernetes and how does it relate to Docker and DevOps?**
> **Kubernetes (K8s)** is a container orchestration platform that automates the deployment, scaling, and management of containerized applications (Docker containers) across a cluster of machines.
>
> Key Kubernetes objects:
> - **Pod** — smallest deployable unit; one or more containers.
> - **Deployment** — manages pod replicas, rolling updates, rollbacks.
> - **Service** — stable network endpoint to reach pods (load balances across replicas).
> - **Ingress** — HTTP routing rules (like an API Gateway at infra level).
> - **ConfigMap / Secret** — externalize configuration and credentials.
>
> In DevOps: Docker packages the app → Kubernetes runs and manages it at scale → CI/CD pipeline automates the entire build-test-deploy cycle.

**Q7. What is the difference between Blue-Green Deployment and Canary Deployment?**
> Both are deployment strategies that minimize downtime and risk:
>
> **Blue-Green Deployment:**
> - Two identical production environments: **Blue** (current live) and **Green** (new version).
> - Deploy new version to Green; test it. Then switch the router/load balancer to Green. Blue becomes standby.
> - **Rollback** = switch router back to Blue instantly.
> - Con: requires double the infrastructure.
>
> **Canary Deployment:**
> - Roll out the new version to a **small percentage** of users (e.g., 5%) while the rest get the old version.
> - Gradually increase traffic to the new version while monitoring metrics.
> - If issues appear, roll back only the canary traffic.
> - Pro: real-world testing with low risk; no double infrastructure cost.

**Q8. What is the role of monitoring and observability in a DevOps pipeline?**
> **Monitoring** answers: "Is the system healthy?" (metrics, alerts).
> **Observability** answers: "Why is the system behaving this way?" (logs, metrics, traces together).
>
> The **three pillars of observability**:
> - **Logs** — timestamped text records of events (ELK Stack: Elasticsearch, Logstash, Kibana).
> - **Metrics** — numerical measurements over time (Prometheus + Grafana).
> - **Distributed Traces** — track a request as it flows through multiple services (Jaeger, Zipkin, AWS X-Ray).
>
> In a DevOps pipeline, observability closes the feedback loop: deploy → observe → alert → investigate → fix → redeploy.
