# Module 9 — Docker & Containerization

**Type:** Hands-on Implementation
**Duration:** ~3 hours
**Prerequisites:** All previous modules understood (services built and running locally)
**Goal:** Containerize all 6 microservices using Docker, orchestrate the full stack with Docker Compose, and understand how to build images using the Spring Boot Maven Plugin.

---

## Learning Objectives

By the end of this module you will be able to:

1. Explain what containerization is and how it differs from virtual machines
2. Describe the Docker architecture (daemon, client, registry, images, layers)
3. Install Docker on macOS using Colima (lightweight, free alternative to Docker Desktop)
4. Write multi-stage Dockerfiles for Spring Boot services
5. Write Dockerfiles for all 6 microservices and build each image individually
6. Build Docker images using Dockerfiles, Spring Boot Buildpacks, and Jib — and understand when to use each
7. Verify all service images are built and ready before orchestration
8. Use Docker Compose to orchestrate the full 6-service stack with MySQL

---

## 9.1 What is Containerization?

### The Problem — "It Works on My Machine"

Imagine this scenario:

```
Developer's MacBook              CI Server (Ubuntu)          Production (RHEL)
─────────────────────            ──────────────────          ─────────────────
Java 17 (Temurin)                Java 11 (OpenJDK)           Java 21 (Oracle)
MySQL 8.0                        MySQL 5.7                   MySQL 8.0.33
Spring Boot 3.2.3                Same code                   Same code
Works perfectly ✓                Tests fail ✗                Crashes on startup ✗
```

The application behaves differently because the **environment** is different. This is the classic "works on my machine" problem.

### Virtual Machines vs Containers

Both VMs and containers solve the environment problem, but in different ways:

```
┌─────────────────────────────────┐    ┌─────────────────────────────────┐
│      VIRTUAL MACHINES           │    │         CONTAINERS              │
├─────────────────────────────────┤    ├─────────────────────────────────┤
│  App A    │  App B    │  App C  │    │  App A  │  App B  │  App C      │
│  Libs A   │  Libs B   │  Libs C │    │  Libs A │  Libs B │  Libs C     │
│  Guest OS │  Guest OS │  GuestOS│    ├─────────┴─────────┴─────────────┤
├───────────┴───────────┴─────────┤    │         Docker Engine           │
│          Hypervisor             │    ├─────────────────────────────────┤
├─────────────────────────────────┤    │         Host OS (macOS)         │
│         Host OS (macOS)         │    ├─────────────────────────────────┤
├─────────────────────────────────┤    │         Hardware                │
│         Hardware                │    └─────────────────────────────────┘
└─────────────────────────────────┘
   Each VM: 20-60 GB, minutes to boot    Each container: 100-500 MB, seconds to start
   Full OS per VM                        Shared OS kernel
```

**Key differences:**

| | Virtual Machines | Containers |
|---|---|---|
| Size | GBs (full OS) | MBs (app + libs only) |
| Boot time | Minutes | Seconds |
| Isolation | Hardware-level | Process-level |
| OS | Full guest OS per VM | Shared host OS kernel |
| Use case | Running different OS types | Packaging and running apps |

### What Docker Provides

- **Image** — a static, immutable blueprint. Think of it like a class in Java.
- **Container** — a running instance of an image. Think of it like an object (instance of a class).
- **Layer** — each instruction in a Dockerfile adds a layer. Layers are cached and shared.
- **Registry** — a storage server for images. Docker Hub is the public registry. ghcr.io (GitHub) and ECR (AWS) are popular alternatives.
- **Volume** — persistent storage that survives container restarts.
- **Network** — isolated network for containers to communicate.

```
Image (stored, static)           Container (running, ephemeral)
─────────────────────            ──────────────────────────────
Layer 4: app.jar          →      Process running in isolated env
Layer 3: JRE 17           →      Has its own filesystem
Layer 2: Alpine Linux     →      Has its own network
Layer 1: base image       →      Can read/write to volumes
```

---

## 9.2 Docker Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    YOUR MACHINE (macOS)                       │
│                                                              │
│   ┌─────────────┐    REST API    ┌──────────────────────┐   │
│   │ Docker CLI  │ ──────────────►│  Docker Daemon       │   │
│   │ (client)    │                │  (dockerd)           │   │
│   └─────────────┘                │                      │   │
│                                  │  - Manages images    │   │
│   docker run nginx               │  - Manages containers│   │
│   docker build .                 │  - Manages volumes   │   │
│   docker-compose up              │  - Manages networks  │   │
│                                  └──────────┬───────────┘   │
│                                             │               │
│                                  ┌──────────▼───────────┐   │
│                                  │    Container Runtime  │   │
│                                  │  (containerd / runc)  │   │
│                                  └──────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                                             │
                                   pulls from │ pushes to
                                             │
                              ┌──────────────▼──────────────┐
                              │   Registry (Docker Hub /    │
                              │   ghcr.io / ECR)            │
                              │                             │
                              │   mysql:8.0                 │
                              │   eclipse-temurin:17-jre    │
                              │   your-org/user-service     │
                              └─────────────────────────────┘
