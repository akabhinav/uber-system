FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY ridex-commons/ ridex-commons/
COPY location-service/ location-service/
RUN apk add --no-cache maven && \
    mvn -q -pl ridex-commons,location-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S ridex && adduser -S ridex -G ridex
USER ridex
COPY --from=builder /build/location-service/target/location-service.jar app.jar
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
