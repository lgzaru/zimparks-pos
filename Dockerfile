# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Cache Maven dependencies as a separate layer (only re-runs when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/logs && chown spring:spring /app/logs

USER spring

COPY --from=builder /build/target/zimparks-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