```

### Image Layers and Caching

Every `RUN`, `COPY`, and `ADD` instruction in a Dockerfile creates a new layer. Layers are cached:

```dockerfile
FROM eclipse-temurin:17-jre-alpine    # Layer 1: base JRE (cached from Docker Hub)
WORKDIR /app                          # Layer 2: creates /app directory
COPY app.jar app.jar                  # Layer 3: your JAR file
EXPOSE 8081                           # metadata only
ENTRYPOINT ["java", "-jar", "app.jar"]
```

If you rebuild and only the JAR changed, layers 1 and 2 are reused from cache — only layer 3 is rebuilt. This makes rebuilds fast.

**Layer caching strategy — put stable things first:**

```dockerfile
# WRONG — copies everything first, cache invalidated on any file change
COPY . .
RUN mvn clean package

# CORRECT — copy pom.xml first (changes rarely), src second (changes often)
COPY pom.xml .          # cached unless pom.xml changes
RUN mvn dependency:go-offline
COPY src ./src          # cache invalidated when src changes
RUN mvn clean package
```

---

## 9.3 Installing Docker on macOS — Colima (Recommended)

### Why Colima instead of Docker Desktop?

| | Docker Desktop | Colima |
|---|---|---|
| Cost | Free for personal; **paid** for orgs >250 employees or >$10M revenue | **Free** (open-source) |
| RAM usage | 4-8 GB typical | 2-4 GB typical |
| Apple Silicon | Yes | Yes (native ARM) |
| GUI | Yes | No (CLI only) |
| Complexity | All-in-one installer | Requires separate installs |
| Lima VM backend | Yes | Yes (same underlying tech) |

For learning and professional use, **Colima is the recommended choice** — it's free forever, lightweight, and works identically to Docker Desktop for all Docker commands.

### Installation Steps

**Step 1 — Install Colima and Docker CLI tools:**

```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Colima (the VM that runs Docker)
brew install colima

# Install the Docker CLI (client only, no daemon)
brew install docker

# Install Docker Compose plugin
brew install docker-compose
```

**Step 2 — Start Colima:**

```bash
# Start with default settings (2 CPU, 2GB RAM)
colima start

# OR — recommended for this project (6 services + MySQL needs more resources)
colima start --cpu 4 --memory 8 --disk 60
```

**Step 3 — Verify Docker works:**

```bash
docker info
docker version
docker run hello-world
```

You should see "Hello from Docker!" in the output.

**Step 4 — Configure Docker Compose plugin:**

```bash
# Create the Docker CLI plugins directory
mkdir -p ~/.docker/cli-plugins

# Link docker-compose as a Docker CLI plugin
ln -sfn $(brew --prefix)/opt/docker-compose/bin/docker-compose ~/.docker/cli-plugins/docker-compose

# Verify
docker compose version
```

### Colima Management Commands

```bash
colima start              # Start the VM and Docker daemon
colima stop               # Stop (containers and VM)
colima status             # Check if running
colima delete             # Delete the VM (remove all containers/images)

# Change resources (requires restart)
colima stop
colima start --cpu 4 --memory 8

# Auto-start Colima when you log in (optional)
brew services start colima
```

### Alternative: Docker Desktop

If you prefer a GUI, Docker Desktop is available at [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/). Install with:
```bash
brew install --cask docker
```
Then launch the Docker Desktop app. All Docker commands work identically.

---

## 9.4 Docker Fundamentals — Essential Commands

### Working with Images

```bash
# Pull an image from Docker Hub
docker pull mysql:8.0
docker pull eclipse-temurin:17-jre-alpine

# List local images
docker images

# Remove an image
docker rmi mysql:8.0

# Search Docker Hub
docker search spring-boot
```

### Running Containers

```bash
# Run MySQL (this is how we'll run it locally instead of installing MySQL)
docker run -d \
  --name mysql-local \
  -e MYSQL_ROOT_PASSWORD=Root123 \
  -p 3306:3306 \
  mysql:8.0

