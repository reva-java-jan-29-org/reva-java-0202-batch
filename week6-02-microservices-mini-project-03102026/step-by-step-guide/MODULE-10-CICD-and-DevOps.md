# Module 10 — CI/CD & DevOps: Automation, Monitoring & Observability

**Type:** Hands-on + Concepts
**Duration:** ~4 hours
**Prerequisites:** Module 9 (Docker) complete — all services containerized and running
**Goal:** Automate the build, test, and deployment pipeline using GitHub Actions; add monitoring with Prometheus + Grafana; implement structured logging and distributed tracing.

---

## Learning Objectives

By the end of this module you will be able to:

1. Explain the DevOps culture and the CI/CD pipeline stages
2. Set up a GitHub Actions CI workflow that builds and tests on every push
3. Build and publish Docker images to GitHub Container Registry (ghcr.io)
4. Explain deployment strategies: blue-green, rolling, canary, recreate
5. Configure Spring Boot Actuator for health checks and readiness probes
6. Add Prometheus metrics scraping and visualize them in Grafana
7. Implement structured JSON logging for multi-service correlation
8. Add distributed tracing with Zipkin

---

## Recap — Where We Are

```
✅ Config Server    :8888  — Containerized
✅ Eureka Server    :8761  — Containerized
✅ User Service     :8081  — Containerized
✅ Product Service  :8082  — Containerized
✅ Order Service    :8083  — Containerized
✅ API Gateway      :8080  — Containerized
✅ MySQL            :3306  — Containerized
⬜ CI/CD Pipeline          ← This module
⬜ Monitoring stack         ← This module
```

---

## 10.1 DevOps Introduction

### The Problem Without DevOps

In a traditional organization, developers and operations teams work in silos:

```
┌─────────────────────┐          ┌─────────────────────┐
│   DEVELOPMENT       │          │   OPERATIONS         │
│                     │          │                      │
│  - Writes code      │   "Wall  │  - Manages servers   │
│  - Adds features    │    of    │  - Handles incidents │
│  - Fixes bugs       │Confusion"│  - Deploys releases  │
│  - Wants fast       │──────────│  - Wants stability   │
│    releases         │          │                      │
└─────────────────────┘          └─────────────────────┘

Dev: "It works on my machine"
Ops: "Then we'll ship your machine"
```

The result: slow releases, deployment failures, blame culture, and a 6-month release cycle.

### What Is DevOps?

DevOps is a **culture and set of practices** that breaks down the wall between development and operations:

- **Shared responsibility** — developers own their services in production (you build it, you run it)
- **Automation** — automate everything that can be automated: builds, tests, deployments
- **Feedback loops** — fast feedback from CI, monitoring, and users
- **Fail fast** — catch problems early (in development) rather than late (in production)

### The DevOps Infinity Loop

```
        ┌─────────────────────────────────────────────────────────┐
        │                    DEVOPS LIFECYCLE                      │
        └─────────────────────────────────────────────────────────┘

 ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
 │   PLAN   │───►│   CODE   │───►│   BUILD  │───►│   TEST   │
 │          │    │          │    │          │    │          │
 │ Jira     │    │ VS Code  │    │ Maven    │    │ JUnit    │
 │ Backlog  │    │ Git      │    │ Docker   │    │ Postman  │
 └──────────┘    └──────────┘    └──────────┘    └──────────┘
      ▲                                                │
      │                                                ▼
 ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
 │ MONITOR  │◄───│ OPERATE  │◄───│  DEPLOY  │◄───│ RELEASE  │
 │          │    │          │    │          │    │          │
 │ Grafana  │    │ K8s      │    │ GitHub   │    │ Docker   │
 │ Zipkin   │    │ Docker   │    │ Actions  │    │ Registry │
 └──────────┘    └──────────┘    └──────────┘    └──────────┘
```

---

## 10.2 CI/CD Overview

### Continuous Integration (CI)

Every developer pushes code to a shared repository multiple times per day. An automated system immediately:
1. Compiles the code
2. Runs unit and integration tests
3. Checks code quality (linting, static analysis)
4. Reports back to the developer within minutes

