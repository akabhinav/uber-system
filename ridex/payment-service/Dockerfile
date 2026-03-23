FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY ridex-commons/ ridex-commons/
COPY payment-service/ payment-service/
RUN apk add --no-cache maven && \
    mvn -q -pl ridex-commons,payment-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S ridex && adduser -S ridex -G ridex
USER ridex
COPY --from=builder /build/payment-service/target/payment-service.jar app.jar
ENTRYPOINT ["java", \
  "-Dspring.threads.virtual.enabled=true", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
