# Module 2 — Project Setup: Maven Multi-Module Structure & Config Repository

**Type:** Hands-on Implementation
**Duration:** ~1.5 hours
**Prerequisites:** Module 1 completed, Java 17 + Maven 3.8+ + MySQL 8 + Git installed
**Goal:** Create the complete project skeleton — parent POM, all module placeholders, and the Git-backed config repository — and verify the structure compiles cleanly before writing any business logic

---

## Learning Objectives

By the end of this module, participants will be able to:

1. Explain what a Maven multi-module project is and why we use one
2. Write a parent POM that manages versions for all child modules
3. Import the Spring Cloud BOM to align dependency versions
4. Create placeholder child modules with minimal POMs
5. Set up a Git repository as the configuration source
6. Write environment-specific properties files for each service
7. Validate the full project structure compiles with `mvn clean install`

---

## Part 1 — Understanding Maven Multi-Module Projects

### 1.1 What Is a Multi-Module Project?

A Maven **multi-module project** is one Maven build that manages several related sub-projects (modules) together. There is one **parent POM** at the root and one **child POM** in each module directory.

```
microservices-mini-project/      ← Root (parent POM here)
│
├── pom.xml                      ← PARENT POM
├── config-server/
│   └── pom.xml                  ← Child POM (inherits from parent)
├── eureka-server/
│   └── pom.xml
├── api-gateway/
│   └── pom.xml
├── user-service/
│   └── pom.xml
├── product-service/
│   └── pom.xml
└── order-service/
    └── pom.xml
```

### 1.2 Why Multi-Module for Microservices?

You could have six completely separate Maven projects. But a multi-module structure gives important benefits:

| Benefit | Detail |
|---|---|
| **Single `mvn install`** | One command builds all modules in the correct dependency order |
| **Centralised version management** | Spring Boot version, Spring Cloud BOM, JJWT version — declared once in parent |
| **No version drift** | Child modules can't accidentally use different Spring versions |
| **IDE integration** | IntelliJ and Eclipse understand the multi-module structure natively |
| **Shared build config** | Plugin configuration (e.g. `spring-boot-maven-plugin`) declared once |

### 1.3 The Three Roles of the Parent POM

The parent POM plays three distinct roles:

**Role 1 — It inherits from Spring Boot's parent:**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.3</version>
</parent>
```
This gives every child module all of Spring Boot's opinionated defaults — compiler settings, dependency versions for common libraries, plugin management.

**Role 2 — It imports the Spring Cloud BOM:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
The BOM (Bill of Materials) is a special POM that contains no code — only `<dependencyManagement>` entries. Importing it means child modules can declare `spring-cloud-starter-gateway` or `spring-cloud-config-server` **without specifying a version** — Maven looks up the correct version from the BOM automatically.

**Role 3 — It lists all modules:**
```xml
<modules>
    <module>config-server</module>
    <module>eureka-server</module>
    <module>api-gateway</module>
    <module>user-service</module>
    <module>product-service</module>
    <module>order-service</module>
</modules>
```
This tells Maven which directories contain child modules. `mvn install` at the root level builds all of them.

---

## Part 2 — Directory Structure

### 2.1 What We Will Create in This Module

```
microservices-mini-project/
│
├── pom.xml                                   ← Step 1: Parent POM
│
├── config-repo/                              ← Step 2: Git config repository
│   ├── user-service.properties
│   ├── product-service.properties
│   └── order-service.properties
│
├── config-server/
│   └── pom.xml                               ← Step 3: Placeholder child POM
│
├── eureka-server/
│   └── pom.xml                               ← Step 3: Placeholder child POM
│
├── api-gateway/
│   └── pom.xml                               ← Step 3: Placeholder child POM
│
├── user-service/
│   └── pom.xml                               ← Step 3: Placeholder child POM
│
├── product-service/
│   └── pom.xml                               ← Step 3: Placeholder child POM
│
└── order-service/
    └── pom.xml                               ← Step 3: Placeholder child POM
