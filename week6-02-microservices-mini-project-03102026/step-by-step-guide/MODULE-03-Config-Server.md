# Module 3 — Config Server: The First Running Service

**Type:** Hands-on Implementation
**Duration:** ~1.5 hours
**Prerequisites:** Module 2 complete — parent POM, all child POMs, and `config-repo/` committed to Git
**Goal:** Implement, start, and fully validate the Spring Cloud Config Server so it serves externalised configuration to all future services

---

## Learning Objectives

By the end of this module, participants will be able to:

1. Explain how the Config Server reads properties from a Git repository
2. Describe the role of `@EnableConfigServer` and what auto-configuration it triggers
3. Write the minimal code and configuration required to run a Config Server
4. Read and interpret the Config Server's JSON response
5. Understand the URL convention `/{service-name}/{profile}` and how it maps to files
6. Explain the difference between `file://`, `file:///`, and `http://` URIs for Git repos
7. Describe how client services will later connect to this server

---

## Recap — Why Config Server Comes First

Before writing a single line of business logic, we need the Config Server running. Here is why the order matters:

```
Step 1 → Config Server starts      (reads config-repo/)
Step 2 → Eureka Server starts      (registers with Config Server — optional)
Step 3 → Business services start   (EACH fetches config from Config Server on boot)
Step 4 → API Gateway starts        (routes to Eureka-registered services)
```

Every business service (User, Product, Order) will call the Config Server **at startup** to fetch its datasource URL, credentials, and other settings. If the Config Server is not running when they start, those services will fail to connect to their databases.

The Config Server is the **foundation** — nothing meaningful runs without it.

---

## Part 1 — How Spring Cloud Config Server Works

### 1.1 The Request–Response Lifecycle

```
Business Service starts up
         │
         │ Spring Boot reads application.yml:
         │   spring.config.import: "optional:configserver:http://localhost:8888"
         │
         ▼
HTTP GET http://localhost:8888/user-service/default
         │
         ▼
Config Server receives request
         │
         │ Parses URL: service-name = "user-service", profile = "default"
         │
         ▼
Config Server uses JGit to:
  1. Clone/fetch the Git repository (file:// or http://)
  2. Checkout branch "main" (default-label)
  3. Read file "user-service.properties"  ← matches service-name
  4. Also read "application.properties"   ← global config for ALL services
         │
         ▼
Returns JSON response with all properties
         │
         ▼
Business Service merges those properties into its Spring Environment
  (Config Server properties OVERRIDE local application.yml values)
         │
         ▼
DataSource bean is created using the fetched datasource.url, username, password
Hibernate connects to MySQL → tables created/validated
Service is fully started
```

### 1.2 The URL Convention

The Config Server exposes this URL pattern:

```
GET /{application}/{profile}
GET /{application}/{profile}/{label}
```

| Segment | Meaning | Example |
|---|---|---|
| `application` | Matches `spring.application.name` of the requesting service | `user-service` |
| `profile` | Spring profile active in the client | `default`, `dev`, `prod` |
| `label` | Git branch or tag | `main`, `v1.2`, `release` |

**File resolution order** (Config Server looks for these, merges all found):

```
config-repo/{application}-{profile}.properties    (most specific)
config-repo/{application}.properties
config-repo/application-{profile}.properties
config-repo/application.properties                (least specific — global)
```

So `GET /user-service/default` would look for:
1. `user-service-default.properties` — profile-specific override
2. `user-service.properties` — service-specific config ← our file
3. `application-default.properties` — global profile-specific override
4. `application.properties` — global config for all services

This hierarchy lets you put shared config (e.g. a common logging level) in `application.properties` and service-specific config (e.g. datasource URL) in `{service-name}.properties`.

### 1.3 What JGit Is

Spring Cloud Config Server uses **JGit** — a pure Java implementation of the Git protocol — to read from Git repositories. This means:

- No need for the `git` command to be on the server's PATH
- Works with local `file://` repos, remote `https://` repos, and SSH repos
- The Config Server performs a `git clone` into a local temp directory, then reads files from that clone
- `force-pull: true` tells JGit to always refresh from the source before serving (prevents stale config)

---

## Part 2 — File Structure to Create

In this module we create exactly **two files** inside the `config-server/` module:

```
config-server/
├── pom.xml                                    ← already done in Module 2
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── ecommerce/
        │           └── config/
        │               └── ConfigServerApplication.java    ← Step 1
        └── resources/
            └── application.yml                             ← Step 2
```

That is the complete implementation. The Config Server requires almost no code — it is almost entirely driven by auto-configuration triggered by `@EnableConfigServer`.

