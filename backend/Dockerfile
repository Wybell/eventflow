FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY config ./config
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
COPY --from=build /workspace/target/eventflow-backend-*.jar /app/eventflow-backend.jar
RUN groupadd --system eventflow \
    && useradd --system --gid eventflow --home-dir /app eventflow \
    && mkdir -p /app/uploads/avatars \
    && chown -R eventflow:eventflow /app

EXPOSE 8080
USER eventflow
ENTRYPOINT ["java", "-jar", "/app/eventflow-backend.jar"]
