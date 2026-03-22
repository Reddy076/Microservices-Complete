# Stage 1: Build from source (context = project root so parent pom.xml is available)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy root parent pom + all module poms (needed for multi-module resolution)
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY config-server/pom.xml config-server/
COPY vault-service/pom.xml vault-service/
COPY generator-service/pom.xml generator-service/
COPY security-service/pom.xml security-service/
COPY notification-service/pom.xml notification-service/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/

# Download user-service dependencies (cached Docker layer)
RUN mvn dependency:go-offline -B -pl user-service -am -q

# Copy user-service source and build
COPY user-service/src user-service/src
RUN mvn package -DskipTests -B -pl user-service -q

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
