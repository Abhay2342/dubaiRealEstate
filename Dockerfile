# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy POM first so dependency resolution is cached as a separate layer.
# Re-runs only when pom.xml changes (not on every src change).
COPY pom.xml .
RUN mvn dependency:go-offline -B -q 2>/dev/null || \
    mvn dependency:resolve  -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Use a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/dubai-real-estate-*.jar app.jar

EXPOSE 8080

# -XX:MaxRAMPercentage=75 lets the JVM use up to 75% of container RAM
# automatically — no hard-coded -Xmx needed
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