---

## Part 3 — Step-by-Step Implementation

### Step 1 — Create the Main Application Class

Create the file:
```
config-server/src/main/java/com/ecommerce/config/ConfigServerApplication.java
```

```java
package com.ecommerce.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer                // ← This single annotation does all the work
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**Line-by-line explanation:**

| Annotation / Statement | What It Does |
|---|---|
| `@SpringBootApplication` | Combines `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. Bootstraps the Spring application context. |
| `@EnableConfigServer` | Triggers Spring Cloud Config's auto-configuration. Registers the `/` REST endpoints that serve properties. Configures the Git-backed `EnvironmentRepository` bean that reads from the Git repo. |
| `SpringApplication.run(...)` | Starts the embedded Tomcat server, initialises the Spring context, and begins listening on the configured port. |

> **Key insight:** Without `@EnableConfigServer`, this would be a plain Spring Boot app with no config-serving behaviour. That one annotation activates the entire Config Server machinery — endpoint registration, JGit integration, profile resolution logic — all via Spring Boot auto-configuration.

---

### Step 2 — Write the application.yml

Create the file:
```
config-server/src/main/resources/application.yml
```

```yaml
# ── Server Configuration ──────────────────────────────────────────────
server:
  port: 8888
  # 8888 is the Spring Cloud Config Server convention.
  # Client services look for the Config Server on this port by default.

# ── Spring Application ────────────────────────────────────────────────
spring:
  application:
    name: config-server
    # This name is used when registering with Eureka.
    # Client services that need config do NOT use this name —
    # they connect directly to http://localhost:8888.

  cloud:
    config:
      server:
        git:
          # ── Git Repository URI ──────────────────────────────────────
          # file:// = local filesystem Git repository
          # Two slashes (file://) + one from the leading / in ${user.home}
          # = three slashes total in the final path → correct absolute path
          #
          # On macOS: ${user.home} = /Users/yourname
          # Expands to: file:///Users/yourname/microservices-mini-project/config-repo
          #
          # WRONG: file:///${user.home}  → four slashes → path resolution error
          # RIGHT: file://${user.home}   → three slashes → correct absolute path
          uri: file://${user.home}/microservices-mini-project/config-repo

          # The Git branch to read from.
          # Modern Git (2.28+) defaults to 'main' on init.
          # Older Git defaults to 'master'.
          # Always verify with: cd config-repo && git branch
          default-label: main

          # force-pull: true instructs JGit to always do a git pull
          # from the source before serving config.
          # Prevents the Config Server from serving stale properties
          # after you commit a change to config-repo.
          force-pull: true

# ── Eureka Client ─────────────────────────────────────────────────────
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    # Note: Eureka may not be running yet when Config Server starts.
    # That is fine — the Config Server will log connection errors
    # but will start successfully and serve config independently.
  instance:
    prefer-ip-address: true

# ── Actuator ──────────────────────────────────────────────────────────
management:
  endpoints:
    web:
      exposure:
        include: "*"
  # Exposes /actuator/health, /actuator/env, /actuator/refresh, etc.
  # Useful for monitoring and for triggering config refreshes later.
```

**Key decisions explained:**

#### Why port 8888?

8888 is the conventional default port for Spring Cloud Config Server. Client services are pre-configured by Spring Cloud auto-configuration to look for the Config Server at `http://localhost:8888` if no other URL is specified. Using the convention reduces configuration needed in client services.

#### The `file://` URI — why two slashes, not three?

This is a common source of confusion. Let's break it down:

```
file://  +  ${user.home}  +  /microservices-mini-project/config-repo
  ↓              ↓                       ↓
file://  +  /Users/john   +  /microservices-mini-project/config-repo
  ↓
file:///Users/john/microservices-mini-project/config-repo
         ↑
         Three slashes in the final URL — this is correct for an absolute path
```

A `file://` URI uses the format `file://[host]/path`. For a local path (no host), the host is empty, giving `file:///path` (three slashes). Since `${user.home}` already starts with `/`, using `file://${user.home}` naturally produces `file:///...` — exactly right.

If you wrote `file:///${user.home}`, the expansion would be `file:////Users/john/...` — four slashes — which is malformed.

#### Why is Eureka configured if it might not be running?

The Config Server registers with Eureka for **discoverability** — client services can, in theory, discover the Config Server's address through Eureka rather than hard-coding `localhost:8888`. However, since Config Server must start before Eureka in our startup order, the Eureka registration will fail on first attempt with a `Connection refused` error. This is harmless — the Config Server continues to start and serve config. Eureka registration retries automatically every 30 seconds.

