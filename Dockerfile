# syntax=docker/dockerfile:1
#
# Trousseau on WildFly 26.1.3 (Java EE 8) with PostgreSQL. Usually built and run
# through docker-compose.yml; see docs/docker.md.
#
# The runtime is Eclipse Temurin 11 plus the WildFly release tarball, rather
# than the official WildFly 26 image: that image is no longer rebuilt and is
# stuck on CentOS 7 and a 2022 JDK. Rebuilding this one with --pull picks up
# current JDK 11 and OS patches.

ARG WILDFLY_VERSION=26.1.3.Final
ARG WILDFLY_SHA1=b9f52ba41df890e09bb141d72947d2510caf758c

# --- 1. Build the WAR ---------------------------------------------------------
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
# The cache mount keeps ~/.m2 between builds without baking it into a layer.
# The PostgreSQL driver is copied out at the version pom.xml pins, so the
# server's driver module and the WAR's copy cannot drift apart.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package \
 && mvn -B -q dependency:copy-dependencies \
        -DincludeArtifactIds=postgresql -Dmdep.stripVersion=true -DoutputDirectory=target/jdbc

# --- 2. Install and configure WildFly ----------------------------------------
FROM eclipse-temurin:11-jre AS wildfly
ARG WILDFLY_VERSION
ARG WILDFLY_SHA1
ENV JBOSS_HOME=/opt/wildfly
RUN curl -fsSL -o /tmp/wildfly.tar.gz \
        "https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.tar.gz" \
 && echo "${WILDFLY_SHA1}  /tmp/wildfly.tar.gz" | sha1sum -c - \
 && mkdir -p "$JBOSS_HOME" \
 && tar -xzf /tmp/wildfly.tar.gz -C "$JBOSS_HOME" --strip-components=1 \
 && rm /tmp/wildfly.tar.gz
COPY --from=build /src/target/jdbc/postgresql.jar /tmp/jdbc/postgresql.jar
COPY docker/configure-wildfly.cli docker/smtp-auth.cli ${JBOSS_HOME}/docker/
RUN "$JBOSS_HOME/bin/jboss-cli.sh" --file="$JBOSS_HOME/docker/configure-wildfly.cli" \
 && rm -rf "$JBOSS_HOME/standalone/configuration/standalone_xml_history" \
           "$JBOSS_HOME/standalone/data" "$JBOSS_HOME/standalone/log" "$JBOSS_HOME/standalone/tmp"

# --- 3. Runtime ---------------------------------------------------------------
FROM eclipse-temurin:11-jre
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
