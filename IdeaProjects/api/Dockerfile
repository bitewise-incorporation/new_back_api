FROM openjdk:21-jdk-slim AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw install -DskipTests

FROM openjdk:21-jre-slim

RUN adduser --system --group spring

WORKDIR /home/spring

COPY --from=build /app/target/*.jar app.jar

ENV PORT 8080
USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]