---

## Part 4 — Understanding Auto-Configuration

When `@EnableConfigServer` is present, Spring Boot's auto-configuration activates the following key beans:

```
@EnableConfigServer
    │
    ▼
ConfigServerAutoConfiguration
    │
    ├── EnvironmentController    ← Handles GET /{application}/{profile} requests
    │                              Translates URL to properties lookup
    │
    ├── ConfigServerProperties   ← Binds spring.cloud.config.server.* from YAML
    │
    └── MultipleJGitEnvironmentRepository
            │
            ├── Clones the Git repo into a temp dir using JGit
            ├── Checks out the specified label (branch)
            ├── Reads *.properties or *.yml files matching the request
            └── Returns an Environment object (list of PropertySource objects)
```

You do not need to write or configure any of these beans — `@EnableConfigServer` handles it all. Your job is simply to provide the correct `application.yml` so these beans are configured with the right Git URI, label, and other settings.

---

## Part 5 — Starting the Config Server

### 5.1 From the Terminal

```bash
cd ~/microservices-mini-project/config-server
mvn spring-boot:run
```

### 5.2 From IntelliJ IDEA

1. In the Project view, expand `config-server` → `src/main/java` → `com.ecommerce.config`
2. Right-click `ConfigServerApplication`
3. Select **Run 'ConfigServerApplication'**

### 5.3 What to Look for in the Logs

A successful startup produces output like this (key lines annotated):

```log
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \

  :: Spring Boot ::                (v3.2.3)

INFO  ConfigServerApplication - Starting ConfigServerApplication
INFO  ConfigServerApplication - The following 1 profile is active: "default"

INFO  TomcatEmbeddedWebappClassLoader - Initializing Spring embedded WebApplicationContext

INFO  MultipleJGitEnvironmentRepository - Initializing with URI [file:///Users/john/microservices-mini-project/config-repo]
                                          ↑
                                          Config Server found and connected to your Git repo

INFO  EurekaAutoServiceRegistration - Registering application config-server with eureka
  OR
WARN  DiscoveryClient - Connection refused for http://localhost:8761/eureka/
                        ↑
                        This is fine — Eureka isn't started yet.
                        Config Server will retry registration every 30s.

INFO  ConfigServerApplication - Started ConfigServerApplication in 4.231 seconds (process running for 4.8)
                                 ↑
                                 Server is up and listening on port 8888
```

> **Watch for these warning signs:**
> - `No such file or directory` → `config-repo/` path is wrong or doesn't exist
> - `cannot open git-upload-pack` → Git repo not initialised (`git init` not run)
> - `branch 'master' not found` → Branch mismatch (`default-label: master` but repo is on `main`)
> - `Port 8888 already in use` → Another process is on port 8888

---

## Part 6 — Validation

This is the most important validation step in the entire training. Before moving to Module 4, every participant must confirm the Config Server is correctly serving properties.

### 6.1 Browser Validation

Open your browser and navigate to:

```
http://localhost:8888/user-service/default
```

You should see a JSON response. Let's understand every field:

```json
{
  "name": "user-service",
  "profiles": ["default"],
  "label": "main",
  "version": "bdc8c49a3f1...",
  "state": null,
  "propertySources": [
    {
      "name": "file:///Users/john/.../config-repo/user-service.properties",
      "source": {
        "spring.datasource.url": "jdbc:mysql://localhost:3306/user_db?...",
        "spring.datasource.username": "root",
        "spring.datasource.password": "root",
        "spring.datasource.driver-class-name": "com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto": "update",
        "spring.jpa.show-sql": "true",
        "spring.jpa.properties.hibernate.dialect": "org.hibernate.dialect.MySQLDialect",
        "jwt.secret": "ecommerce-jwt-secret-key-for-microservices-project-2024-secure",
        "jwt.expiration": "86400000"
      }
    }
  ]
}
```

**Field-by-field explanation:**

| Field | Value | Meaning |
|---|---|---|
| `name` | `"user-service"` | The application name from the URL |
| `profiles` | `["default"]` | The profile from the URL |
| `label` | `"main"` | The Git branch that was checked out |
| `version` | `"bdc8c49..."` | The Git commit hash that was read |
| `propertySources` | Array | List of property sources found, most specific first |
| `source` | Object | The actual key-value properties from the file |

> **Why `version` matters:** The commit hash proves exactly which commit of the config was read. If something is broken, you can `git log` in `config-repo/` and trace what changed.