**Goal:** Detect integration problems early — before they compound. A bug caught in CI costs 10x less to fix than one caught in production.

**Key practices:**
- Feature branches + pull requests (no direct pushes to `main`)
- Automated tests run on every push
- Build must stay green (no broken main branch)
- Fast feedback — CI should complete in under 10 minutes

### Continuous Delivery (CD)

Every build that passes CI is **automatically packaged and deployed to a staging environment**. The team can deploy to production at any time by clicking a button.

- Testing is automated
- Deployment scripts are automated
- The **decision** to release to production is manual (human approval gate)

### Continuous Deployment

Like Continuous Delivery, but the deployment to production is also automatic — no human approval needed. Used by companies like Netflix and Amazon (they deploy thousands of times per day).

Requires very high test confidence (typically >90% coverage + E2E tests).

### The Pipeline

```
Developer pushes code
         │
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                          CI PIPELINE                               │
│                                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────────┐ │
│  │ Checkout │─►│  Build   │─►│   Test   │─►│  Static Analysis  │ │
│  │   Code   │  │ (mvn pkg)│  │(mvn test)│  │ (SpotBugs, Sonar) │ │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────────┘ │
│                                                         │          │
└─────────────────────────────────────────────────────────┼──────────┘
                                                          │
                                                   ✓ All pass
                                                          │
                                                          ▼
┌────────────────────────────────────────────────────────────────────┐
│                          CD PIPELINE                               │
│                                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Docker  │─►│  Push to │─►│ Deploy to│─►│ Deploy to Prod   │  │
│  │  Build   │  │ Registry │  │ Staging  │  │ (manual approval)│  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 10.3 Source Control Best Practices

### Git Workflow — GitHub Flow (Recommended for this project)

```
main ─────────────────────────────────────►
        │               ▲
        │feature/add-   │
        ▼cart-validation│
feature ──────────────►PR Review──► Merge
                          │
                          └─► CI runs on PR → must pass before merge
```

**GitHub Flow rules:**
1. `main` is always deployable
2. Create a branch for every change: `feature/add-pagination`, `fix/cart-total-bug`
3. Open a pull request when ready for review
4. CI runs on the PR — must pass to merge
5. Merge to `main` → triggers CD pipeline

### Branch Naming Conventions

```
feature/  → new functionality:    feature/add-order-status-update
fix/      → bug fixes:            fix/cart-empty-after-login
chore/    → maintenance:          chore/update-spring-boot-3.2.4
test/     → adding tests:         test/unit-tests-for-cart-service
docs/     → documentation:        docs/update-api-endpoints
release/  → release preparation:  release/v1.2.0
```

### Conventional Commits

```
feat: add order status update endpoint
fix: resolve cart total calculation error when quantity is 0
chore: upgrade spring-boot to 3.2.4
test: add unit tests for JwtService
docs: update Postman collection with new cart endpoints
refactor: extract JWT validation into shared utility
ci: add Docker layer caching to GitHub Actions workflow
```

Format: `<type>[optional scope]: <description>`

### `.gitignore` for This Project

```gitignore
# Maven build output
target/
*.jar
*.war
*.ear

# IDE — IntelliJ IDEA
.idea/
*.iml
*.iws

# IDE — Eclipse / STS
.eclipse/
.settings/
.project
.classpath

# IDE — VS Code
.vscode/

# macOS
.DS_Store
.AppleDouble

# Logs
*.log
logs/

# Environment — NEVER commit these
.env
.env.local
*.env

# Docker override file (local dev customizations)
docker-compose.override.yml
```

---

## 10.4 GitHub Actions — CI Pipeline

### GitHub Actions Concepts

| Concept | Description | Example |
|---|---|---|
| **Workflow** | YAML file in `.github/workflows/` | `ci.yml` |
| **Event** | Triggers the workflow | `push`, `pull_request`, `schedule` |
| **Job** | Unit of work, runs on a runner | `build`, `test`, `docker-publish` |
| **Step** | Individual command in a job | Checkout code, run Maven |
| **Runner** | VM that executes the job | `ubuntu-latest` |
| **Action** | Reusable step (`uses:`) | `actions/checkout@v4` |
| **Secret** | Encrypted variable | `${{ secrets.DOCKER_PASSWORD }}` |
| **Context** | Pre-defined variables | `${{ github.sha }}`, `${{ github.ref }}` |

### CI Workflow — `.github/workflows/ci.yml`

Create this file in your project root:

```yaml
# .github/workflows/ci.yml
# Triggers on every push to main and on all pull requests

