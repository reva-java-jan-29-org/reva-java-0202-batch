# 12 — Microservices with Messaging: Introduction to Kafka

## Navigation
← [11 Integration Testing](11-integration-testing.md)

---

## Table of Contents
1. [Why Microservices Need Messaging](#1-why-microservices-need-messaging)
2. [Messaging Patterns — Synchronous vs Asynchronous](#2-messaging-patterns)
3. [Introduction to Apache Kafka](#3-introduction-to-apache-kafka)
4. [Kafka Architecture Deep Dive](#4-kafka-architecture-deep-dive)
   - 4a. [Broker](#4a-broker)
   - 4b. [Topic](#4b-topic)
   - 4c. [Partition](#4c-partition)
   - 4d. [Producer](#4d-producer)
   - 4e. [Consumer & Consumer Group](#4e-consumer--consumer-group)
   - 4f. [Offset](#4f-offset)
   - 4g. [ZooKeeper / KRaft](#4g-zookeeper--kraft)
5. [Kafka Message Flow — End to End](#5-kafka-message-flow--end-to-end)
6. [Running Kafka Locally (Docker)](#6-running-kafka-locally-docker)
7. [Kafka Producer with Java (Plain API)](#7-kafka-producer-with-java-plain-api)
8. [Kafka Consumer with Java (Plain API)](#8-kafka-consumer-with-java-plain-api)
9. [Spring Boot Kafka Integration](#9-spring-boot-kafka-integration)
   - 9a. [Dependencies & Configuration](#9a-dependencies--configuration)
   - 9b. [Producing Messages — KafkaTemplate](#9b-producing-messages--kafkatemplate)
   - 9c. [Consuming Messages — @KafkaListener](#9c-consuming-messages--kafkalistener)
   - 9d. [Sending and Receiving Java Objects (JSON)](#9d-sending-and-receiving-java-objects-json)
   - 9e. [Error Handling & Dead Letter Topics](#9e-error-handling--dead-letter-topics)
10. [Practical Example — Order Service → Inventory Service](#10-practical-example--order-service--inventory-service)
11. [Summary Cheat Sheet](#11-summary-cheat-sheet)
12. [Interview Questions](#12-interview-questions)

---

## 1. Why Microservices Need Messaging

In a monolithic app all modules call each other directly — a method call is instant and synchronous. In microservices, each service is a **separate process** running on a separate machine. Direct calls between services create **tight coupling**:

```
Monolith (tight coupling):
  OrderModule.create()
      → calls InventoryModule.deductStock()    ← in-memory, instant
      → calls NotificationModule.sendEmail()   ← in-memory, instant

Microservices with direct HTTP (still tight coupling):
  Order Service POST /orders
      → HTTP call to Inventory Service         ← network, can fail, can be slow
      → HTTP call to Notification Service      ← if email service is down, order fails!
```

**Problems with direct HTTP between microservices:**
- **Temporal coupling** — if Inventory Service is down, Order Service cannot create orders.
- **Cascading failures** — one slow service blocks all callers.
- **Scaling complexity** — Order Service needs to know the URLs of all downstream services.

**Solution: Asynchronous messaging via a message broker (Kafka)**

```
Order Service
    → publishes "order.created" event to Kafka
        ↓
       Kafka (durable, persistent)
        ↓                      ↓
Inventory Service          Notification Service
(consumes when ready)      (consumes when ready)
```

Now services are **decoupled**:
- Order Service doesn't care if Inventory Service is slow or down.
- Each service processes messages at its own pace.
- New services can subscribe to events without changing Order Service.

---

## 2. Messaging Patterns

### Synchronous (HTTP / gRPC)
```
Caller → waits → Receiver responds → Caller continues
```
- Use when: you need an immediate response (e.g., GET /products, payment verification).
- Drawback: caller is blocked; if receiver is down, caller fails.

### Asynchronous (Message Queue / Event Stream)
```
Publisher → drops message in broker → continues immediately
                                  Subscriber picks up when ready
```
- Use when: caller doesn't need an immediate response (e.g., send email, update inventory, analytics).
- Benefit: decoupled, resilient, scalable.

### Two main asynchronous patterns:

```
Point-to-Point (Queue)                 Publish-Subscribe (Topic)
──────────────────────────             ──────────────────────────────────
One producer, ONE consumer.            One producer, MANY consumers.
Message is deleted after read.         Message is retained (configurable).
Used in: RabbitMQ queues.              Used in: Kafka topics.
Example: task queue, job scheduling.   Example: order events, audit logs.
```

**Kafka follows the Publish-Subscribe model** — one event can be consumed by multiple independent consumer groups simultaneously.

---

## 3. Introduction to Apache Kafka

**Apache Kafka** is a distributed, fault-tolerant, high-throughput **event streaming platform** created at LinkedIn and open-sourced in 2011.

Key characteristics:
- **Durable** — messages are written to disk (not in-memory), surviving restarts and failures.
- **Ordered** — messages within a partition are strictly ordered.
- **Replayable** — consumers can re-read old messages by resetting their offset.
- **Scalable** — topics are partitioned across multiple brokers; millions of messages/second.
- **Decoupled** — producers and consumers have no direct dependency on each other.

**Kafka vs traditional message queues (RabbitMQ, ActiveMQ):**

```
Traditional Queue (RabbitMQ)        Kafka
────────────────────────────        ──────────────────────────────
Message deleted after consumed.     Message retained (days/weeks/forever).
Push-based (broker pushes to consumer). Pull-based (consumer pulls).
Complex routing (exchanges/bindings). Simple topic-based routing.
Limited replay ability.             Full replay — rewind consumer to any offset.
Great for task queues.              Great for event streams, audit logs, analytics.
```

---

## 4. Kafka Architecture Deep Dive

### The Big Picture

```
                        ┌─────────────────────────────────────────────┐
                        │              Kafka Cluster                  │
                        │                                             │
  Producer A ──────────►│  Broker 1  ──  Broker 2  ──  Broker 3      │
  Producer B ──────────►│                                             │
                        │  Topic: "orders"                            │
                        │  ┌──────────────┬──────────────┐            │
                        │  │ Partition 0  │ Partition 1  │            │
                        │  │ [msg1][msg3] │ [msg2][msg4] │            │
                        │  └──────────────┴──────────────┘            │
                        └──────────────────────────────────────────────┘
                                        │
                        ┌───────────────┴───────────────┐
                        ▼                               ▼
               Consumer Group A                Consumer Group B
               (Inventory Service)             (Notification Service)
               Consumer 1 ← Partition 0       Consumer 1 ← Partition 0
               Consumer 2 ← Partition 1       Consumer 2 ← Partition 1
```

---

### 4a. Broker

A **Kafka broker** is a single Kafka server process. It:
- Stores messages on disk in partition log files.
- Serves read/write requests from producers and consumers.
- Replicates partition data to other brokers (for fault tolerance).

A **Kafka cluster** is a group of brokers working together. In production you run at least 3 brokers. In development, 1 broker is enough.

```
Kafka Cluster (3 brokers)
┌────────┐  ┌────────┐  ┌────────┐
│Broker 1│  │Broker 2│  │Broker 3│
│ Leader │  │Replica │  │Replica │  ← for partition 0
└────────┘  └────────┘  └────────┘
```

One broker is the **leader** for a partition; others hold replicas. Producers and consumers always talk to the leader. If the leader fails, a replica is elected as the new leader.

---

### 4b. Topic

A **topic** is a named feed/category to which messages are published. Think of it like a database table — a logical grouping of related events.

```
Topics in a Product Catalog system:
  "product.created"    — fired when a new product is added
  "order.placed"       — fired when a customer places an order
  "inventory.updated"  — fired when stock changes
  "user.registered"    — fired when a user signs up
```

Topics are:
- **Named** — producers publish to a topic by name; consumers subscribe by name.
- **Multi-subscriber** — many consumer groups can read the same topic independently.
- **Configurable retention** — messages can be kept for a time period (e.g., 7 days) or indefinitely.

---

### 4c. Partition

A topic is split into one or more **partitions**. Each partition is an ordered, immutable log of messages.

```
Topic "order.placed" with 3 partitions:

Partition 0: [order#1][order#4][order#7][order#10]...  ← append-only
Partition 1: [order#2][order#5][order#8][order#11]...
Partition 2: [order#3][order#6][order#9][order#12]...
```

**Why partitions?**
- **Parallelism** — different consumers in a group can read different partitions in parallel (horizontal scaling).
- **Distribution** — partitions are spread across brokers (no single broker becomes a bottleneck).

**How is a message assigned to a partition?**
- If a **message key** is provided: `partition = hash(key) % numPartitions` — all messages with the same key always go to the same partition, guaranteeing order for that key.
- If **no key**: round-robin across partitions.

```
Key = "user-123" → always Partition 1 (ordered history for that user)
Key = null       → Round-robin (no ordering guarantee)
```

---

### 4d. Producer

A **producer** is an application that **publishes** messages to Kafka topics.

Key producer concepts:
- **Record** — a message. Consists of: key (optional), value (required), topic, partition (optional), headers (optional), timestamp.
- **Acknowledgement (acks)** — controls durability vs speed:
  - `acks=0` — fire and forget (fastest, no guarantee).
  - `acks=1` — leader acknowledges write (fast, message safe if leader doesn't crash immediately).
  - `acks=all` — all replicas acknowledge (slowest, strongest guarantee).
- **Batching** — producer buffers records and sends them in batches for efficiency.
- **Retries** — producer retries failed sends automatically.

---

### 4e. Consumer & Consumer Group

A **consumer** is an application that **reads** messages from Kafka topics.

A **consumer group** is a group of consumers working together to read a topic. Kafka guarantees that each partition is read by **exactly one consumer** within the group.

```
Topic "order.placed" — 3 partitions

Consumer Group "inventory-service":
  Consumer A ← Partition 0
  Consumer B ← Partition 1
  Consumer C ← Partition 2
  (Each consumer processes 1/3 of the load)

Consumer Group "notification-service":
  Consumer X ← Partition 0, 1, 2
  (Only one consumer — reads all partitions)
```

**Scaling rule:** adding more consumers than partitions gives no benefit — extra consumers sit idle.

---

### 4f. Offset

The **offset** is an integer that identifies a message's position within a partition. Offsets start at 0 and increment by 1 for each new message.

```
Partition 0:
  Offset 0: { "orderId": 1, "product": "Laptop" }
  Offset 1: { "orderId": 4, "product": "Phone" }
  Offset 2: { "orderId": 7, "product": "Mouse" }
  Current offset for consumer: 2 (next to read: offset 3)
```

Kafka stores each consumer group's **committed offset** for each partition. This is how Kafka knows where each consumer left off. If a consumer crashes and restarts, it resumes from the last committed offset.

**Offset commit strategies:**
- `enable.auto.commit=true` — Kafka commits offsets automatically at intervals (simpler, risk of duplicate processing on failure).
- Manual commit — consumer explicitly commits after successfully processing (more control, exactly-once semantics possible).

---

### 4g. ZooKeeper / KRaft

Older versions of Kafka used **Apache ZooKeeper** to store metadata (broker info, topic configs, partition leaders). Kafka 3.x introduced **KRaft mode** (Kafka Raft) which removes the ZooKeeper dependency — Kafka manages its own metadata using a built-in consensus protocol.

For local development, both modes work. KRaft is the future standard.

---

## 5. Kafka Message Flow — End to End

```
Step 1: Producer creates a record
  ProducerRecord {
    topic:     "order.placed"
    key:       "user-123"           (optional)
    value:     { orderId: 101, ... }
    timestamp: 2026-03-16T10:00:00
  }

Step 2: Producer serializes key and value
  key   → bytes (StringSerializer)
  value → bytes (JsonSerializer)

Step 3: Partitioner decides which partition
  hash("user-123") % 3 → Partition 1

Step 4: Producer sends to Broker (leader of Partition 1)
  Batch of records → TCP → Broker

Step 5: Broker writes to Partition 1 log at next offset
  Partition 1, Offset 5: [bytes]

Step 6: Broker replicates to follower brokers (if acks=all)

Step 7: Broker sends acknowledgement to producer

Step 8: Consumer polls Broker
  consumer.poll(Duration.ofMillis(1000))
  → fetches records from Partition 1, starting at its committed offset

Step 9: Consumer deserializes
  bytes → String key
  bytes → OrderPlacedEvent object

Step 10: Consumer processes the record
  inventoryService.deductStock(event.getProductId(), event.getQuantity())

Step 11: Consumer commits offset
  Broker stores: group="inventory-service", partition=1, offset=6
```

---

## 6. Running Kafka Locally (Docker)

The easiest way to run Kafka for development is with Docker Compose.

**`docker-compose-kafka.yml`** (save in project root):

```yaml
version: '3.8'

services:

  # ── Kafka (KRaft mode — no ZooKeeper needed) ───────────────────────────────
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: kafka
    ports:
      - "9092:9092"       # application connects here
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"

  # ── Kafka UI (optional but very helpful) ───────────────────────────────────
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8090:8080"       # open http://localhost:8090 in browser
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    depends_on:
      - kafka
```

```bash
# Start Kafka + UI
docker-compose -f docker-compose-kafka.yml up -d

# Verify Kafka is running
docker-compose -f docker-compose-kafka.yml ps

# Stop
docker-compose -f docker-compose-kafka.yml down

# Open Kafka UI in browser
open http://localhost:8090
```

**Useful Kafka CLI commands (inside the container):**

```bash
# Get into the container
docker exec -it kafka bash

# List all topics
kafka-topics --bootstrap-server localhost:9092 --list

# Create a topic manually
kafka-topics --bootstrap-server localhost:9092 --create \
  --topic order.placed --partitions 3 --replication-factor 1

# Describe a topic (partitions, replicas, leaders)
kafka-topics --bootstrap-server localhost:9092 --describe --topic order.placed

# Produce messages from CLI (type messages, press Enter)
kafka-console-producer --bootstrap-server localhost:9092 --topic order.placed

# Consume messages from CLI
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order.placed --from-beginning
```

---

## 7. Kafka Producer with Java (Plain API)

This shows the raw Java Kafka API before introducing Spring abstractions. Understanding this first makes Spring's `@KafkaListener` much easier to understand.

```java
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class PlainKafkaProducerExample {

    public static void main(String[] args) throws Exception {

        // ── Step 1: Configure the producer ────────────────────────────────────
        Properties props = new Properties();

        // Address of the Kafka broker(s) — "bootstrap" because Kafka will
        // discover all other brokers from this initial connection
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // How to serialize the message key (String → bytes)
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // How to serialize the message value (String → bytes)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Acknowledgement level: "all" = leader + all replicas must confirm
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry up to 3 times on transient failures
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        // ── Step 2: Create the producer ───────────────────────────────────────
        // KafkaProducer is thread-safe — create once and share
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

            for (int i = 1; i <= 10; i++) {

                // ── Step 3: Create a ProducerRecord ───────────────────────────
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    "order.placed",              // topic
                    "user-" + (i % 3),           // key  → determines partition
                    "{\"orderId\":" + i + "}"    // value (JSON string)
                );

                // ── Step 4: Send asynchronously with a callback ───────────────
                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        System.out.printf("Sent to topic=%s, partition=%d, offset=%d%n",
                            metadata.topic(), metadata.partition(), metadata.offset());
                    } else {
                        System.err.println("Failed to send: " + exception.getMessage());
                    }
                });
            }

            // ── Step 5: Flush — wait for all pending sends to complete ────────
            producer.flush();

        } // ── Step 6: close() called by try-with-resources ─────────────────────
    }
}
```

---

## 8. Kafka Consumer with Java (Plain API)

```java
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class PlainKafkaConsumerExample {

    public static void main(String[] args) {

        // ── Step 1: Configure the consumer ────────────────────────────────────
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Consumer group ID — Kafka tracks offsets per group
        // Multiple instances of this app with the same group ID form a group
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-service");

        // How to deserialize key and value (bytes → String)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Where to start reading if no committed offset exists for this group:
        // "earliest" = from the very first message
        // "latest"   = only new messages from now on
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Disable auto-commit so we control when offsets are committed
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // ── Step 2: Create the consumer ───────────────────────────────────────
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {

            // ── Step 3: Subscribe to topics ───────────────────────────────────
            consumer.subscribe(List.of("order.placed"));

            // ── Step 4: Poll loop ─────────────────────────────────────────────
            while (true) {

                // poll() fetches records from Kafka. Duration = max wait time.
                // Returns immediately if records are available.
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf(
                        "topic=%s, partition=%d, offset=%d, key=%s, value=%s%n",
                        record.topic(), record.partition(), record.offset(),
                        record.key(), record.value()
                    );

                    // ── Process the message ───────────────────────────────────
                    // e.g., parse JSON and update inventory

                    // ── Step 5: Manually commit the offset ────────────────────
                    // Only after successful processing — prevents data loss on crash
                    consumer.commitSync();
                }
            }
        }
    }
}
```

---

## 9. Spring Boot Kafka Integration

Spring Boot's `spring-kafka` library wraps the raw Kafka API with:
- `KafkaTemplate` — a simple, Spring-idiomatic producer.
- `@KafkaListener` — annotation-driven consumer (no poll loop needed).
- Auto-configuration — broker address, serializers, and consumer groups from `application.properties`.
- JSON serialization/deserialization of Java objects (no manual JSON strings).

---

### 9a. Dependencies & Configuration

**Add to `pom.xml`:**

```xml
<!-- Spring Kafka — KafkaTemplate + @KafkaListener -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**`src/main/resources/application.properties`:**

```properties
# ── Kafka Broker ──────────────────────────────────────────────────────────────
spring.kafka.bootstrap-servers=localhost:9092

# ── Producer ─────────────────────────────────────────────────────────────────
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# ── Consumer ─────────────────────────────────────────────────────────────────
spring.kafka.consumer.group-id=product-catalog-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.auto-offset-reset=earliest

# JsonDeserializer needs to know which packages are trusted for deserialization
spring.kafka.consumer.properties.spring.json.trusted.packages=com.training.*
```

**Topic configuration — define topics as Spring beans:**

```java
package com.training.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Spring will auto-create this topic in Kafka on startup if it doesn't exist
    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name("product.created")
                .partitions(3)          // 3 partitions for parallelism
                .replicas(1)            // 1 replica (development only; use 3 in prod)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name("product.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name("product.deleted")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
```

---

### 9b. Producing Messages — KafkaTemplate

**Event DTO:**

```java
package com.training.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// This object will be serialized to JSON and sent as the Kafka message value
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
}
```

**Producer service:**

```java
package com.training.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    // KafkaTemplate<K, V>: K = key type, V = value type
    // Spring auto-configures this bean using application.properties
    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    private static final String TOPIC_PRODUCT_CREATED = "product.created";

    /**
     * Publishes a ProductCreatedEvent to Kafka.
     *
     * The key is the productId (as String).
     * Using productId as key ensures all events for the same product
     * go to the same partition → ordered history per product.
     */
    public void publishProductCreated(ProductCreatedEvent event) {

        String key = String.valueOf(event.getProductId());

        // send() is async — returns a CompletableFuture
        CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
            kafkaTemplate.send(TOPIC_PRODUCT_CREATED, key, event);

        // Callback: called when the broker acknowledges or an error occurs
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("ProductCreatedEvent sent: productId={}, partition={}, offset={}",
                    event.getProductId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send ProductCreatedEvent: productId={}, error={}",
                    event.getProductId(), ex.getMessage());
            }
        });
    }
}
```

**Wiring the producer into ProductService:**

```java
// Inside ProductService.create() — call the producer after saving

@Transactional
public ProductResponse create(ProductRequest request) {
    Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

    Product product = Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .stockQuantity(request.getStockQuantity())
            .category(category)
            .build();

    Product saved = productRepository.save(product);

    // ── Publish event AFTER successful DB save ────────────────────────────────
    // Publishing inside @Transactional: if the transaction rolls back,
    // the event is still sent. For stronger guarantees, use Transactional Outbox.
    ProductCreatedEvent event = ProductCreatedEvent.builder()
            .productId(saved.getId())
            .productName(saved.getName())
            .price(saved.getPrice())
            .stockQuantity(saved.getStockQuantity())
            .categoryId(category.getId())
            .categoryName(category.getName())
            .createdAt(java.time.LocalDateTime.now())
            .build();

    productEventProducer.publishProductCreated(event);

    return ProductResponse.from(saved);
}
```

---

### 9c. Consuming Messages — @KafkaListener

```java
package com.training.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductEventConsumer {

    /**
     * @KafkaListener wires this method to the Kafka consumer poll loop.
     * Spring runs the poll loop in a background thread and calls this method
     * for each record fetched from the topic.
     *
     * topics      — which topic(s) to listen on
     * groupId     — consumer group (overrides application.properties default)
     * concurrency — how many listener threads (= how many partitions consumed in parallel)
     */
    @KafkaListener(
        topics = "product.created",
        groupId = "audit-service",
        concurrency = "3"         // 3 threads → one per partition
    )
    public void onProductCreated(
            ConsumerRecord<String, ProductCreatedEvent> record,
            Acknowledgment acknowledgment) {

        try {
            ProductCreatedEvent event = record.value();

            log.info("Received ProductCreatedEvent: productId={}, name={}, partition={}, offset={}",
                event.getProductId(), event.getProductName(),
                record.partition(), record.offset());

            // ── Process the event ─────────────────────────────────────────────
            // e.g., write to audit log, update search index, send welcome email
            processAuditLog(event);

            // ── Manually acknowledge after successful processing ───────────────
            // Tells Kafka: "I processed this message, advance my offset"
            // Requires: spring.kafka.listener.ack-mode=MANUAL in properties
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process ProductCreatedEvent: {}", e.getMessage());
            // Do NOT acknowledge — message will be redelivered (or go to DLT)
        }
    }

    private void processAuditLog(ProductCreatedEvent event) {
        // Implementation: save to audit_log table, push to Elasticsearch, etc.
        log.info("[AUDIT] Product created: id={}, name={}", event.getProductId(), event.getProductName());
    }
}
```

**Add to `application.properties` for manual acknowledgement:**

```properties
# MANUAL — listener must call acknowledgment.acknowledge() to commit offset
spring.kafka.listener.ack-mode=MANUAL
```

---

### 9d. Sending and Receiving Java Objects (JSON)

Spring Kafka's `JsonSerializer` and `JsonDeserializer` handle the object ↔ JSON ↔ bytes conversion automatically. You need to tell the deserializer which class to instantiate.

**Option 1 — Via properties (simpler):**

```properties
# The deserializer uses the type in the Kafka message header (set by JsonSerializer)
spring.kafka.consumer.properties.spring.json.use.type.headers=true
spring.kafka.consumer.properties.spring.json.trusted.packages=com.training.*
```

**Option 2 — Via `@KafkaListener` with explicit type:**

```java
// Specify the value type directly in the listener — no header needed
@KafkaListener(topics = "product.created", groupId = "inventory-service")
public void consume(ProductCreatedEvent event) {   // Spring auto-deserializes
    log.info("Inventory updated for product: {}", event.getProductId());
}
```

**Full producer/consumer config bean (for fine-grained control):**

```java
package com.training.config;

import com.training.events.ProductCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer Factory ──────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, ProductCreatedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer Factory ──────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, ProductCreatedEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "product-catalog-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.training.*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProductCreatedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);   // 3 threads (matches partition count)
        return factory;
    }
}
```

---

### 9e. Error Handling & Dead Letter Topics

When a consumer throws an exception, Kafka does not discard the message. Instead, Spring Kafka retries it. After all retries are exhausted, the message is sent to a **Dead Letter Topic (DLT)** for manual inspection.

```java
package com.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    /**
     * DefaultErrorHandler:
     * - Retries the message 3 times with a 1-second gap.
     * - After 3 failures, sends the message to a Dead Letter Topic (DLT).
     * - DLT topic name = original topic + ".DLT"
     *   e.g., "product.created" → "product.created.DLT"
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

        // DeadLetterPublishingRecoverer publishes failed messages to the DLT
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);

        // FixedBackOff(interval ms, max attempts)
        FixedBackOff backOff = new FixedBackOff(1000L, 3);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
```

**Listen on the DLT to inspect / alert on failed messages:**

```java
@KafkaListener(topics = "product.created.DLT", groupId = "dlt-inspector")
public void onDeadLetter(ConsumerRecord<String, ProductCreatedEvent> record) {
    log.error("[DLT] Message could not be processed after retries: key={}, value={}",
        record.key(), record.value());
    // Alert team, persist to error table, notify on-call, etc.
}
```

---

## 10. Practical Example — Order Service → Inventory Service

This ties everything together in a real microservices scenario.

### System Design

```
                  POST /orders
                       │
                  Order Service
                  (Spring Boot)
                       │ publishes to Kafka
                       ▼
              Topic: "order.placed"
             ┌─────────┬──────────┐
             │Partition0│Partition1│
             └─────────┴──────────┘
                  │              │
                  ▼              ▼
         Inventory Service    Notification Service
         (deducts stock)      (sends confirmation email)
```

### Order Service — Publishing the event

```java
// OrderPlacedEvent.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private String customerId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
}

// OrderService.java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void placeOrder(OrderRequest request) {
        // ... save order to DB ...

        OrderPlacedEvent event = OrderPlacedEvent.builder()
            .orderId(UUID.randomUUID().toString())
            .customerId(request.getCustomerId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .totalAmount(request.getTotalAmount())
            .build();

        // Key = customerId → all orders for same customer on same partition (ordered)
        kafkaTemplate.send("order.placed", event.getCustomerId(), event);
        log.info("Published OrderPlacedEvent: {}", event.getOrderId());
    }
}
```

### Inventory Service — Consuming the event

```java
// InventoryEventConsumer.java
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryRepository inventoryRepository;

    @KafkaListener(topics = "order.placed", groupId = "inventory-service")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Processing order: {} for product: {}", event.getOrderId(), event.getProductId());

        inventoryRepository.findByProductId(event.getProductId())
            .ifPresent(inventory -> {
                if (inventory.getStock() < event.getQuantity()) {
                    log.warn("Insufficient stock for product: {}", event.getProductId());
                    // Publish a "stock.insufficient" event back to Kafka
                    return;
                }
                inventory.setStock(inventory.getStock() - event.getQuantity());
                inventoryRepository.save(inventory);
                log.info("Stock deducted for product: {}, remaining: {}",
                    event.getProductId(), inventory.getStock());
            });
    }
}
```

### Notification Service — Consuming the same event independently

```java
@Component
@Slf4j
public class NotificationEventConsumer {

    // Different groupId = independent consumer group
    // Gets its own copy of every message from "order.placed"
    @KafkaListener(topics = "order.placed", groupId = "notification-service")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Sending confirmation email for order: {}", event.getOrderId());
        // emailService.send(event.getCustomerId(), "Order Confirmed", ...)
    }
}
```

---

## 11. Summary Cheat Sheet

```
┌────────────────────┬───────────────────────────────────────────────────────┐
│ Concept            │ Description                                           │
├────────────────────┼───────────────────────────────────────────────────────┤
│ Broker             │ Kafka server. Stores partitions, serves reads/writes. │
│ Topic              │ Named category. Like a DB table for events.           │
│ Partition          │ Ordered sub-log of a topic. Enables parallelism.      │
│ Offset             │ Position of a message in a partition (starts at 0).   │
│ Producer           │ Writes (publishes) messages to a topic.               │
│ Consumer           │ Reads (subscribes) messages from a topic.             │
│ Consumer Group     │ Set of consumers sharing a topic's partitions.        │
│ KafkaTemplate      │ Spring's producer abstraction (wraps raw API).        │
│ @KafkaListener     │ Spring annotation to create a consumer method.        │
│ DLT                │ Dead Letter Topic — receives messages after retries.  │
└────────────────────┴───────────────────────────────────────────────────────┘

Key rules:
  1. Partitions = max parallelism ceiling (can't have more consumers than partitions)
  2. Same key → same partition → ordered delivery for that key
  3. Commit offset AFTER processing (not before) to avoid data loss
  4. Consumer group ID separates independent subscribers on the same topic
  5. DLT is your safety net — always configure error handling
```

---

## 12. Interview Questions

**Q1. What is Apache Kafka and why is it preferred over traditional message queues?**

Kafka is a distributed event streaming platform. Unlike traditional queues (RabbitMQ), Kafka stores messages persistently on disk and allows consumers to re-read (replay) old messages by resetting their offset. It is pull-based (consumers pull at their own pace), partition-based for horizontal scaling, and supports multiple independent consumer groups reading the same topic simultaneously. This makes it ideal for event-driven microservices, audit logs, and real-time analytics.

---

**Q2. What is a Kafka Broker?**

A Kafka broker is a single Kafka server process. It receives messages from producers, stores them in partition log files on disk, and serves consumers. A cluster of brokers provides fault tolerance — when the broker that is the partition leader fails, a replica broker is elected as the new leader. In production, a minimum of 3 brokers is recommended.

---

**Q3. What is a Topic and how does it differ from a Partition?**

A topic is a logical category/feed to which messages are published (e.g., `order.placed`). A topic is split into one or more partitions, each of which is an independent ordered log of messages. Topics are the "what" (what kind of event), partitions are the "how" (how the data is distributed and parallelized). You subscribe to a topic; Kafka assigns partitions to consumers.

---

**Q4. How does Kafka guarantee message ordering?**

Kafka guarantees order **within a partition**, not across partitions. Messages with the same key are always routed to the same partition (using `hash(key) % numPartitions`), so all events for a given key (e.g., a specific user or order) are strictly ordered. If you need global ordering, use a single partition — but this eliminates parallelism.

---

**Q5. What is a Consumer Group and why is it important?**

A consumer group is a set of consumers identified by a shared `group.id`. Kafka assigns each partition to exactly one consumer within the group, enabling parallel processing. Two different groups receive independent copies of all messages. This allows independent services (inventory, notification, analytics) to consume the same topic without interfering with each other's offset tracking.

---

**Q6. What is an offset in Kafka?**

An offset is an integer that identifies a message's position within a partition. It starts at 0 and increments by 1. Kafka stores the committed offset for each (consumer group, partition) pair. When a consumer restarts, it resumes from the last committed offset. This is how Kafka provides "at-least-once" delivery — if the consumer crashes before committing, it will reprocess the last batch.

---

**Q7. What is the difference between `acks=0`, `acks=1`, and `acks=all` in a Kafka producer?**

- `acks=0` — Fire and forget. Producer does not wait for acknowledgement. Fastest, but messages can be lost if the broker crashes before writing.
- `acks=1` — Leader acknowledges. Producer waits for the partition leader to write. Faster, but if the leader crashes before replication, the message is lost.
- `acks=all` — All in-sync replicas acknowledge. Slowest but strongest durability guarantee. Required for critical data.

---

**Q8. What is `auto.offset.reset` and when would you use `earliest` vs `latest`?**

`auto.offset.reset` controls where a consumer starts reading when there is no committed offset for its group (i.e., the first time the consumer group runs, or if the offset has expired).
- `earliest` — start from the very first message in the partition (replay all history).
- `latest` — start from new messages only (ignore past messages).

Use `earliest` when a new service needs to process historical events. Use `latest` when only future events are relevant (e.g., real-time alerts).

---

**Q9. What is `KafkaTemplate` in Spring Boot?**

`KafkaTemplate` is Spring Kafka's abstraction over the raw `KafkaProducer`. It is auto-configured as a Spring bean from `application.properties`. It provides methods like `send(topic, key, value)` that return a `CompletableFuture<SendResult>`. You can attach a callback for success/failure handling. It supports both synchronous (`.get()` on the future) and asynchronous sending.

---

**Q10. What does `@KafkaListener` do and how does Spring wire it?**

`@KafkaListener` annotates a method to become a Kafka consumer. Spring creates a `ConcurrentKafkaListenerContainerFactory` that starts a background poll loop. When records arrive on the specified topic, Spring deserializes them and invokes the annotated method. The `concurrency` attribute controls how many threads (and thus partitions) are processed in parallel.

---

**Q11. What is a Dead Letter Topic (DLT) and why is it important?**

A DLT (also called Dead Letter Queue in some systems) receives messages that could not be processed after all retry attempts. Without a DLT, a poison message (one that always causes an exception) would block the consumer forever. By routing such messages to a DLT, the main consumer can continue processing. The DLT can be monitored separately for alerting, manual inspection, or reprocessing after a bug fix.

---

**Q12. What is the difference between at-most-once, at-least-once, and exactly-once delivery?**

- **At-most-once** — offset committed before processing. Message may be lost on crash (processed zero times or once).
- **At-least-once** — offset committed after processing. On crash before commit, message is redelivered (processed once or multiple times). This is the most common practical choice.
- **Exactly-once** — Kafka's transactional API (or idempotent consumers) ensures each message is processed exactly once. Requires producer idempotence + transactional consumers. Complex to implement; use only when duplicate processing is truly unacceptable (e.g., financial transactions).

---

**Q13. What is the Transactional Outbox pattern and why is it needed with Kafka?**

When you save to the DB and publish to Kafka inside the same `@Transactional` method, there is a window where the DB commit succeeds but the Kafka send fails (or vice versa). This creates inconsistency.

The Transactional Outbox pattern solves this: save the event to an `outbox` table in the same DB transaction as your business data. A separate background process (or Change Data Capture tool like Debezium) reads the outbox table and publishes to Kafka. This guarantees the event is published if and only if the DB commit succeeds.

---

**Q14. How does Kafka differ from RabbitMQ?**

| Dimension         | Kafka                          | RabbitMQ                          |
|-------------------|--------------------------------|-----------------------------------|
| Storage           | Disk (durable, replayable)     | In-memory/disk (deleted on ack)   |
| Model             | Publish-Subscribe streams      | Queue + Exchange routing          |
| Pull/Push         | Pull (consumer controls pace)  | Push (broker pushes to consumer)  |
| Replay            | Yes — reset offset             | No                                |
| Throughput        | Very high (millions/sec)       | High (tens of thousands/sec)      |
| Use case          | Event streaming, logs          | Task queues, RPC                  |

---

**Q15. How do you scale Kafka consumers?**

You scale consumers by adding more instances of the consumer application with the same `group.id`. Kafka redistributes partitions among the consumers in the group (rebalancing). The maximum effective parallelism is equal to the number of partitions — adding more consumers than partitions leaves extra consumers idle. To increase parallelism, increase the partition count when creating the topic.

---

**Q16. What is a Kafka message key and why would you use one?**

A message key is an optional field attached to a Kafka record. If present, Kafka uses `hash(key) % numPartitions` to deterministically assign the message to a partition. The main use cases are:
1. **Ordering** — all messages with the same key (e.g., same user ID) go to the same partition, guaranteeing ordered delivery for that entity.
2. **Compaction** — if log compaction is enabled on the topic, Kafka retains only the latest message per key, like a key-value store.

If no key is provided, records are distributed round-robin (no ordering guarantee).

---

**Q17. What is `JsonSerializer` and `JsonDeserializer` in Spring Kafka?**

These are Spring Kafka serializers that convert Java objects ↔ JSON ↔ bytes automatically. `JsonSerializer` converts your event object to JSON bytes before sending. `JsonDeserializer` converts the incoming bytes back to your event class. You configure the target class via `spring.kafka.consumer.properties.spring.json.value.default.type` or a type header set by the producer.

---

**Q18. What does `concurrency` mean in `@KafkaListener` and how should you set it?**

`concurrency` sets how many listener threads Spring creates for that listener. Each thread is assigned one or more partitions. The optimal value is the number of partitions of the topic — this gives each partition its own dedicated thread for maximum parallelism. Setting `concurrency` higher than the partition count wastes threads (extra ones will be idle).

---

**Q19. What happens if a consumer falls behind (consumer lag)?**

Consumer lag is the difference between the latest offset in a partition and the consumer's committed offset. High lag means the consumer is not keeping up with the producer's rate. Consequences:
- Messages accumulate in the partition (uses disk space).
- Processing delay increases (event data becomes stale).

Solutions: increase `concurrency` (more threads), add more partitions (requires rebalancing), optimize consumer logic, or scale horizontally by adding more consumer instances.

---

**Q20. What is KRaft mode in Kafka 3.x and why was it introduced?**

Prior to Kafka 3.x, Kafka required Apache ZooKeeper to store cluster metadata (broker info, controller election, topic configs). ZooKeeper was a separate system that added operational complexity, deployment overhead, and a scaling bottleneck for large clusters.

KRaft (Kafka Raft) replaces ZooKeeper with a built-in consensus mechanism based on the Raft algorithm. One broker is elected the active controller; others are observers. This simplifies deployment (single process to manage), improves startup time, supports much larger partition counts, and removes the ZooKeeper dependency entirely. KRaft is the default mode from Kafka 3.3+ and will be the only mode in Kafka 4.x.
