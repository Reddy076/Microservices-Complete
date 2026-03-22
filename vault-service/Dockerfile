# Stage 1: Build from source
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY config-server/pom.xml config-server/
COPY vault-service/pom.xml vault-service/
COPY generator-service/pom.xml generator-service/
COPY security-service/pom.xml security-service/
COPY notification-service/pom.xml notification-service/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/

RUN mvn dependency:go-offline -B -pl vault-service -am -q

COPY vault-service/src vault-service/src
RUN mvn package -DskipTests -B -pl vault-service -q

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/vault-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