name: CI — Build and Test

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '17'

jobs:
  build-and-test:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
      # ── Step 1: Checkout the repository ─────────────────────────────────
      - name: Checkout code
        uses: actions/checkout@v4

      # ── Step 2: Set up Java 17 (Temurin / Eclipse Adoptium) ─────────────
      - name: Set up JDK ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin      # Eclipse Temurin (formerly AdoptOpenJDK)
          cache: maven               # Cache ~/.m2 between runs (speeds up builds)

      # ── Step 3: Cache Maven dependencies ────────────────────────────────
      # actions/setup-java already handles this with cache: maven above,
      # but here's how to do it explicitly if you need more control:
      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      # ── Step 4: Build and run tests ──────────────────────────────────────
      # mvn clean verify: compiles, runs tests, runs JaCoCo, runs Checkstyle
      - name: Build and run tests
        run: mvn clean verify -B
        # -B = batch mode (no interactive prompts, cleaner CI output)

      # ── Step 5: Upload test results ─────────────────────────────────────
      # Makes test results visible in GitHub Actions UI
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()          # Upload even if tests fail (to see what failed)
        with:
          name: test-results
          path: |
            **/target/surefire-reports/*.xml
            **/target/site/jacoco/

      # ── Step 6: Upload JaCoCo coverage report ───────────────────────────
      - name: Upload coverage reports to Codecov
        uses: codecov/codecov-action@v4
        if: success()
        with:
          files: '**/target/site/jacoco/jacoco.xml'
          fail_ci_if_error: false    # Don't fail the build if Codecov is down

  # ── Docker build job (runs on main branch only, after tests pass) ────────
  docker-build-test:
    name: Docker Build Test
    runs-on: ubuntu-latest
    needs: build-and-test            # Only runs if build-and-test succeeds
    if: github.ref == 'refs/heads/main'   # Only on main branch, not PRs

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      # Test that all service images build successfully
      # (Don't push here — that's the docker-publish.yml workflow)
      - name: Build user-service image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: user-service/Dockerfile
          push: false                # build only, don't push
          tags: ecommerce/user-service:test
          cache-from: type=gha      # GitHub Actions cache for Docker layers
          cache-to: type=gha,mode=max

      - name: Build product-service image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: product-service/Dockerfile
          push: false
          tags: ecommerce/product-service:test
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Build order-service image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: order-service/Dockerfile
          push: false
          tags: ecommerce/order-service:test
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Build api-gateway image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: api-gateway/Dockerfile
          push: false
          tags: ecommerce/api-gateway:test
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

---

## 10.5 Docker Publish Pipeline — `.github/workflows/docker-publish.yml`

