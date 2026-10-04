#!/bin/sh
# Container entrypoint: applies optional runtime configuration, then starts
# WildFly in the foreground.
set -e

if [ -n "${SMTP_USERNAME:-}" ]; then
    echo "trousseau: SMTP_USERNAME is set; enabling SMTP authentication"
    "$JBOSS_HOME/bin/jboss-cli.sh" --file="$JBOSS_HOME/docker/smtp-auth.cli"
fi

exec "$JBOSS_HOME/bin/standalone.sh" -b 0.0.0.0 "$@"
