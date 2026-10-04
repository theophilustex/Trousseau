# syntax=docker/dockerfile:1
#
# Trousseau on WildFly 41 (Jakarta EE 11) and Java 21, with PostgreSQL. Usually
# built and run through docker-compose.yml; see docs/docker.md.
#
# The runtime is Eclipse Temurin 21 plus the WildFly release tarball, so JDK and
# OS patches arrive with `docker compose build --pull` on Temurin's schedule,
# independent of when a WildFly image happens to be rebuilt.

# Keep WILDFLY_VERSION in step with pom.xml. The checksum is the SHA-256 GitHub
# publishes for the release asset (github.com/wildfly/wildfly/releases).
ARG WILDFLY_VERSION=41.0.1.Final
ARG WILDFLY_SHA256=26e27908f5c720d53f24abb95f9575d04f580510e5d85fad513cda9ade8119c3

# --- 1. Build the WAR ---------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
# The cache mount keeps ~/.m2 between builds without baking it into a layer.
# The PostgreSQL driver is copied out at the version pom.xml pins and installed
# as a WildFly module; the WAR itself does not carry it.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package \
 && mvn -B -q dependency:copy-dependencies \
        -DincludeArtifactIds=postgresql -Dmdep.stripVersion=true -DoutputDirectory=target/jdbc

# --- 2. Install and configure WildFly ----------------------------------------
FROM eclipse-temurin:21-jre AS wildfly
ARG WILDFLY_VERSION
ARG WILDFLY_SHA256
ENV JBOSS_HOME=/opt/wildfly
RUN curl -fsSL -o /tmp/wildfly.tar.gz \
        "https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.tar.gz" \
 && echo "${WILDFLY_SHA256}  /tmp/wildfly.tar.gz" | sha256sum -c - \
 && mkdir -p "$JBOSS_HOME" \
 && tar -xzf /tmp/wildfly.tar.gz -C "$JBOSS_HOME" --strip-components=1 \
 && rm /tmp/wildfly.tar.gz
COPY --from=build /src/target/jdbc/postgresql.jar /tmp/jdbc/postgresql.jar
COPY docker/configure-wildfly.cli docker/smtp-auth.cli ${JBOSS_HOME}/docker/
RUN "$JBOSS_HOME/bin/jboss-cli.sh" --file="$JBOSS_HOME/docker/configure-wildfly.cli" \
 && rm -rf "$JBOSS_HOME/standalone/configuration/standalone_xml_history" \
           "$JBOSS_HOME/standalone/data" "$JBOSS_HOME/standalone/log" "$JBOSS_HOME/standalone/tmp"

# --- 3. Runtime ---------------------------------------------------------------
FROM eclipse-temurin:21-jre
ENV JBOSS_HOME=/opt/wildfly \
    LAUNCH_JBOSS_IN_BACKGROUND=true
RUN groupadd --system wildfly \
 && useradd --system --gid wildfly --home-dir "$JBOSS_HOME" --no-create-home wildfly
COPY --from=wildfly --chown=wildfly:wildfly /opt/wildfly /opt/wildfly
COPY --from=build --chown=wildfly:wildfly /src/target/trousseau.war /opt/wildfly/standalone/deployments/
COPY --chmod=755 docker/entrypoint.sh /usr/local/bin/trousseau-entrypoint

USER wildfly
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
    CMD curl -fsS -o /dev/null http://localhost:8080/trousseau/login.xhtml || exit 1
ENTRYPOINT ["trousseau-entrypoint"]
