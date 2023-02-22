
# Build stage

FROM maven:3.9.0-eclipse-temurin-17 AS build
ARG mavenOptions
COPY / /home/app/
RUN mvn ${mavenOptions} -f /home/app/pom.xml clean install -DskipTests
RUN mvn ${mavenOptions} -f /home/app/server/pom.xml clean package -DskipTests


# Package stage

FROM eclipse-temurin:17-jre

WORKDIR /opt/flare
COPY ontology /opt/flare/ontology
COPY --from=build /home/app/server/target/server-*.jar /opt/flare/flare.jar
RUN chown -R 10001:10001 /opt/flare
ARG VERSION=0.0.0
ENV APP_VERSION=${VERSION}

USER 10001
ENTRYPOINT ["java","-jar","flare.jar"]
EXPOSE 8080

ARG GIT_REF=""
ARG BUILD_TIME=""
LABEL maintainer="num-codex" \
    org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="num-codex" \
    org.opencontainers.image.source="https://github.com/num-codex/codex-feasibility-backend" \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="num-codex" \
    org.opencontainers.image.title="codex feasibility backend" \
    org.opencontainers.image.description="Provides backend functions for feasibility UI including query execution"
