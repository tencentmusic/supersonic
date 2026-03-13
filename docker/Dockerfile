# syntax=docker/dockerfile:1
# SuperSonic Multi-Stage Build
# Usage: docker build -f docker/Dockerfile -t supersonicbi/supersonic:latest .

# ── Stage 1: Backend ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY . .
COPY docker/settings.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -DskipTests -Dspotless.skip=true -pl launchers/standalone -am -q \
    && tar xzf launchers/standalone/target/launchers-standalone-*-bin.tar.gz -C /tmp

# ── Stage 2: Frontend ────────────────────────────────────────────────────────
FROM node:18-slim AS frontend
RUN npm config set registry https://registry.npmmirror.com && npm i -g pnpm
WORKDIR /build
COPY webapp/ .
ENV NODE_OPTIONS=--openssl-legacy-provider
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm config set registry https://registry.npmmirror.com \
    && cd packages/chat-sdk && pnpm i && pnpm run build \
    && cd ../supersonic-fe && pnpm link ../chat-sdk && pnpm i && pnpm run build:os-local

# ── Stage 3: Custom JRE via jlink (glibc-based for Debian runtime) ──────────
FROM eclipse-temurin:21-jdk-jammy AS jre-build
RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.desktop,java.instrument,java.logging,java.management,\
java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,\
java.security.sasl,java.sql,java.xml,jdk.crypto.ec,jdk.httpserver,jdk.management,\
jdk.naming.dns,jdk.naming.rmi,jdk.net,jdk.unsupported \
    --strip-debug --no-man-pages --no-header-files \
    --compress=zip-9 --output /custom-jre

# ── Stage 4: Runtime (Debian slim + custom JRE) ───────────────────────────────
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends procps curl && rm -rf /var/lib/apt/lists/*
COPY --from=jre-build /custom-jre /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk PATH="/opt/java/openjdk/bin:$PATH"
WORKDIR /app
COPY --from=backend /tmp/launchers-standalone-*/lib ./lib
COPY --from=backend /tmp/launchers-standalone-*/conf ./conf
COPY --from=backend /tmp/launchers-standalone-*/bin ./bin
COPY --from=frontend /build/packages/supersonic-fe/supersonic-webapp ./webapp
RUN echo '{"env":""}' > webapp/supersonic.config.json && chmod +x bin/*.sh
EXPOSE 9080
# Use "run" so Java runs in foreground (PID 1) and receives SIGTERM for graceful shutdown.
# Profile (h2/mysql/postgres) is taken from S2_DB_TYPE when not passed.
CMD ["bin/supersonic-daemon.sh", "run", "standalone"]