# Flags explained:
# -d            → detached (run in background)
# --name        → give the container a name
# -e KEY=value  → set environment variable
# -p host:container → map host port to container port
```

### Managing Containers

```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# View logs
docker logs mysql-local
docker logs -f mysql-local          # follow (like tail -f)

# Execute a command inside a running container
docker exec -it mysql-local bash
docker exec -it mysql-local mysql -u root -pRoot123

# Stop a container
docker stop mysql-local

# Remove a container (must be stopped first)
docker rm mysql-local

# Stop and remove in one step
docker rm -f mysql-local
```

### Volumes and Networks

```bash
# Named volume (persists after container is removed)
docker run -d \
  --name mysql-local \
  -e MYSQL_ROOT_PASSWORD=Root123 \
  -v mysql-data:/var/lib/mysql \       # persist data
  -p 3306:3306 \
  mysql:8.0

# Bind mount (map a host directory into the container)
docker run -d \
  -v /Users/vishalshah/microservices-mini-project/config-repo:/workspace/config-repo \
  config-server

# List volumes
docker volume ls

# Create a custom network
docker network create ecommerce-network

# Run container on custom network (containers can talk to each other by service name)
docker run -d --network ecommerce-network --name mysql mysql:8.0
```

### Cleanup

```bash
# Remove all stopped containers
docker container prune

# Remove all unused images
docker image prune

# Remove everything unused (containers, images, networks, build cache)
docker system prune -a

# Check disk usage
docker system df
```

---

## 9.5 Writing Dockerfiles

### Dockerfile Instruction Reference

```dockerfile
FROM eclipse-temurin:17-jre-alpine    # Base image (OS + runtime)
                                       # Always the first instruction

WORKDIR /app                           # Set working directory for all subsequent instructions
                                       # Creates the directory if it doesn't exist

COPY target/app.jar app.jar           # Copy files from host → image
                                       # COPY <src> <dest>

RUN mvn clean package -DskipTests     # Execute a shell command during BUILD time
                                       # Creates a new layer

ENV JAVA_OPTS="-Xms256m -Xmx512m"    # Set environment variable (visible at runtime)

ARG JAR_FILE=target/*.jar             # Build-time variable (docker build --build-arg)
                                       # NOT visible at runtime

EXPOSE 8081                           # Document which port the app uses
                                       # Does NOT actually open the port (use -p for that)

ENTRYPOINT ["java", "-jar", "app.jar"]  # Main command — cannot be overridden at runtime
                                        # (without --entrypoint flag)

CMD ["--spring.profiles.active=prod"]   # Default arguments to ENTRYPOINT
                                        # CAN be overridden: docker run myimage --debug
```

### `ENTRYPOINT` vs `CMD`

```dockerfile
# Pattern 1: ENTRYPOINT only (most common for Spring Boot)
ENTRYPOINT ["java", "-jar", "app.jar"]
# → docker run myimage                  runs: java -jar app.jar
# → docker run myimage --debug          runs: myimage --debug (replaces entire command!)

# Pattern 2: ENTRYPOINT + CMD (recommended)
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
# → docker run myimage                  runs: java -jar app.jar
# → docker run myimage -jar other.jar   runs: java -jar other.jar (only CMD replaced)

# Pattern 3: Shell form vs Exec form
ENTRYPOINT java -jar app.jar            # Shell form — runs via /bin/sh -c (bad: no signal handling)
ENTRYPOINT ["java", "-jar", "app.jar"]  # Exec form — runs directly (good: proper PID 1 + SIGTERM handling)
```

**Always use exec form** (square brackets) for Spring Boot. Shell form means your app doesn't receive SIGTERM when the container stops, so it won't shut down gracefully.

### Single-Stage Dockerfile (simple but large)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/user-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Image size: ~200 MB (JRE + Alpine + your JAR)

**Problem:** Requires the JAR to already be built locally. Works well for CI/CD where you build then package.

### Multi-Stage Build (self-contained, recommended)

```dockerfile
# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy parent POM (changes rarely → cached layer)
COPY pom.xml .

# Copy all child module POMs (needed to resolve the multi-module structure)
# WHY: Maven reads the entire reactor (all module POMs) to resolve the
# dependency graph — even when building only one module with -pl.
# Without sibling POMs, Maven throws "Could not find artifact" errors.
# Note: only POMs are copied here, NOT the other services' source code.
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

# Download dependencies (cached as long as POMs don't change)
RUN mvn dependency:go-offline -B

# Copy source of the specific module being built
COPY user-service/src user-service/src

# Build only the target module (-pl) and its dependencies (-am = also-make)
RUN mvn -pl user-service -am clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Only copy the built JAR — Maven, JDK, and source code are left in Stage 1
COPY --from=build /workspace/user-service/target/*.jar app.jar

EXPOSE 8081

# JVM tuning for containers — respect container memory limits
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

**Build stage image:** ~600 MB (Maven + JDK + source + dependencies)
**Final image:** ~200 MB (JRE + JAR only — Maven and source discarded)

**`-XX:+UseContainerSupport` and `-XX:MaxRAMPercentage=75.0`:**
Without these flags, the JVM reads the *host* machine's RAM (e.g. 16 GB) and sizes its heap accordingly — a container with 512 MB limit will crash with OutOfMemoryError. These flags tell the JVM to respect the container's memory limit.

### `.dockerignore` — Exclude Files from Build Context

Create `.dockerignore` in the project root:

```
# Build output (already built in multi-stage, or not needed)
**/target/