```

> **Note:** We are NOT writing any Java code in this module. We are only creating the project skeleton and config repository. Business logic, application classes, and resources come in later modules.

---

## Part 3 — Step-by-Step Implementation

### Step 1 — Create the Project Root Directory

Open a terminal and create the project directory:

```bash
mkdir -p ~/microservices-mini-project
cd ~/microservices-mini-project
```

All subsequent steps are run from this directory.

---

### Step 2 — Write the Parent POM

Create the file `pom.xml` at the project root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!--
      Role 1: Inherit from Spring Boot's parent POM.
      This gives all child modules:
        - Java compiler settings (source/target version via java.version property)
        - Managed versions for hundreds of common libraries (Jackson, Hibernate, etc.)
        - spring-boot-maven-plugin pre-configured
    -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>  <!-- tells Maven: don't look locally, fetch from Maven Central -->
    </parent>

    <!-- This project's own coordinates -->
    <groupId>com.ecommerce</groupId>
    <artifactId>microservices-mini-project</artifactId>
    <version>1.0.0</version>

    <!--
      packaging = pom means this POM does NOT produce a jar or war.
      It is a container-only POM that manages child modules.
      This is REQUIRED for a multi-module parent.
    -->
    <packaging>pom</packaging>

    <name>E-Commerce Microservices Project</name>

    <!--
      Role 3: Declare all child modules.
      The value must match the sub-directory name.
      Maven builds them in the order listed (respecting inter-module dependencies).
    -->
    <modules>
        <module>config-server</module>
        <module>eureka-server</module>
        <module>api-gateway</module>
        <module>user-service</module>
        <module>product-service</module>
        <module>order-service</module>
    </modules>

    <!--
      Shared properties available to all child modules via ${property.name}
    -->
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>

    <!--
      Role 2: Import the Spring Cloud BOM.
      dependencyManagement does NOT add dependencies — it only declares versions.
      Child modules must still explicitly declare what they need.
      But they do NOT specify versions — those come from here.
    -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!--
      Shared build plugin configuration.
      spring-boot-maven-plugin is inherited by all child modules automatically.
      Child modules that are NOT Spring Boot apps (none in our case) can exclude it.
    -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

**Key things to understand:**

- `<packaging>pom</packaging>` — this is what makes it a parent POM, not a regular project
- `<relativePath/>` — empty tag tells Maven to fetch `spring-boot-starter-parent` from Maven Central, not look in parent directories
- `<dependencyManagement>` with `<scope>import</scope>` — this is how BOMs are imported in Maven
- The `${spring-cloud.version}` property in `<version>` is resolved from the `<properties>` section above

---

### Step 3 — Create Placeholder Child POMs

Each child module needs its own `pom.xml`. For now, these are **minimal stubs** — just enough for Maven to recognise them as valid modules. We will fill in the dependencies in later modules.

Create each directory and its POM file:

#### config-server/pom.xml

```bash
mkdir config-server
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!--
      Every child module references the parent.
      relativePath="../pom.xml" tells Maven to look one directory up.
      This is how the child inherits from the parent.
    -->
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <!-- Only artifactId is needed — groupId and version are inherited from parent -->
    <artifactId>config-server</artifactId>
    <name>Config Server</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
            <!-- No <version> — resolved from Spring Cloud BOM in parent -->
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

</project>
```

#### eureka-server/pom.xml

```bash
mkdir eureka-server
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>eureka-server</artifactId>
    <name>Eureka Server</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

</project>
```

#### api-gateway/pom.xml

```bash
mkdir api-gateway
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>api-gateway</artifactId>
    <name>API Gateway</name>

    <dependencies>
        <!--
          spring-cloud-starter-gateway pulls in spring-boot-starter-webflux.
          Do NOT add spring-boot-starter-web here — the two are mutually exclusive.
          Gateway is reactive (WebFlux); adding the servlet web starter causes conflicts.
        -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!-- JWT library: API (compile), impl + jackson (runtime only) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

