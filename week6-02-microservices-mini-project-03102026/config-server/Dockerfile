# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build
# Build context must be the project root:
#   docker build -f config-server/Dockerfile -t config-server:latest .
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy root POM first (layer-cached — only re-downloaded when POM changes)
COPY pom.xml .

# Copy all child POMs so Maven reactor can resolve the module graph
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/

# Download all dependencies declared in POMs (cached layer — skips on re-builds
# when only source code changed but POMs are unchanged)
RUN mvn dependency:go-offline -pl config-server -am -q

# Copy config-server source code
COPY config-server/src config-server/src

# Build only config-server (-pl) and its dependencies (-am = also-make)
RUN mvn -pl config-server -am clean package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Minimal JRE image — no Maven, no source code
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Add curl for healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy the executable fat JAR from the build stage
COPY --from=build /app/config-server/target/*.jar app.jar

EXPOSE 8888

# Allow JVM options to be passed via JAVA_OPTS environment variable
# e.g. -e JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
