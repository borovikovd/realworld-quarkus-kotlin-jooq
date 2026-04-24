FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 AS builder

USER root
WORKDIR /build

COPY gradlew .
COPY gradle gradle
COPY gradle.properties .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY db db
COPY src src

RUN chmod +x gradlew && \
    ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=false --no-daemon

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0

WORKDIR /work/
RUN chown 1001 /work && chmod "g+rwX" /work && chown 1001:root /work
COPY --from=builder --chown=1001:root --chmod=0755 /build/build/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["/work/application", "-Dquarkus.http.host=0.0.0.0"]