</project>
```

> **Why `jjwt-impl` and `jjwt-jackson` are `runtime` scope:**
> At compile time, you only use the `jjwt-api` interfaces. The implementation classes (`jjwt-impl`) and the Jackson-based serialiser (`jjwt-jackson`) are only needed at runtime. Declaring them as `runtime` keeps your compile classpath clean — you can't accidentally code against implementation internals.

#### user-service/pom.xml

```bash
mkdir user-service
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>user-service</artifactId>
    <name>User Service</name>

    <dependencies>
        <!-- Servlet-based REST API (Spring MVC) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- ORM + Repository pattern (Spring Data JPA + Hibernate) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <!-- BCrypt password encoding -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!-- Registers this service with Eureka -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <!-- Fetches datasource config from Config Server at startup -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <!-- MySQL JDBC driver (runtime only — not needed at compile time) -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- JWT generation -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <!-- Reduces boilerplate (getters, setters, constructors) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

#### product-service/pom.xml

```bash
mkdir product-service
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>product-service</artifactId>
    <name>Product Service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

#### order-service/pom.xml

```bash
mkdir order-service
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>microservices-mini-project</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>order-service</artifactId>
    <name>Order Service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <!--
          OpenFeign: only the Order Service needs this.
          It is the only service that calls another service (Product Service).
        -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

---

### Step 4 — Set Up the Config Repository

The Config Repository is a **separate Git repository** that holds the externalised configuration for each business service. It lives inside the project folder but is a completely independent Git repo.

#### 4.1 Why a Separate Git Repo for Config?

In a real production setup, the config repository would be a **separate private GitHub/GitLab repository**:

```
github.com/your-org/ecommerce-app          ← Application code
github.com/your-org/ecommerce-config       ← Configuration (separate repo)
```

Benefits of separation:
- Config changes do not trigger a full application build/CI pipeline
- Access control — fewer people need access to production DB credentials
- Config history is tracked independently from code history
- The Config Server can watch for commits and push live updates

For this training project, we use a **local Git directory** for simplicity.

#### 4.2 Create and Initialise the Config Repository

```bash
# From project root
mkdir config-repo
cd config-repo
git init
```

#### 4.3 Write the Properties Files

Create one properties file per business service. The filename must exactly match the service's `spring.application.name`.

**`config-repo/user-service.properties`**

```properties
# -------------------------------------------------------
# Datasource Configuration for User Service
# -------------------------------------------------------
spring.datasource.url=jdbc:mysql://localhost:3306/user_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# -------------------------------------------------------
# JPA / Hibernate Configuration
# -------------------------------------------------------
# update: Hibernate will ADD new columns/tables, never DROP existing ones
# create-drop: drops and recreates schema on every restart (dev only)
# validate: only checks schema matches entities, never modifies DB
# none: no DDL management at all (production recommended — use Flyway instead)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# -------------------------------------------------------
# JWT Configuration
# Key must be at least 256 bits (32 characters) for HS256.
# The SAME secret must be configured in the API Gateway.
# -------------------------------------------------------
jwt.secret=ecommerce-jwt-secret-key-for-microservices-project-2024-secure
jwt.expiration=86400000
```

**`config-repo/product-service.properties`**

```properties
# -------------------------------------------------------
# Datasource Configuration for Product Service
# -------------------------------------------------------
spring.datasource.url=jdbc:mysql://localhost:3306/product_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# -------------------------------------------------------
# JPA / Hibernate Configuration
# -------------------------------------------------------
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

**`config-repo/order-service.properties`**

```properties
# -------------------------------------------------------
# Datasource Configuration for Order Service
# -------------------------------------------------------
spring.datasource.url=jdbc:mysql://localhost:3306/order_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# -------------------------------------------------------
# JPA / Hibernate Configuration
# -------------------------------------------------------
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

#### 4.4 Commit the Config Files

The Config Server uses JGit to read the repository. **There must be at least one commit** before the Config Server can read from it.

