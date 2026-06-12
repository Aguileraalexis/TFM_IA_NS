FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY mock-hotel-booking-service/pom.xml mock-hotel-booking-service/pom.xml
COPY mock-hotel-booking-service/src mock-hotel-booking-service/src
RUN mvn -q -f mock-hotel-booking-service/pom.xml package spring-boot:repackage -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/mock-hotel-booking-service/target/mock-hotel-booking-service-0.1.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]