### 6.2 Test All Three Services

Validate that all three services' configs are served correctly:

```bash
# User service config
curl http://localhost:8888/user-service/default | python3 -m json.tool

# Product service config
curl http://localhost:8888/product-service/default | python3 -m json.tool

# Order service config
curl http://localhost:8888/order-service/default | python3 -m json.tool
```

Each should return a JSON response with a `propertySources` array containing the relevant datasource properties.

### 6.3 Verify Git Integration — Make a Change and Refresh

This exercise demonstrates the live config update capability:

**Step 1** — Modify a property in the config repo:
```bash
cd ~/microservices-mini-project/config-repo
# Add a test property to user-service.properties
echo "test.property=hello-from-config-server" >> user-service.properties
git add .
git commit -m "Add test property"
cd ..
```

**Step 2** — Immediately hit the Config Server endpoint again:
```bash
curl http://localhost:8888/user-service/default | python3 -m json.tool
```

You should see `"test.property": "hello-from-config-server"` in the response — **without restarting the Config Server**. This is the power of externalised configuration with `force-pull: true`.

**Step 3** — Clean up:
```bash
cd config-repo
# Remove the test line
# (edit user-service.properties and remove the last line)
git add .
git commit -m "Remove test property"
cd ..
```

### 6.4 Test the Actuator Endpoints

The Config Server exposes Spring Boot Actuator endpoints that are useful for monitoring:

```bash
# Health check
curl http://localhost:8888/actuator/health
# Expected: {"status":"UP"}

# View all environment properties the Config Server itself is using
curl http://localhost:8888/actuator/env | python3 -m json.tool

# View all beans in the context (useful for debugging)
curl http://localhost:8888/actuator/beans | python3 -m json.tool
```

---

## Part 7 — How Client Services Will Use the Config Server

Although we haven't built the client services yet, it's important to understand now how they will connect so you can see the full picture.

### 7.1 Client Configuration

Each business service (User, Product, Order) will have this in its `application.yml`:

```yaml
spring:
  application:
    name: user-service      # MUST match the filename in config-repo/
                            # user-service → looks for user-service.properties

  config:
    import: "optional:configserver:http://localhost:8888"
    # optional: → if Config Server is unreachable, start anyway with local config
    # Remove 'optional:' to make Config Server mandatory (fail-fast if unreachable)
```

### 7.2 What `spring.config.import` Does

This property was introduced in Spring Boot 2.4 and replaces the old `bootstrap.yml` approach.

When a service starts, Spring Boot processes `application.yml` and encounters `spring.config.import`. It then:

1. Makes an HTTP GET request to `http://localhost:8888/user-service/default`
2. Receives the JSON response with all properties
3. Creates a `PropertySource` from the response and adds it to the `Environment`
4. **Config Server properties have higher priority** than the local `application.yml`

This means: if `application.yml` says `spring.jpa.show-sql=false` but `user-service.properties` in the config repo says `spring.jpa.show-sql=true`, the Config Server value wins.

### 7.3 The Old Bootstrap Approach (For Reference)

Before Spring Boot 2.4, the approach was a separate `bootstrap.yml` file:

```yaml
# OLD WAY — bootstrap.yml (still works but deprecated)
spring:
  application:
    name: user-service
  cloud:
    config:
      uri: http://localhost:8888
```

This required an additional dependency (`spring-cloud-starter-bootstrap`) and was processed before `application.yml`. You may see this in older Spring Cloud tutorials — the modern `spring.config.import` approach is preferred.

---

## Part 8 — Profiles: Dev, Staging, Production

Although our project uses only the `default` profile, understanding profile-based configuration is essential for real-world usage.

### 8.1 How Profiles Work with Config Server

If a service is started with the `prod` profile:
```bash
java -jar user-service.jar --spring.profiles.active=prod
```

The Config Server will look for (in order):
1. `user-service-prod.properties` ← production-specific overrides
2. `user-service.properties` ← base service config
3. `application-prod.properties` ← global production overrides
4. `application.properties` ← global base config

This lets you have:

```
config-repo/
├── application.properties          ← Shared across ALL services, ALL environments
├── user-service.properties         ← User service defaults
├── user-service-dev.properties     ← User service dev overrides
├── user-service-prod.properties    ← User service prod overrides (prod DB URL)
├── product-service.properties
└── order-service.properties
```

### 8.2 Example Profile Separation