```bash
# From inside config-repo/
git add .
git commit -m "Initial configuration for all services"

# Verify the branch name (important — must match default-label in Config Server)
git branch
# Expected output:  * main
```

> **Common Mistake:** Modern Git defaults to `main` as the initial branch name. Older Git versions defaulted to `master`. The Config Server's `default-label` setting must match the actual branch name. Always run `git branch` to verify.

```bash
cd ..   # Return to project root
```

---

## Part 4 — Understanding the JDBC URL Parameters

The datasource URL contains several query parameters worth understanding:

```
jdbc:mysql://localhost:3306/user_db
  ?createDatabaseIfNotExist=true     ← Creates DB schema if it doesn't exist
  &useSSL=false                      ← Disables SSL (required for local dev)
  &serverTimezone=UTC                ← Prevents timezone-related issues
  &allowPublicKeyRetrieval=true      ← Required by MySQL 8 auth plugin in some configs
```

> **Production note:** In production, `useSSL=false` would be replaced with proper SSL certificate configuration. `createDatabaseIfNotExist=true` is convenient for development but in production the schema should be pre-created via migration tools (Flyway, Liquibase).

---

## Part 5 — Create MySQL Databases

Before any service starts, the database schemas must exist (even though `createDatabaseIfNotExist=true` handles this automatically, it is good practice to create them explicitly).

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS product_db;
CREATE DATABASE IF NOT EXISTS order_db;

-- Verify
SHOW DATABASES;
EXIT;
```

---

## Part 6 — Validation Checkpoint

### 6.1 Verify Project Structure

Your directory tree should look like this:

```bash
# Run from project root
find . -name "pom.xml" | sort
```

Expected output:
```
./pom.xml
./api-gateway/pom.xml
./config-server/pom.xml
./eureka-server/pom.xml
./order-service/pom.xml
./product-service/pom.xml
./user-service/pom.xml
```

```bash
find . -name "*.properties" | sort
```

Expected output:
```
./config-repo/order-service.properties
./config-repo/product-service.properties
./config-repo/user-service.properties
```

### 6.2 Validate Maven Build

Run the build from the project root. Since there are no Java source files yet, this just validates that all POMs are syntactically correct and all declared dependencies can be resolved:

```bash
cd ~/microservices-mini-project
mvn clean install -DskipTests
```

**What to look for in the output:**

```
[INFO] Reactor Build Order:
[INFO]   E-Commerce Microservices Project            [pom]
[INFO]   Config Server                               [jar]
[INFO]   Eureka Server                               [jar]
[INFO]   API Gateway                                 [jar]
[INFO]   User Service                                [jar]
[INFO]   Product Service                             [jar]
[INFO]   Order Service                               [jar]

...

[INFO] BUILD SUCCESS
```

The **Reactor Build Order** confirms that Maven has found and ordered all 6 modules correctly.

> **If you see `BUILD FAILURE`:**
> - Check that every child `pom.xml` has the correct `<parent>` coordinates
> - Check `<relativePath>../pom.xml</relativePath>` is correct
> - Check that the `<modules>` section in the root POM lists directory names that actually exist

### 6.3 Validate Config Repository

```bash
cd config-repo
git log --oneline
```

Expected output (at least one commit):
```
bdc8c49 Initial configuration for all services
```

```bash
git branch
```

Expected output:
```
* main
```

```bash
cd ..
```

---

## Part 7 — Dependency Decisions Explained

Several decisions in the POMs deserve deeper explanation. Understanding these now prevents confusion later.

### Why does only Order Service have OpenFeign?

OpenFeign is only needed when a service needs to **call another service**. In our system:

- User Service → calls nobody
- Product Service → calls nobody
- Order Service → calls Product Service (to get product details and reduce stock)

Only Order Service needs `spring-cloud-starter-openfeign`.

### Why does Config Server have `eureka-client` but no `config-client`?

Config Server registers itself with Eureka (so other services can discover it), but it does not fetch its own configuration from itself. Its configuration comes from its own `application.yml`.

Bootstrapping problem: if Config Server fetched config from a Config Server, which Config Server would it talk to?

### Why does API Gateway NOT have `spring-boot-starter-web`?

Spring Cloud Gateway is built on **Spring WebFlux** (reactive). The `spring-cloud-starter-gateway` dependency already brings in `spring-boot-starter-webflux` transitively.

Adding `spring-boot-starter-web` (servlet-based) alongside WebFlux causes a conflict — Spring Boot cannot be both reactive and servlet-based. This is one of the most common mistakes with Spring Cloud Gateway.

```
spring-cloud-starter-gateway
    └── spring-boot-starter-webflux  ← already here
          └── spring-webflux
                └── reactor-netty    ← reactive HTTP server