# IDE files
**/.idea/
**/*.iml
**/.eclipse/
**/.settings/

# Git
.git/
.gitignore

# Local env files
.env
**/*.env.local

# Documentation
**/teaching/
README.md

# OS files
.DS_Store
```

Without `.dockerignore`, `COPY . .` sends the entire project (including `target/` directories with all compiled classes and JARs) to the Docker daemon. For a multi-module project this can be 500+ MB of unnecessary data sent on every build.

---

## 9.6 Dockerfiles for Each Service

All Dockerfiles use the multi-stage pattern and must be **built from the project root** (because they need the parent `pom.xml`):

```bash
# Build from the project root, specify dockerfile path
docker build -f config-server/Dockerfile -t ecommerce/config-server:latest .
#                                                                            ^
#                                          build context = current directory (root)
```

### `config-server/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

RUN mvn dependency:go-offline -B

COPY config-server/src config-server/src
RUN mvn -pl config-server -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/config-server/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

```bash
# Build config-server image (run from project root)
docker build -f config-server/Dockerfile -t ecommerce/config-server:latest .
```

### `eureka-server/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

RUN mvn dependency:go-offline -B

COPY eureka-server/src eureka-server/src
RUN mvn -pl eureka-server -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

```bash
# Build eureka-server image (run from project root)
docker build -f eureka-server/Dockerfile -t ecommerce/eureka-server:latest .
```

### `user-service/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

RUN mvn dependency:go-offline -B

COPY user-service/src user-service/src
RUN mvn -pl user-service -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

```bash
# Build user-service image (run from project root)
docker build -f user-service/Dockerfile -t ecommerce/user-service:latest .
```

### `product-service/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

RUN mvn dependency:go-offline -B

COPY product-service/src product-service/src
RUN mvn -pl product-service -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/product-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

```bash
# Build product-service image (run from project root)
docker build -f product-service/Dockerfile -t ecommerce/product-service:latest .
```

### `order-service/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

RUN mvn dependency:go-offline -B

COPY order-service/src order-service/src
RUN mvn -pl order-service -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

```bash
# Build order-service image (run from project root)
docker build -f order-service/Dockerfile -t ecommerce/order-service:latest .
```

### `api-gateway/Dockerfile`

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

RUN mvn dependency:go-offline -B

COPY api-gateway/src api-gateway/src
RUN mvn -pl api-gateway -am clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

```bash
# Build api-gateway image (run from project root)
docker build -f api-gateway/Dockerfile -t ecommerce/api-gateway:latest .
```

> **Note:** First build takes 10–20 minutes per service (downloads Maven dependencies and base images). Subsequent builds use cached layers and complete in under a minute.

---

## 9.7 Alternative: Spring Boot Buildpacks

Spring Boot 3.x includes a built-in image builder using **Cloud Native Buildpacks (CNB)**. No Dockerfile required — Spring Boot inspects your project and builds an optimized layered image automatically.

### How It Works

```
mvn spring-boot:build-image
         │
         ▼
Cloud Native Buildpacks
         │
         ├─ Detects Java project
         ├─ Downloads appropriate JRE buildpack
         ├─ Creates optimized layered image automatically
         │    Layer 1: JRE runtime
         │    Layer 2: Spring Boot loader
         │    Layer 3: Dependencies (changes rarely)
         │    Layer 4: Application classes (changes often)
         └─ Pushes to local Docker daemon
```

### Configuration in `pom.xml`