```yaml
# .github/workflows/docker-publish.yml
# Builds and pushes Docker images to GitHub Container Registry (ghcr.io)
# Triggers: push to main OR version tags (v1.0.0)

name: Docker — Build and Publish

on:
  push:
    branches: [ main ]
    tags: [ 'v*.*.*' ]      # e.g. v1.0.0, v2.1.3

env:
  REGISTRY: ghcr.io
  # github.repository = "your-username/microservices-mini-project"
  IMAGE_PREFIX: ghcr.io/${{ github.repository_owner }}/ecommerce

jobs:
  publish:
    name: Build and Push — ${{ matrix.service }}
    runs-on: ubuntu-latest

    # Build matrix: one job per service (runs in parallel)
    strategy:
      matrix:
        include:
          - service: config-server
            port: 8888
          - service: eureka-server
            port: 8761
          - service: user-service
            port: 8081
          - service: product-service
            port: 8082
          - service: order-service
            port: 8083
          - service: api-gateway
            port: 8080

    # Required to push to ghcr.io
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      # ── Log in to GitHub Container Registry ─────────────────────────────
      # GITHUB_TOKEN is automatically provided by GitHub Actions — no setup needed
      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      # ── Generate image metadata and tags ────────────────────────────────
      # Creates tags: latest, git SHA (e.g. sha-abc1234), and version if tagged
      - name: Extract Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.IMAGE_PREFIX }}/${{ matrix.service }}
          tags: |
            type=raw,value=latest,enable={{is_default_branch}}
            type=sha,prefix=sha-,format=short
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}

      # ── Set up Docker Buildx ────────────────────────────────────────────
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      # ── Build and push the image ────────────────────────────────────────
      - name: Build and push ${{ matrix.service }}
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ${{ matrix.service }}/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          # Layer caching: store in GitHub Actions cache
          cache-from: type=gha,scope=${{ matrix.service }}
          cache-to: type=gha,mode=max,scope=${{ matrix.service }}
```

### What Happens After a Push

```
git push origin main
         │
         ▼
GitHub Actions triggers ci.yml AND docker-publish.yml
         │
         ├─ ci.yml:
         │   ├─ JDK setup (cached)
         │   ├─ mvn clean verify
         │   ├─ Upload test artifacts
         │   └─ Build Docker images (test, no push)
         │
         └─ docker-publish.yml (6 parallel jobs):
             ├─ Login to ghcr.io
             ├─ Build config-server → push ghcr.io/your-org/ecommerce/config-server:latest
             ├─ Build eureka-server → push ghcr.io/your-org/ecommerce/eureka-server:latest
             ├─ Build user-service  → push ghcr.io/your-org/ecommerce/user-service:latest
             ├─ Build product-service → push ...
             ├─ Build order-service → push ...
             └─ Build api-gateway  → push ...

Resulting images available at:
ghcr.io/your-org/ecommerce/user-service:latest
ghcr.io/your-org/ecommerce/user-service:sha-abc1234
```

---

## 10.6 Environment Management

### Spring Profiles for Multi-Environment Config

```
application.yml        ← base config (all environments)
application-dev.yml    ← local development overrides
application-docker.yml ← Docker Compose overrides
application-staging.yml← staging environment
application-prod.yml   ← production
```

Activate a profile:
- Via property: `spring.profiles.active=docker`
- Via environment variable: `SPRING_PROFILES_ACTIVE=docker`

**Example: `user-service/src/main/resources/application-docker.yml`**

```yaml
# Overrides for Docker Compose environment
spring:
  config:
    import: "optional:configserver:http://config-server:8888"
  datasource:
    url: jdbc:mysql://mysql:3306/user_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
```

Add to docker-compose.yml:
```yaml
user-service:
  environment:
    - SPRING_PROFILES_ACTIVE=docker
```

### `.env` File Pattern for Docker Compose

Never hardcode credentials in `docker-compose.yml`. Use an `.env` file:

**`.env.example`** (commit this — it's the template):
```env
# Copy this file to .env and fill in your values
MYSQL_ROOT_PASSWORD=changeme
JWT_SECRET=changeme-minimum-32-chars
SPRING_PROFILES_ACTIVE=docker
```

**`.env`** (never commit — add to `.gitignore`):
```env
MYSQL_ROOT_PASSWORD=Root123
JWT_SECRET=ecommerce-jwt-secret-key-for-microservices-project-2024-secure
SPRING_PROFILES_ACTIVE=docker
```

**`docker-compose.yml`** — reference variables:
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}

user-service:
  environment:
    - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
```

### GitHub Secrets — For CI/CD

For production deployments, store credentials as GitHub Secrets:

```
Repository → Settings → Secrets and variables → Actions → New repository secret
```

Common secrets:
- `DOCKER_USERNAME` / `DOCKER_PASSWORD` (if using Docker Hub)
- `SONAR_TOKEN` (SonarQube)
- `DEPLOYMENT_SSH_KEY` (server access)

Access in workflow:
```yaml
- name: Deploy
  run: ssh user@server "docker compose pull && docker compose up -d"
  env:
    SSH_KEY: ${{ secrets.DEPLOYMENT_SSH_KEY }}