DO NOT also add:
spring-boot-starter-web
    └── spring-webmvc                ← conflicts with webflux
          └── tomcat                 ← conflicts with reactor-netty
```

### Why does the parent POM have `spring-boot-maven-plugin` but child POMs do not?

The `spring-boot-maven-plugin` configuration in the parent's `<build><plugins>` is inherited by all child modules. Since all six modules are Spring Boot applications, they all need it.

If a child module were NOT a Spring Boot application (e.g. a shared library module), it would need to explicitly exclude this plugin to avoid Maven trying to repackage a library as a runnable fat JAR.

---

## Part 8 — Project Structure Reference Card

Keep this as a quick reference for the rest of the training:

```
microservices-mini-project/
│
├── pom.xml                   ← Parent POM (Spring Boot 3.2.3, Cloud 2023.0.0)
│
├── config-repo/              ← Git repo (NOT part of the app; read by Config Server)
│   ├── user-service.properties
│   ├── product-service.properties
│   └── order-service.properties
│
├── config-server/            ← Port 8888 | @EnableConfigServer
│   └── pom.xml              ← Deps: spring-cloud-config-server, eureka-client
│
├── eureka-server/            ← Port 8761 | @EnableEurekaServer
│   └── pom.xml              ← Deps: spring-cloud-starter-netflix-eureka-server
│
├── api-gateway/              ← Port 8080 | Reactive (WebFlux) | @EnableDiscoveryClient
│   └── pom.xml              ← Deps: gateway, eureka-client, security, jjwt
│
├── user-service/             ← Port 8081 | user_db | JWT generation
│   └── pom.xml              ← Deps: web, jpa, security, eureka-client, config, mysql, jjwt
│
├── product-service/          ← Port 8082 | product_db | Public catalogue
│   └── pom.xml              ← Deps: web, jpa, security, eureka-client, config, mysql
│
└── order-service/            ← Port 8083 | order_db | Calls Product Service via Feign
    └── pom.xml              ← Deps: web, jpa, security, eureka-client, config, openfeign, mysql
```

---

## Module 2 — Summary

| What We Built | Why |
|---|---|
| Root `pom.xml` with `<packaging>pom</packaging>` | Multi-module container; no code produced |
| `<parent>` → Spring Boot 3.2.3 | Inherits Spring Boot defaults for all child modules |
| `<dependencyManagement>` → Spring Cloud BOM | Version alignment for all Spring Cloud dependencies |
| `<modules>` listing all 6 services | Maven knows what to build and in what order |
| 6 child `pom.xml` files | Each module declares its own specific dependencies |
| `config-repo/` as a Git repository | Config Server requires a committed Git repo to read from |
| 3 properties files | Each business service's datasource config, externalised from code |
| `mvn clean install -DskipTests` passes | Skeleton is valid and all dependencies resolve |

---

## What's Next — Module 3

With the skeleton in place, the next module implements the **first running service: the Config Server**.

We will:
1. Write `ConfigServerApplication.java` with `@EnableConfigServer`
2. Write `application.yml` pointing to the config-repo Git repository
3. Start the Config Server
4. Validate it by hitting `http://localhost:8888/user-service/default` in the browser and seeing our properties returned as JSON

**This will be the first time we run any code in the project.**

---

*End of Module 2*
