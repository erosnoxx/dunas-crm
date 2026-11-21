# syntax=docker/dockerfile:1
#
# Imagem da API. Contexto de build: backend/ (ver compose.dev.yml).
# O build e o runtime ficam em JDK 21: o maven-enforcer-plugin recusa outro (crm-build-and-test).

# --- build ---------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /src

COPY . .

# O cache do ~/.m2 sobrevive entre builds: so baixa dependencia que mudou.
# Testes ficam fora: a suite usa Testcontainers e roda no "mvn test", nao aqui.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -pl edge/boot -am package \
 && cp edge/boot/target/boot-*.jar /app.jar

# --- runtime -------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50"

RUN addgroup -S crm && adduser -S crm -G crm

WORKDIR /app

COPY --from=build --chown=crm:crm /app.jar app.jar

USER crm

EXPOSE 8080

# So /actuator/health e publico (architecture.md §12). Ele depende de banco, Redis, RabbitMQ e SMTP.
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=10 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