Add to each service's `pom.xml` (or to the parent `pom.xml` to apply to all):

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <image>
                    <!-- Image name: ecommerce/user-service:1.0.0 -->
                    <name>ecommerce/${project.artifactId}:${project.version}</name>
                    <!-- Also tag as :latest -->
                    <tags>
                        <tag>ecommerce/${project.artifactId}:latest</tag>
                    </tags>
                    <!-- JVM settings via environment -->
                    <env>
                        <BPL_JVM_THREAD_COUNT>50</BPL_JVM_THREAD_COUNT>
                    </env>
                </image>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Building All 6 Service Images

```bash
# Build all services from the project root (builds each one sequentially)
mvn spring-boot:build-image -DskipTests

# Or build a single service
cd config-server
mvn spring-boot:build-image -DskipTests
```

> **Note:** First run downloads buildpacks (~500 MB). Subsequent builds reuse them. Expect 5–10 minutes per service on first run.

**`host.docker.internal`** — if you run a single image standalone (not via Compose), use this special DNS name to connect back to services running on the host machine:
```bash
docker run -d \
  -p 8081:8081 \
  -e SPRING_CONFIG_IMPORT=optional:configserver:http://host.docker.internal:8888 \
  ecommerce/user-service:1.0.0
```

---

## 9.8 Alternative: Jib Maven Plugin

**Jib** (from Google) builds OCI-compliant images without needing a running Docker daemon. It builds directly from Maven and pushes to any registry.

### Why Jib?

- **No Docker daemon required** — works in CI environments without Docker socket access
- **Faster incremental builds** — changes only the layers that differ
- **Reproducible builds** — same source always produces the same image
- **No Dockerfile to maintain**

### Configuration in `pom.xml`

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.0</version>
    <configuration>
        <from>
            <image>eclipse-temurin:17-jre-alpine</image>
        </from>
        <to>
            <!-- Push to Docker Hub: docker.io/yourusername/user-service:latest -->
            <image>docker.io/yourusername/user-service</image>
            <tags>
                <tag>latest</tag>
                <tag>${project.version}</tag>
            </tags>
        </to>
        <container>
            <ports>
                <port>8081</port>
            </ports>
            <jvmFlags>
                <jvmFlag>-XX:+UseContainerSupport</jvmFlag>
                <jvmFlag>-XX:MaxRAMPercentage=75.0</jvmFlag>
                <jvmFlag>-Xms256m</jvmFlag>
            </jvmFlags>
            <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
        </container>
    </configuration>
</plugin>
```

### Building All 6 Service Images

```bash
# Build and load into local Docker daemon (requires Docker to be running)
mvn jib:dockerBuild

# Build a single service
mvn -pl config-server jib:dockerBuild

# Build and push directly to a registry (no local Docker daemon needed)
mvn jib:build

# Build to a local tarball (for air-gapped environments)
mvn jib:buildTar
```

### Using Jib (or Buildpacks) with Docker Compose

Jib and Buildpacks build images **outside** of Docker Compose — Compose cannot drive their build step. The workflow is always two steps: build first, then run.

**Step 1 — Pre-build all images (run once, or after every code change):**

```bash
# Using Jib
mvn jib:dockerBuild

# Or using Spring Boot Buildpacks
mvn spring-boot:build-image -DskipTests
```

**Step 2 — Update `docker-compose.yml` to reference pre-built images by name instead of building from a Dockerfile:**

```yaml
# With Dockerfile (current setup — Compose owns the build)
config-server:
  build:
    context: .
    dockerfile: config-server/Dockerfile

# With Jib or Buildpacks (Compose only runs, does not build)
config-server:
  image: ecommerce/config-server:latest   # must already exist in local daemon
