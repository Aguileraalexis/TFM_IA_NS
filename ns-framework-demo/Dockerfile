# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# El contexto de build debe ser la raíz del workspace (C:\sistemas\TFE\full)
# para poder copiar tanto el framework como la app demo.
COPY ns-framework ./ns-framework
COPY ns-framework-demo ./ns-framework-demo

# Compila el framework y empaqueta la app demo.
RUN mvn -f ns-framework/pom.xml -pl ../ns-framework-demo -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy

# DockerPlanner invoca el binario docker; por eso se instala el cliente en runtime.
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/ns-framework-demo/target/ns-framework-demo-app-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

