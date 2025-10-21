FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN ./mvnw install -DskipTests
FROM eclipse-temurin:21-jre-jammy
RUN adduser --system --group spring
WORKDIR /home/spring
COPY --from=build /app/target/*.jar app.jar
ENV PORT 8080
USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]
