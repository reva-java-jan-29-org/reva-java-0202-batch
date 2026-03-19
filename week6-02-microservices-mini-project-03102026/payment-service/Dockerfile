FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY config-server/pom.xml    config-server/
COPY eureka-server/pom.xml    eureka-server/
COPY api-gateway/pom.xml      api-gateway/
COPY user-service/pom.xml     user-service/
COPY product-service/pom.xml  product-service/
COPY order-service/pom.xml    order-service/
COPY payment-service/pom.xml  payment-service/
RUN mvn dependency:go-offline -pl payment-service -am -q
COPY payment-service/src payment-service/src
RUN mvn -pl payment-service -am clean package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/payment-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