```

---

## 10.7 Deployment Strategies

### Recreate (Simplest)

```
Current: [v1] [v1] [v1]
         ↓ Stop all
             [  ] [  ] [  ]     ← Downtime!
             ↓ Start new
             [v2] [v2] [v2]
```

- Zero infrastructure overhead
- Has **downtime**
- Fine for development/staging
- `docker compose down && docker compose up -d`

### Rolling Update (Standard)

```
[v1] [v1] [v1] [v1]
 ↓
[v2] [v1] [v1] [v1]   → test v2
[v2] [v2] [v1] [v1]   → keep rolling
[v2] [v2] [v2] [v1]
[v2] [v2] [v2] [v2]   → done
```

- No downtime (always some instances running)
- Gradual replacement
- Kubernetes default strategy
- Health checks determine when to proceed to the next instance

### Blue-Green Deployment

```
                    Load Balancer
                         │
          ┌──────────────┴──────────────┐
          │                             │
     [Blue: v1]                   [Green: v2]
   (ACTIVE - 100%)             (IDLE - being tested)
```

1. Green environment (v2) deployed alongside Blue (v1) — no traffic yet
2. Run smoke tests against Green directly
3. Switch load balancer: 100% traffic → Green
4. Blue becomes idle (instant rollback available — just flip back)

- **Zero downtime**
- **Instant rollback**
- Requires double the infrastructure while both environments run
- Works great with Docker Compose by running two compose stacks

### Canary Release

```
Traffic split:
┌─────────────────────────────────────────┐
│  95% → [v1] instances                   │
│   5% → [v2] instances  ← monitor errors │
└─────────────────────────────────────────┘

If v2 looks healthy after 1 hour:
┌─────────────────────────────────────────┐
│  50% → [v1]                             │
│  50% → [v2]                             │
└─────────────────────────────────────────┘

If still healthy:
┌─────────────────────────────────────────┐
│   0% → [v1] (decommission)              │
│ 100% → [v2]                             │
└─────────────────────────────────────────┘
```

- Named after canary birds in coal mines (early warning)
- Expose only a small % of users to the new version
- Requires a load balancer that supports weighted routing (Nginx, Traefik, AWS ALB)
- Best for high-risk changes

---

## 10.8 Health Checks and Actuator

Spring Boot Actuator is already in all your services. Let's configure it properly.

### Actuator Endpoints

```
GET /actuator                      → lists all available endpoints
GET /actuator/health               → service health status
GET /actuator/health/liveness      → liveness probe (is the app alive?)
GET /actuator/health/readiness     → readiness probe (is the app ready for traffic?)
GET /actuator/info                 → app metadata
GET /actuator/metrics              → available metric names
GET /actuator/metrics/jvm.memory.used → specific metric
GET /actuator/env                  → resolved configuration properties
GET /actuator/beans                → all Spring beans
GET /actuator/mappings             → all request mappings
```

### Recommended Actuator Configuration

Add to each service's `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always        # Show individual component health
      probes:
        enabled: true             # Enable /health/liveness and /health/readiness
  info:
    env:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}   # Tag all metrics with service name

# Custom /actuator/info content
info:
  app:
    name: ${spring.application.name}
    version: '@project.version@'
    description: "E-Commerce Microservice"
```

### Health Response

```json
GET /actuator/health

