FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY mock-flight-booking-service/pom.xml mock-flight-booking-service/pom.xml
COPY mock-flight-booking-service/src mock-flight-booking-service/src
RUN mvn -q -f mock-flight-booking-service/pom.xml package spring-boot:repackage -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/mock-flight-booking-service/target/mock-flight-booking-service-0.1.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]