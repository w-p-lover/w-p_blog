FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=builder /workspace/target/*.jar /app/app.jar
COPY --from=builder /workspace/src/main/resources/static /app/static

RUN mkdir -p /app/config /data/blog/upload

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
