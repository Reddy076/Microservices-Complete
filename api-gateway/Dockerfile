# Stage 1: Build from source (context = project root so parent pom.xml is available)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy root parent pom + all module poms (needed for multi-module resolution)
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY config-server/pom.xml config-server/
COPY user-service/pom.xml user-service/
COPY vault-service/pom.xml vault-service/
COPY generator-service/pom.xml generator-service/
COPY security-service/pom.xml security-service/
COPY notification-service/pom.xml notification-service/
COPY api-gateway/pom.xml api-gateway/

# Download api-gateway dependencies (cached Docker layer)
RUN mvn dependency:go-offline -B -pl api-gateway -am -q

# Copy api-gateway source and build
COPY api-gateway/src api-gateway/src
RUN mvn package -DskipTests -B -pl api-gateway -q

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
