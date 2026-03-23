FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY ridex-commons/ ridex-commons/
COPY trip-service/ trip-service/
RUN apk add --no-cache maven && \
    mvn -q -pl ridex-commons,trip-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S ridex && adduser -S ridex -G ridex
USER ridex
COPY --from=builder /build/trip-service/target/trip-service.jar app.jar
ENTRYPOINT ["java", \
  "-Dspring.threads.virtual.enabled=true", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