```

**Step 3 — Run as normal:**

```bash
docker compose up -d
# No --build flag needed — Compose just starts the pre-built images
```

**After a code change:**

```bash
# Re-build with Jib, then restart the affected service
mvn -pl config-server jib:dockerBuild
docker compose up -d config-server        # picks up the newly built image
```

### Comparing All Three Approaches

| | Dockerfile | `spring-boot:build-image` | Jib |
|---|---|---|---|
| File required | Yes | No | No |
| Customization | Full control | Limited via buildpack config | Good |
| Build speed | Faster (with layer cache) | Slowest (downloads buildpacks) | **Fastest** (only changed layers) |
| Image size control | Full control | Least control | Good |
| Docker daemon required | Yes | Yes | **No** |
| CI/CD friendly | Yes | OK | **Best** |
| Compose-driven rebuild | Yes (`--build`) | No — manual Maven step | No — manual Maven step |
| Best for | Production, fine-tuned images | Quick prototyping | CI/CD pipelines, microservices |

### Which Should You Use?

**Dockerfile** — choose when:
- You need full control over the runtime environment (custom tools, scripts, OS packages)
- Your team is already comfortable with Docker
- You have non-standard build steps

**`spring-boot:build-image`** — choose when:
- You want zero-config setup and trust Spring Boot's opinionated defaults
- You are prototyping and don't care about build speed

**Jib** — choose when:
- You want fast, reproducible builds without a running Docker daemon
- You are working in a CI/CD pipeline (e.g. GitHub Actions, Jenkins) where no Docker socket is available
- You want minimal configuration and no Dockerfile maintenance

> **Recommendation for this project:** Jib is the best fit for CI/CD. The existing Dockerfiles are well-written and remain useful for learning and full control — but for everyday rebuilds and CI/CD, Jib's speed and daemon-free operation make it the pragmatic choice.

---

## 9.9 Verify All Images Are Built

Before moving to Docker Compose, confirm all 6 service images are available in your local Docker daemon:

```bash
docker images | grep ecommerce
```

Expected output:

```
REPOSITORY                      TAG       IMAGE ID       CREATED         SIZE
ecommerce/api-gateway           latest    a1b2c3d4e5f6   2 minutes ago   230MB
ecommerce/order-service         latest    b2c3d4e5f6a1   3 minutes ago   225MB
ecommerce/product-service       latest    c3d4e5f6a1b2   4 minutes ago   220MB
ecommerce/user-service          latest    d4e5f6a1b2c3   5 minutes ago   228MB
ecommerce/eureka-server         latest    e5f6a1b2c3d4   6 minutes ago   215MB
ecommerce/config-server         latest    f6a1b2c3d4e5   7 minutes ago   210MB
```

All 6 images should be present. Each is ~200–230 MB (JRE + JAR only — no Maven, no source code).

> **Note:** MySQL does NOT need to be built — it uses the official `mysql:8.0` image pulled directly from Docker Hub. You do not write a Dockerfile for MySQL.

---

## 9.10 MySQL Init Script

Create `docker/mysql/init.sql` to set up all three databases in a single MySQL container:

```sql
-- docker/mysql/init.sql
-- This script runs automatically when the MySQL container starts for the first time

CREATE DATABASE IF NOT EXISTS user_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS product_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS order_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

---

## 9.11 Docker Compose — Orchestrating the Full Stack

### The Challenge: `localhost` → Container Names

When running locally, every service points to `localhost`:
- Config Server: `http://localhost:8888`
- Eureka: `http://localhost:8761/eureka/`
- MySQL: `localhost:3306`

Inside Docker, containers communicate **by service name** on a shared network:
- Config Server: `http://config-server:8888`
- Eureka: `http://eureka-server:8761/eureka/`
- MySQL: `mysql:3306`

Spring Boot supports overriding properties via environment variables. The translation is:

```
spring.datasource.url
  → SPRING_DATASOURCE_URL

eureka.client.service-url.defaultZone
  → EUREKA_CLIENT_SERVICEURL_DEFAULTZONE

spring.config.import
  → SPRING_CONFIG_IMPORT
```

This lets us keep `localhost` URLs in `application.yml` for local development and override them in Docker Compose via environment variables — no code changes needed.

### The Config Server Git URI Problem

The Config Server is configured with:
```yaml
spring.cloud.config.server.git.uri: file://${user.home}/microservices-mini-project/config-repo
```

This path doesn't exist inside a Docker container. Solution: mount the `config-repo` directory into the container and override the URI with an environment variable:

```yaml
volumes:
  - ./config-repo:/workspace/config-repo
environment:
  - SPRING_CLOUD_CONFIG_SERVER_GIT_URI=file:///workspace/config-repo
```

### Complete `docker-compose.yml`