```properties
# user-service.properties (base — all environments)
spring.jpa.show-sql=true
jwt.expiration=86400000

# user-service-prod.properties (production only)
spring.datasource.url=jdbc:mysql://prod-db.company.com:3306/user_db
spring.datasource.username=prod_user
spring.datasource.password=<production-secret>
spring.jpa.show-sql=false    # Never log SQL in production
jwt.expiration=3600000       # Shorter token expiry in production (1 hour)
```

---

## Part 9 — Common Mistakes and How to Fix Them

### Mistake 1 — Whitelabel Error Page on `http://localhost:8888/user-service/default`

**Root cause A — Wrong Git branch:**
```yaml
# Wrong:
default-label: master   # but git branch shows: * main

# Fix:
default-label: main
```

Always run `cd config-repo && git branch` to verify the branch name before starting.

**Root cause B — Four slashes in the file URI:**
```yaml
# Wrong:
uri: file:///${user.home}/...   # expands to file:////Users/... (4 slashes)

# Fix:
uri: file://${user.home}/...    # expands to file:///Users/... (3 slashes)
```

**Root cause C — No commits in config-repo:**
```bash
cd config-repo && git log
# If this shows "fatal: your current branch 'main' does not have any commits yet"
# then:
git add .
git commit -m "Initial config"
```

### Mistake 2 — Config Server Starts But Returns Empty `propertySources`

```json
{
  "name": "user-service",
  "propertySources": []    ← empty!
}
```

**Cause:** The properties file name doesn't match the requested application name.

Check:
- File is named `user-service.properties` (not `userservice.properties` or `UserService.properties`)
- The URL used is `/user-service/default` (with the hyphen)
- The file is committed to Git (not just created on disk — must be committed)

```bash
cd config-repo && git status
# If file shows as "Untracked" or "modified":
git add . && git commit -m "Fix: add/update properties files"
```

### Mistake 3 — Services Can't Connect to Config Server

```
APPLICATION FAILED TO START
Could not resolve placeholder 'spring.datasource.url'
```

**Cause:** A business service started before the Config Server was ready, or `spring.config.import` is missing from the service's `application.yml`.

**Fix:**
1. Always start Config Server first
2. Verify `spring.config.import: "optional:configserver:http://localhost:8888"` is in the service's `application.yml`
3. Verify the service's `spring.application.name` matches the config file name

### Mistake 4 — Eureka Connection Errors Spam the Logs

```
WARN  DiscoveryClient - Can't get a response from http://localhost:8761/eureka/
```

**This is not an error** — it is expected if Eureka is not running. The Config Server will still function normally. The warning repeats every 30 seconds (Eureka retry interval) until Eureka starts.

Once Eureka starts in Module 4, these warnings will stop and you'll see:
```
INFO  DiscoveryClient - Registered instance CONFIG-SERVER/...
```

---

## Part 10 — Module Checkpoint

Before proceeding to Module 4, confirm all of the following:

```
□ Config Server starts without errors on port 8888
□ http://localhost:8888/user-service/default returns JSON with propertySources
□ The JSON contains: datasource.url, datasource.username, datasource.password
□ http://localhost:8888/product-service/default returns JSON with propertySources
□ http://localhost:8888/order-service/default returns JSON with propertySources
□ http://localhost:8888/actuator/health returns {"status":"UP"}
□ Making a change to config-repo/ (commit) is reflected immediately in the next API call
□ Eureka connection warnings in logs are understood and accepted as normal at this stage
```

**Do not proceed to Module 4 until all boxes are checked.**

---

## Module 3 — Summary

| What We Built | Key Learning |
|---|---|
| `ConfigServerApplication.java` with `@EnableConfigServer` | One annotation activates the entire Config Server — no boilerplate |
| `application.yml` with Git URI and `default-label` | `file://${user.home}/...` = correct 3-slash format; `main` not `master` |
| Git-backed property serving | Config Server reads committed files — uncommitted changes are invisible |
| URL convention `/{application}/{profile}` | Filename in config-repo must match `spring.application.name` of client |
| Profile-based config resolution | Dev/staging/prod configs without changing code |
| `force-pull: true` | Live config updates — change a property, commit, and it's available immediately |

---

## What's Next — Module 4

With the Config Server validated, we build the **Eureka Service Registry**.

In Module 4 we will:
1. Implement `EurekaServerApplication.java` with `@EnableEurekaServer`
2. Write its `application.yml` with self-registration disabled
3. Start Eureka and explore the **live dashboard** at `http://localhost:8761`
4. Watch the Config Server register itself with Eureka
5. Understand what the dashboard tells us about service health

After Module 4, both infrastructure services will be running, and we will be ready to build the first business service — User Service.

---

*End of Module 3*
