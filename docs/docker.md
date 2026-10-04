# Running with Docker Compose

The simplest way to self-host Trousseau. One command starts the app (WildFly 26.1.3 on Eclipse Temurin 11) and a PostgreSQL 16 database. All data, photos included, is kept in a Docker volume.

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Day-to-day operations](#day-to-day-operations)
- [Email](#email)
- [Behind a reverse proxy](#behind-a-reverse-proxy)
- [How the image is built](#how-the-image-is-built)
- [Troubleshooting](#troubleshooting)

---

## Quick start

Requirements: Docker Engine 23+ with the Compose plugin. Java and Maven are not needed on the host; the image builds the app itself.

```bash
cp .env.example .env
# edit .env and set DB_PASSWORD (and TZ)
docker compose up -d --build
```

The first build takes a few minutes (it downloads Maven dependencies and WildFly). After that, open **http://localhost:8473/trousseau/** and register an account.

`docker compose ps` shows the app as `healthy` once it is serving pages, usually about 10 seconds after the database is ready.

---

## Configuration

All settings go in `.env` next to `docker-compose.yml`. `.env` is git-ignored because it holds the database password. [`.env.example`](../.env.example) lists every variable.

| Variable | Default | Purpose |
|---|---|---|
| `DB_PASSWORD` | *(required)* | PostgreSQL password. Used to create the database on first start, and by the app to connect. |
| `DB_NAME`, `DB_USER` | `trousseau` | Database name and user |
| `TROUSSEAU_PORT` | `8473` | Host port. The app is at `http://<host>:<port>/trousseau/`. |
| `TZ` | `UTC` | Timezone for the Sunday 17:00 email and for "today" in the app (e.g. `Europe/London`) |
| `JAVA_MAX_HEAP` | `1g` | Maximum JVM heap. Data imports are read into memory, so raise this for large wardrobes. |
| `MAX_UPLOAD_BYTES` | `268435456` (256 MB) | Largest accepted HTTP request. Applies to photo uploads and data imports. |
| `BEHIND_PROXY` | `false` | Trust `X-Forwarded-*` headers. See [Behind a reverse proxy](#behind-a-reverse-proxy). |
| `SMTP_HOST`, `SMTP_PORT` | `localhost`, `25` | SMTP server for the weekly email |
| `SMTP_STARTTLS`, `SMTP_SSL` | `false` | Use STARTTLS (typically port 587) or implicit TLS (typically port 465) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | *(unset)* | SMTP login. Leave `SMTP_USERNAME` unset for relays that need no authentication. |
| `SMTP_FROM` | `noreply@trousseau.app` | Sender address. Use one your SMTP provider accepts. |
| `MAILPIT_PORT` | `8025` | Web UI port of the optional Mailpit test mail catcher |

After changing `.env`, run `docker compose up -d`. Compose recreates the containers whose settings changed.

> **Changing `DB_PASSWORD` later does not change the database password.** PostgreSQL only reads it when the volume is first created. To change it, run `ALTER USER trousseau PASSWORD '…'` in the database (see [Operations](#day-to-day-operations)) **and** update `.env`.

The port is published on all host interfaces, so other devices on your network can reach the app. Before exposing it further, read [Security](security.md). To restrict it to the host only, change the port mapping in `docker-compose.yml` to `"127.0.0.1:${TROUSSEAU_PORT:-8473}:8080"`.

---

## Day-to-day operations

| Task | Command |
|---|---|
| Start / apply `.env` changes | `docker compose up -d` |
| Stop (keeps data) | `docker compose down` |
| Status and health | `docker compose ps` |
| Application logs | `docker compose logs -f app` |
| Update after pulling new code | `docker compose up -d --build` |
| Rebuild with the latest JDK and OS patches | `docker compose build --pull && docker compose up -d` |
| Open a SQL shell | `docker compose exec db psql -U trousseau trousseau` |
| WildFly management CLI | `docker compose exec app /opt/wildfly/bin/jboss-cli.sh --connect` |
| **Delete everything, including all data** | `docker compose down -v` |

### Backups

Everything lives in PostgreSQL, so one dump is a complete backup:

```bash
# back up
docker compose exec -T db pg_dump -U trousseau -Fc trousseau > trousseau-$(date +%F).dump

# restore into an empty stack
docker compose up -d db
docker compose exec -T db pg_restore -U trousseau -d trousseau --clean --if-exists < trousseau-2026-10-04.dump
docker compose up -d
```

Users can also take their own portable backups from **Data → Download Export**.

### Upgrades

`docker compose up -d --build` replaces the app container. The database volume is untouched, and new tables and columns are added automatically on start (see [Setup → Upgrading](setup-and-deployment.md#upgrading)). Take a backup first.

Moving PostgreSQL to a new major version (for example, from 16 to 17) needs a dump and restore: back up, `docker compose down -v`, change the `db` image tag, start, and restore.

---

## Email

Every Sunday at 17:00 (in `TZ`), each user with an email address is sent next week's outfit plan. A typical provider setup:

```ini
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_STARTTLS=true
SMTP_USERNAME=trousseau@example.com
SMTP_PASSWORD=app-password
SMTP_FROM=trousseau@example.com
```

Without SMTP settings, the app still works. Each Sunday's sends fail and are logged as warnings.

### Testing email with Mailpit

The compose file includes an optional [Mailpit](https://mailpit.axllent.org) service that accepts all mail and shows it in a web UI:

```ini
# .env
SMTP_HOST=mailpit
SMTP_PORT=1025
```

```bash
docker compose --profile mailpit up -d
```

Captured mail appears at http://localhost:8025.

To send the weekly email immediately, instead of waiting for Sunday, trigger the timer:

```bash
docker compose exec app /opt/wildfly/bin/jboss-cli.sh --connect \
  --command="/deployment=trousseau.war/subsystem=ejb3/singleton-bean=WeeklyPlannerEmailScheduler/service=timer-service:read-children-names(child-type=timer)"
# copy the id from the output, then:
docker compose exec app /opt/wildfly/bin/jboss-cli.sh --connect \
  --command="/deployment=trousseau.war/subsystem=ejb3/singleton-bean=WeeklyPlannerEmailScheduler/service=timer-service/timer=<id>:trigger"
```

This sends real email to every user with an address. Only do it against your real SMTP server if you mean to.

---

## Behind a reverse proxy

For HTTPS or a custom domain, put a reverse proxy (Caddy, nginx, Traefik) in front of the published port (`TROUSSEAU_PORT`, default 8473) and set:

```ini
BEHIND_PROXY=true
```

The proxy must send `X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-Port`. It must also allow request bodies as large as `MAX_UPLOAD_BYTES` (nginx: `client_max_body_size 256m;`).

Leave `BEHIND_PROXY=false` when there is no proxy. Otherwise any client could set those headers and control the host name in password-reset links.

---

## How the image is built

The [`Dockerfile`](../Dockerfile) has three stages:

1. **build** (`maven:3.9-eclipse-temurin-11`): runs `mvn package` and copies out the PostgreSQL JDBC driver at the version `pom.xml` pins. A BuildKit cache mount keeps `~/.m2` between builds.
2. **wildfly** (`eclipse-temurin:11-jre`): downloads the WildFly 26.1.3 release tarball, verifies its SHA-1, and runs [`docker/configure-wildfly.cli`](../docker/configure-wildfly.cli) against an embedded server. That script:
   - installs the PostgreSQL driver as a module and creates the `TrousseauDS` datasource
   - points `java:comp/DefaultDataSource` at it, and removes the in-memory `ExampleDS` so a misconfiguration fails loudly
   - wires the mail session, upload limit and proxy handling to the environment variables above
3. **runtime** (`eclipse-temurin:11-jre`): the configured WildFly plus `trousseau.war`. It runs as the unprivileged `wildfly` user and has a health check on `/trousseau/login.xhtml`.

Settings are stored in `standalone.xml` as **expressions** like `${env.DB_HOST:db}`, which WildFly resolves from the environment at each start. The image therefore contains no credentials, and one image works for any deployment.

The one exception is SMTP authentication. WildFly always attempts SMTP AUTH when a username is configured, even an empty one, which would break relays that need no login. So [`docker/entrypoint.sh`](../docker/entrypoint.sh) adds the username and password (again as expressions) at container start, and only when `SMTP_USERNAME` is set.

**Why not the official `quay.io/wildfly/wildfly:26.1.3.Final` image?** That image is no longer rebuilt. It runs CentOS 7, which reached end of life in June 2024, and a JDK from October 2022. Building on `eclipse-temurin:11-jre` means `docker compose build --pull` brings in current Java 11 and Ubuntu security updates.

### Building without Compose

```bash
docker build -t trousseau .
docker run -d -p 8473:8080 -e DB_HOST=… -e DB_PASSWORD=… trousseau
```

Point it at any PostgreSQL server you already run.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `required variable DB_PASSWORD is missing a value` | Create `.env` from `.env.example` and set `DB_PASSWORD`. |
| App restarts or stays `starting`, logs show `password authentication failed` | `DB_PASSWORD` in `.env` doesn't match the password the volume was created with. See the note under [Configuration](#configuration). |
| Upload or import fails with a connection reset or 413 | The request is larger than `MAX_UPLOAD_BYTES`, or than your reverse proxy's body limit. |
| Large import fails with `OutOfMemoryError` | Raise `JAVA_MAX_HEAP`. |
| Weekly email not arriving | `docker compose logs app | grep -i "planner email"` shows the result of each run and any SMTP error. Try the [Mailpit test](#testing-email-with-mailpit). |
| Email sent at the wrong hour | Set `TZ`. |
| Password-reset links point at `localhost` or `http://` behind a proxy | Set `BEHIND_PROXY=true` and have the proxy send `X-Forwarded-*` headers. |