```yaml
# docker-compose.yml
# Place this file in the project root: /Users/vishalshah/microservices-mini-project/

version: '3.8'

networks:
  ecommerce-network:
    driver: bridge

volumes:
  mysql-data:

services:

  # ── MySQL ──────────────────────────────────────────────────────────────────
  mysql:
    image: mysql:8.0
    container_name: ecommerce-mysql
    environment:
      MYSQL_ROOT_PASSWORD: Root123
      MYSQL_DATABASE: user_db        # Creates user_db; product_db and order_db from init.sql
    volumes:
      - mysql-data:/var/lib/mysql                      # persist data across restarts
      - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql  # create all DBs
    ports:
      - "3306:3306"                  # expose to host for DB clients (TablePlus, DBeaver)
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pRoot123"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s              # MySQL takes ~30s to initialize on first start

  # ── Config Server ─────────────────────────────────────────────────────────
  config-server:
    build:
      context: .                     # build context = project root (needs parent pom.xml)
      dockerfile: config-server/Dockerfile
    container_name: ecommerce-config-server
    environment:
      # Override the git URI to point to the mounted volume
      - SPRING_CLOUD_CONFIG_SERVER_GIT_URI=file:///workspace/config-repo
      # Override Eureka URL to use container name
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    volumes:
      - ./config-repo:/workspace/config-repo  # mount local config-repo into container
    ports:
      - "8888:8888"
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8888/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  # ── Eureka Server ──────────────────────────────────────────────────────────
  eureka-server:
    build:
      context: .
      dockerfile: eureka-server/Dockerfile
    container_name: ecommerce-eureka-server
    ports:
      - "8761:8761"
    networks:
      - ecommerce-network
    depends_on:
      config-server:
        condition: service_healthy   # wait until config-server is healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8761/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  # ── User Service ───────────────────────────────────────────────────────────
  user-service:
    build:
      context: .
      dockerfile: user-service/Dockerfile
    container_name: ecommerce-user-service
    environment:
      # Override all localhost references with Docker service names
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/user_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=Root123
    ports:
      - "8081:8081"
    networks:
      - ecommerce-network
    depends_on:
      mysql:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8081/actuator/health || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 60s

  # ── Product Service ────────────────────────────────────────────────────────
  product-service:
    build:
      context: .
      dockerfile: product-service/Dockerfile
    container_name: ecommerce-product-service
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/product_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=Root123
    ports:
      - "8082:8082"
    networks:
      - ecommerce-network
    depends_on:
      mysql:
        condition: service_healthy
      eureka-server:
        condition: service_healthy

  # ── Order Service ──────────────────────────────────────────────────────────
  order-service:
    build:
      context: .
      dockerfile: order-service/Dockerfile
    container_name: ecommerce-order-service
    environment:
      - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/order_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=Root123
    ports:
      - "8083:8083"
    networks:
      - ecommerce-network
    depends_on:
      mysql:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      product-service:
        condition: service_started  # OpenFeign calls product-service; use service_started (no healthcheck on product)

  # ── API Gateway ────────────────────────────────────────────────────────────
  api-gateway:
    build:
      context: .
      dockerfile: api-gateway/Dockerfile
    container_name: ecommerce-api-gateway
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    ports:
      - "8080:8080"
    networks:
      - ecommerce-network
    depends_on:
      eureka-server:
        condition: service_healthy
```

### How `depends_on` with Health Checks Works

```
docker-compose up
       │
       ├─ Start mysql ──────────────────────────────► health check: mysqladmin ping
       │                                                               ▼ (30s)
       │                                               healthy!
       │
       ├─ Start config-server (depends: mysql healthy)── health check: GET /actuator/health
       │                                                               ▼ (30s)
       │                                               healthy!
       │
       ├─ Start eureka-server (depends: config-server healthy)
       │                                              ▼ (30s)
       │                                           healthy!
       │
       ├─ Start user-service (depends: mysql + eureka healthy)
       ├─ Start product-service (depends: mysql + eureka healthy)
       ├─ Start order-service (depends: mysql + eureka + product started)
       └─ Start api-gateway (depends: eureka healthy)
```

Without health checks and `depends_on`, all services would start simultaneously — user-service would fail to connect to MySQL before it's ready.

---

## 9.12 Docker Compose Commands

```bash
# Start all services (build images if not cached)
docker compose up -d
# NOTE: Compose only builds an image if it doesn't already exist locally.
# If your source code changed but the image is already cached, the old image
# is reused. Always pass --build after a code change.

# Rebuild images before starting (required after code changes)
docker compose up -d --build

# Start only specific services
docker compose up -d mysql eureka-server config-server

# Follow logs for all services
docker compose logs -f

# Follow logs for a specific service
docker compose logs -f user-service
docker compose logs -f --tail=100 order-service   # last 100 lines

# Status of all services
docker compose ps

# Stop all services (containers remain, can be restarted)
docker compose stop

# Stop AND remove containers
docker compose down

# Stop, remove containers, AND remove named volumes (clears all database data)
docker compose down -v

# Restart a single service (e.g. after changing application.yml)
docker compose restart user-service

# Rebuild and restart a single service
docker compose up -d --build user-service

# Scale a service (run 3 instances — requires different port mapping)
docker compose up -d --scale product-service=3

# Execute a command inside a running service container
docker compose exec mysql mysql -u root -pRoot123
docker compose exec user-service sh

# View resource usage
docker stats
```