{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 50000000000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Liveness vs Readiness (Kubernetes concepts, also useful with Docker)

- **Liveness probe** (`/actuator/health/liveness`) — "Is the app alive?"
  - If DOWN → restart the container
  - Fails if the app is in a deadlock or unrecoverable state

- **Readiness probe** (`/actuator/health/readiness`) — "Can the app handle traffic?"
  - If DOWN → remove from load balancer (don't send requests)
  - Fails if a required dependency (DB, message broker) is unavailable

In Docker Compose:
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:8081/actuator/health/readiness || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
```

---

## 10.9 Monitoring — Micrometer + Prometheus + Grafana

### The Three Pillars of Observability

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   METRICS   │  │    LOGS     │  │   TRACES    │
│             │  │             │  │             │
│ What?       │  │ What        │  │ Where?      │
│ How much?   │  │ happened?   │  │ Which path? │
│ How fast?   │  │             │  │             │
│             │  │             │  │             │
│ Prometheus  │  │ ELK/Loki    │  │ Zipkin      │
│ Grafana     │  │ Grafana     │  │ Jaeger      │
└─────────────┘  └─────────────┘  └─────────────┘
```

### Step 1: Add Prometheus Dependency

Add to each service's `pom.xml` (or to the parent `pom.xml`):

```xml
<!-- Exposes /actuator/prometheus endpoint for Prometheus scraping -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

After adding, `GET /actuator/prometheus` returns metrics in Prometheus format:

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="user-service",area="heap",id="G1 Eden Space",} 1.2345678E7

# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="user-service",method="POST",status="200",uri="/api/auth/login",} 42.0
http_server_requests_seconds_sum{application="user-service",method="POST",status="200",uri="/api/auth/login",} 2.345
```

### Step 2: Create `docker/prometheus/prometheus.yml`

```yaml
# docker/prometheus/prometheus.yml
global:
  scrape_interval: 15s          # Scrape metrics every 15 seconds
  evaluation_interval: 15s      # Evaluate alerting rules every 15 seconds

scrape_configs:
  # Scrape Prometheus itself (for monitoring the monitoring)
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Scrape all microservices
  - job_name: 'config-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['config-server:8888']
    labels:
      service: 'config-server'

  - job_name: 'eureka-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['eureka-server:8761']

  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:8081']

  - job_name: 'product-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['product-service:8082']

  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8083']

  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']
```

### Step 3: Add Prometheus + Grafana to Docker Compose

Append to `docker-compose.yml`:

```yaml
  # ── Prometheus ─────────────────────────────────────────────────────────────
  prometheus:
    image: prom/prometheus:latest
    container_name: ecommerce-prometheus
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - ecommerce-network
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'    # allows config reload via POST /prometheus/-/reload

  # ── Grafana ────────────────────────────────────────────────────────────────
  grafana:
    image: grafana/grafana:latest
    container_name: ecommerce-grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    networks:
      - ecommerce-network
    depends_on:
      - prometheus

# Add to volumes section:
volumes:
  mysql-data:
  grafana-data:     # ← add this
```

### Step 4: Configure Grafana

1. Open `http://localhost:3000` (admin/admin)
2. Go to **Configuration → Data Sources → Add data source → Prometheus**
3. URL: `http://prometheus:9090`
4. Click **Save & Test**

**Import Spring Boot Dashboard:**
1. Go to **Dashboards → Import**
2. Enter Dashboard ID: `11378` (JVM Micrometer dashboard)
3. Select the Prometheus data source
4. Click **Import**

You'll immediately see: JVM memory, GC activity, HTTP request rates, error rates, thread counts.

### Key Metrics to Monitor

```
# Request rate (per second)
rate(http_server_requests_seconds_count{application="user-service"}[5m])

# Error rate (5xx responses)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# P99 response time (99th percentile)
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap usage
jvm_memory_used_bytes{area="heap"}

# Active database connections
hikaricp_connections_active{application="user-service"}

# JVM garbage collection time
rate(jvm_gc_pause_seconds_sum[5m])
```

---

## 10.10 Structured Logging

### The Problem with Plain Text Logs in Microservices

When you have 6 services each writing logs, this is useless:

```
2024-01-15 10:30:01 INFO  User registered: alice
2024-01-15 10:30:01 INFO  Cart created for user 1
2024-01-15 10:30:02 ERROR Connection failed
```

Which service logged the error? Which request caused it? You can't tell.

### JSON Structured Logging with Logback

Add dependency to each service's `pom.xml`:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

Create `src/main/resources/logback-spring.xml` in each service:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- ── Development profile: human-readable ─────────────────────────── -->
    <springProfile name="default,dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- ── Docker/Production profile: structured JSON ──────────────────── -->
    <springProfile name="docker,staging,prod">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <!-- Include stack traces -->
                <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                    <maxDepthPerCause>10</maxDepthPerCause>
                    <shortenedClassNameLength>20</shortenedClassNameLength>
                    <rootCauseFirst>true</rootCauseFirst>
                </throwableConverter>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

</configuration>
```

**JSON log output (Docker profile):**

```json
{
  "@timestamp": "2024-01-15T10:30:01.123+11:00",
  "@version": "1",
  "message": "User registered: alice",
  "logger_name": "c.e.user.controller.AuthController",
  "thread_name": "http-nio-8081-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "application": "user-service"
}
```

Now you can search, filter, and aggregate logs across all services using tools like Grafana Loki or the ELK stack.

### Correlation IDs with MDC

When a request flows through multiple services (Gateway → Order → Product), how do you trace a single request across all logs?

**API Gateway — add a correlation ID header:**

```java
// In JwtAuthFilter, after validating the token:
String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
if (correlationId == null) {
    correlationId = UUID.randomUUID().toString();
}
// Forward it to downstream services
ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
        .header("X-User-Id", userId.toString())
        .header("X-Username", username)
        .header("X-Correlation-Id", correlationId)   // ← add this
        .build();
```

**Downstream services — put it in MDC (Mapped Diagnostic Context):**

```java
// Create a filter in each service that reads the correlation ID and puts it in MDC
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();    // Always clean up MDC
        }
    }
}
```

**`logback-spring.xml`** — include MDC fields in JSON:

The `LogstashEncoder` automatically includes all MDC fields. Your JSON logs will now contain:
```json
{
  "@timestamp": "2024-01-15T10:30:01.123+11:00",
  "message": "Adding product 1 to cart",
  "level": "INFO",
  "application": "order-service",
  "correlationId": "a3f2b1c4-..."   ← same ID across all services for this request
}
```

Now you can search `correlationId: "a3f2b1c4-..."` and see the complete request journey.

---

## 10.11 Log Aggregation — Grafana Loki

Loki is a lightweight log aggregation system (by Grafana Labs). It's like Prometheus but for logs.

### Add Loki to Docker Compose

```yaml
  # ── Loki (log storage) ─────────────────────────────────────────────────────
  loki:
    image: grafana/loki:latest
    container_name: ecommerce-loki
    ports:
      - "3100:3100"
    networks:
      - ecommerce-network

  # ── Promtail (log shipper — reads container logs and sends to Loki) ─────────
  promtail:
    image: grafana/promtail:latest
    container_name: ecommerce-promtail
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock   # read Docker container logs
      - ./docker/promtail/config.yml:/etc/promtail/config.yml
    networks:
      - ecommerce-network
    depends_on:
      - loki
