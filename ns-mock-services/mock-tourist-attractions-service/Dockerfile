FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY mock-tourist-attractions-service/pom.xml mock-tourist-attractions-service/pom.xml
COPY mock-tourist-attractions-service/src mock-tourist-attractions-service/src
RUN mvn -q -f mock-tourist-attractions-service/pom.xml package spring-boot:repackage -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/mock-tourist-attractions-service/target/mock-tourist-attractions-service-0.1.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]