### Accessing MySQL from the Host

With `ports: - "3306:3306"` in the compose file, you can connect from any MySQL client:

```bash
# From the host machine
mysql -h 127.0.0.1 -P 3306 -u root -pRoot123

# Or using docker compose
docker compose exec mysql mysql -u root -pRoot123 -e "SHOW DATABASES;"
```

---

## 9.13 Package Structure for Docker Files

After this module, your project root should have:

```
microservices-mini-project/
├── docker-compose.yml               ← orchestrates all services
├── .dockerignore                    ← excludes target/, .git/, etc.
│
├── docker/
│   └── mysql/
│       └── init.sql                 ← creates all 3 databases
│
├── config-server/
│   └── Dockerfile
├── eureka-server/
│   └── Dockerfile
├── user-service/
│   └── Dockerfile
├── product-service/
│   └── Dockerfile
├── order-service/
│   └── Dockerfile
└── api-gateway/
    └── Dockerfile
```

---

## 9.14 Common Docker Troubleshooting

### Container exits immediately

```bash
docker compose logs user-service
# Look for: "Application failed to start", "Connection refused", "Port already in use"
```

### Port already in use on the host

```bash
# Find what's using port 8081
lsof -i :8081
# Kill it or change the host port mapping in docker-compose.yml: "8091:8081"
```

### Services can't talk to each other

```
# WRONG — localhost inside a container refers to the container itself
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/user_db

# CORRECT — use the service name from docker-compose.yml
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/user_db
```

All services must be on the same Docker network (`ecommerce-network`).

### MySQL "Access denied" or connection refused at startup

MySQL takes ~30 seconds to initialize on first boot. The `healthcheck` + `depends_on: condition: service_healthy` pattern handles this. If you see connection errors in early startup logs, wait — the service will retry.

If MySQL never becomes healthy, check:
```bash
docker compose logs mysql
# Look for: "[Server] /usr/sbin/mysqld: ready for connections."
```

### "no space left on device" build errors

```bash
docker system df          # check disk usage
docker system prune -a    # remove unused images, containers, build cache
```

### Apple Silicon (M1/M2/M3) — Platform Mismatch

Some base images are only available for AMD64. If you see `exec format error`:

```bash
# Force AMD64 emulation (slower but compatible)
docker run --platform linux/amd64 myimage

# Or specify in docker-compose.yml:
services:
  myservice:
    platform: linux/amd64
    image: ...

# Better: use multi-platform base images that support arm64
# eclipse-temurin:17-jre-alpine → supports both amd64 and arm64
```

### Container memory issues

```bash
# Check container memory usage
docker stats

# If container is OOMKilled (out of memory), increase memory limit
services:
  user-service:
    deploy:
      resources:
        limits:
          memory: 512m
```

---

## 9.15 Module Checkpoint

### Step 1 — Build all images

```bash
cd /Users/vishalshah/microservices-mini-project
docker compose build
```

Watch for `Successfully built` for each service. First build takes 10-20 minutes (downloads Maven dependencies). Subsequent builds use cached layers.

### Step 2 — Start the full stack

```bash
docker compose up -d
```

### Step 3 — Verify all services are running

```bash
docker compose ps
```

Expected output: all 7 containers (mysql + 6 services) showing `running (healthy)` or `running`.

### Step 4 — Check Eureka Dashboard

Open `http://localhost:8761` — all 5 Spring services should be registered.

### Step 5 — Postman validation

- `POST http://localhost:8080/api/auth/register` → 200 with token
- `GET http://localhost:8080/api/products` → 200 with 8 products
- `POST http://localhost:8080/api/auth/login` → 200 with token
- `GET http://localhost:8080/api/customers` (with token) → 200

### Checklist

- [ ] `colima start` and `docker info` show Docker is running
- [ ] `docker compose build` completes for all 6 services
- [ ] `docker compose up -d` starts all 7 containers
- [ ] `docker compose ps` shows all containers running
- [ ] Eureka dashboard at http://localhost:8761 shows all 5 services
- [ ] Postman: register → login → place order all work through the containerized stack
- [ ] `docker compose down -v && docker compose up -d` starts cleanly from scratch

---

## What's Next — Module 10

With the full stack containerized, Module 10 covers **CI/CD and DevOps** — automating the build, test, and deployment pipeline using GitHub Actions:

- Automated build and test on every code push
- Building and publishing Docker images to GitHub Container Registry
- Deployment strategies (blue-green, rolling, canary)
- Monitoring with Prometheus + Grafana
- Structured logging and distributed tracing