```

Create `docker/promtail/config.yml`:

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        target_label: container
      - source_labels: ['__meta_docker_container_label_com_docker_compose_service']
        target_label: service
```

After starting, configure Loki as a data source in Grafana:
1. **Configuration → Data Sources → Add → Loki**
2. URL: `http://loki:3100`
3. Go to **Explore** and query: `{service="user-service"} |= "ERROR"`

---

## 10.12 Distributed Tracing — Zipkin

### The Problem

A `POST /api/orders` request touches 3 services:

```
Client → API Gateway → Order Service → Product Service
                               │
                               └── Which step took 2 seconds??
```

Without distributed tracing, you can only see each service's logs in isolation. You can't see the full journey of a single request.

### What Is Distributed Tracing?

- **Trace** — the complete journey of a single request across all services
- **Span** — a single operation within a trace (one service processing the request)
- **Trace ID** — a unique ID shared by all spans in the same trace

```
Trace ID: abc123
──────────────────────────────────────────────────────────
Span 1: api-gateway     [════════════════════] 320ms
  Span 2: order-service   [═══════════════] 290ms
    Span 3: product-service [══════════] 180ms  ← slow!
```

### Add Micrometer Tracing + Zipkin

Add to each service's `pom.xml`:

```xml
<!-- Micrometer Tracing with Brave (OpenZipkin) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Zipkin reporter — sends traces to Zipkin server -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

Add to `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0     # 1.0 = trace 100% of requests (use 0.1 in production)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### Add Zipkin to Docker Compose

```yaml
  # ── Zipkin (distributed tracing) ──────────────────────────────────────────
  zipkin:
    image: openzipkin/zipkin:latest
    container_name: ecommerce-zipkin
    ports:
      - "9411:9411"
    networks:
      - ecommerce-network
```

After starting, open `http://localhost:9411` and place an order through the gateway. Click "Find Traces" and select the `POST /api/orders` trace to see the full request waterfall across all services.

---

## 10.13 Complete Observability Stack

Here's the full docker-compose addition for the observability stack:

```yaml
# Add these services to docker-compose.yml

  prometheus:
    image: prom/prometheus:latest
    container_name: ecommerce-prometheus
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - ecommerce-network

  grafana:
    image: grafana/grafana:latest
    container_name: ecommerce-grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    networks:
      - ecommerce-network

  loki:
    image: grafana/loki:latest
    container_name: ecommerce-loki
    ports:
      - "3100:3100"
    networks:
      - ecommerce-network

  promtail:
    image: grafana/promtail:latest
    container_name: ecommerce-promtail
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./docker/promtail/config.yml:/etc/promtail/config.yml
    networks:
      - ecommerce-network

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: ecommerce-zipkin
    ports:
      - "9411:9411"
    networks:
      - ecommerce-network
```

**Observability endpoints summary:**

| Tool | URL | Purpose |
|---|---|---|
| Prometheus | http://localhost:9090 | Metrics storage + query |
| Grafana | http://localhost:3000 | Dashboards (metrics + logs) |
| Zipkin | http://localhost:9411 | Distributed trace viewer |
| Actuator Health | http://localhost:8081/actuator/health | Individual service health |
| Actuator Prometheus | http://localhost:8081/actuator/prometheus | Raw metrics |

---

## 10.14 Module Checkpoint

### Setup checklist:

- [ ] `.github/workflows/ci.yml` created and pushed to GitHub
- [ ] CI pipeline runs automatically on push — check the **Actions** tab
- [ ] All Maven tests pass in CI (`mvn clean verify`)
- [ ] `.github/workflows/docker-publish.yml` created and triggered
- [ ] Docker images visible at `https://github.com/your-username?tab=packages`

### Monitoring checklist:

- [ ] Prometheus dependency added to each service's `pom.xml`
- [ ] `GET http://localhost:8081/actuator/prometheus` returns metrics text
- [ ] Prometheus running at http://localhost:9090 → Status → Targets (all UP)
- [ ] Grafana running at http://localhost:3000 (admin/admin)
- [ ] JVM dashboard imported (ID 11378) showing heap memory and HTTP metrics
- [ ] Loki added as Grafana data source → explore logs by service

### Logging checklist:

- [ ] `logstash-logback-encoder` added to `pom.xml`
- [ ] `logback-spring.xml` created with dev/docker profiles
- [ ] `docker compose logs -f user-service` shows JSON when `SPRING_PROFILES_ACTIVE=docker`

### Tracing checklist:

- [ ] Micrometer Tracing + Zipkin reporter dependencies added
- [ ] Zipkin running at http://localhost:9411
- [ ] Place an order through the gateway → trace visible in Zipkin UI showing all spans

---

## What's Next — Module 11

With automated pipelines and observability in place, Module 11 covers **Code Quality and Reliability** — ensuring the codebase stays maintainable as it grows:

- JUnit 5 + Mockito unit tests for JwtService, AuthController, CartService
- JaCoCo coverage enforcement (70% minimum in CI)
- Checkstyle and SpotBugs for static analysis
- SonarQube for comprehensive quality gates
- OWASP dependency vulnerability scanning
- Resilience4j circuit breakers for inter-service calls
- SpringDoc OpenAPI (Swagger UI)
