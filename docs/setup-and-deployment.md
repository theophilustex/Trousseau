# Setup & deployment

This guide covers running Trousseau locally, deploying it to a WildFly server, and configuring the database, email and weather integrations.

> **Just want to run it?** [Docker Compose](docker.md) does everything on this page (WildFly, PostgreSQL, email, upload limits, proxy handling) from a single `.env` file. Read on to install onto your own WildFly or to develop locally.

- [Docker Compose](docker.md) (separate page)
- [Requirements](#requirements)
- [Running locally](#running-locally)
- [Deploying to an existing WildFly](#deploying-to-an-existing-wildfly)
- [Using PostgreSQL](#using-postgresql)
- [Weekly email (SMTP)](#weekly-email-smtp)
- [Password reset emails](#password-reset-emails)
- [Weather forecasts](#weather-forecasts)
- [Running behind a reverse proxy](#running-behind-a-reverse-proxy)
- [Configuration reference](#configuration-reference)
- [Backups](#backups)
- [Upgrading](#upgrading)

---

## Requirements

| Component | Version | Notes |
|---|---|---|
| JDK | 21 or newer (a full JDK, with `javac`, to build) | The build targets Java 21 with `maven.compiler.release=21`, so a newer JDK cannot accidentally pull in post-21 APIs. WildFly 41 runs on Java 21 and 25. |
| Maven | 3.6+ | |
| Application server | WildFly 41.0.x (Jakarta EE 11) | WildFly provides Hibernate ORM 7, Jakarta Mail and Jakarta JSON; the WAR carries only PrimeFaces and jBCrypt. Another Jakarta EE 11 server would need equivalents of the WildFly mail and datasource setup. |
| Database | H2 2.x (bundled with WildFly) or PostgreSQL | Any database Hibernate 7 supports should work. Only H2 and PostgreSQL are considered in the code. |

Network access is **not** required at runtime. The app makes only these optional external connections:

- **Server → `api.open-meteo.com:443`** for weather, only for users who set a location.
- **Server → your SMTP server** for the Sunday email.
- **Browser → `fonts.googleapis.com`** for the Inter web font. Without it, pages fall back to the system sans-serif font.

---

## Running locally

```bash
mvn clean package wildfly:run
```

The `wildfly-maven-plugin` then:

1. Downloads and provisions **WildFly 41.0.1.Final** into `target/server` (first run after a `clean`, ~270 MB).
2. Runs [`src/main/scripts/configure-ds.cli`](../src/main/scripts/configure-ds.cli). This raises the request limit to 256 MB, as in Docker, and repoints WildFly's built-in `ExampleDS` datasource from an in-memory H2 database to a file:
   ```
   jdbc:h2:file:~/.trousseau/trousseau;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
   ```
3. Starts the server and deploys `target/trousseau.war`.

The app is at **http://localhost:8080/trousseau/**. The WildFly management console is at http://localhost:9990 (it needs a management user, created with WildFly's `add-user.sh`).

On first start Hibernate creates all tables. No seed data is loaded, so register an account to begin.

| Task | How |
|---|---|
| Stop | `Ctrl+C` |
| Reset all data | Stop the server, delete `~/.trousseau/` |
| Inspect the database | `AUTO_SERVER=TRUE` lets a second process open the file while the app is running, e.g. the H2 console or any JDBC tool with the URL above, user `sa`, password `sa` (WildFly's `ExampleDS` defaults). |
| See SQL | Set `hibernate.show_sql` to `true` in `persistence.xml`. |

> `mvn clean` deletes `target/`, including the downloaded WildFly, so the next run downloads it again. Your data in `~/.trousseau/` is not affected.

> **Upgrading from the Java EE 8 version?** Run `mvn clean` once. A `target/` left over from the old build still holds a deleted `jboss-deployment-structure.xml`, which hides WildFly's Hibernate from the app, and deployment fails with `ClassNotFoundException: org.hibernate.proxy.HibernateProxy`. Also, WildFly 41's H2 2.x cannot open database files written by the H2 1.4 in WildFly 26. Export your data from the old version (**Data → Download Export**) before upgrading, then import it into the new one.

---

## Deploying to an existing WildFly

1. Build the WAR:
   ```bash
   mvn clean package
   ```
   This produces `target/trousseau.war`.
2. Make sure the server's **default datasource** points to the database you want. The app uses `java:comp/DefaultDataSource`, which WildFly maps through `/subsystem=ee/service=default-bindings`. Out of the box this is the in-memory `ExampleDS`, and **all data is lost on restart**. Either run `configure-ds.cli` against the server, or set up PostgreSQL as below.
3. Deploy:
   ```bash
   $JBOSS_HOME/bin/jboss-cli.sh --connect --command="deploy target/trousseau.war --force"
   ```
   or copy the WAR into `$JBOSS_HOME/standalone/deployments/`.

### Raise the upload limit

WildFly's HTTP listener rejects requests over **10 MB** by default. That is below bulk add's 20 MB per-photo limit, and far below a data import with embedded photos. Raise it:

```
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=max-post-size,value=268435456)
```

(Also apply it to `https-listener=https` if you serve HTTPS from WildFly directly.)

### Context path

The context path comes from the WAR name: `trousseau.war` is served at `/trousseau`. To serve the app at `/`, deploy it as `ROOT.war`, or set `<context-root>` in a `jboss-web.xml`.

### Hibernate comes from WildFly

The WAR does not bundle Hibernate; it uses the version WildFly 41 provides (ORM 7.4). Upgrading WildFly therefore upgrades Hibernate too. WildFly runs Hibernate in strict Jakarta Persistence compliance mode, so JPQL must follow the specification. For example, an alias on a `JOIN FETCH` is rejected at deployment.

---

## Using PostgreSQL

`persistence.xml` deliberately has no `hibernate.dialect`, so Hibernate detects the database from the JDBC connection and the same WAR works on H2 and PostgreSQL.

1. Create an empty database and user:
   ```sql
   CREATE USER trousseau WITH PASSWORD 'change-me';
   CREATE DATABASE trousseau OWNER trousseau;
   ```
2. Install the driver and a datasource in WildFly, then make it the default (run in `jboss-cli.sh --connect`):
   ```
   deploy ~/.m2/repository/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.jar

   data-source add --name=TrousseauDS \
       --jndi-name=java:jboss/datasources/TrousseauDS \
       --driver-name=postgresql-42.7.13.jar \
       --connection-url=jdbc:postgresql://db-host:5432/trousseau \
       --user-name=trousseau --password=change-me \
       --validate-on-match=true \
       --valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker

   /subsystem=ee/service=default-bindings:write-attribute(name=datasource,value=java:jboss/datasources/TrousseauDS)
   reload
   ```
3. Deploy the WAR. Tables are created on first start.

The PostgreSQL driver is also bundled in the WAR, but WildFly datasources are defined at server level and cannot load drivers from inside an application, which is why step 2 installs it separately.

### Moving data from H2 to PostgreSQL

There is no database-level migration tool. The supported route is per user: each user exports their data on the old instance (**Data → Download Export**) and imports it on the new one. Ratings, comments, shares and saved planner weeks are not carried over (see [Export format](export-format.md)).

---

## Weekly email (SMTP)

`WeeklyPlannerEmailScheduler` runs **every Sunday at 17:00, server local time**. For each user with an email address it gets (or generates and saves) the plan for the week starting the next day, and sends it as an HTML email.

It uses the mail session at JNDI name `java:jboss/mail/Default`. WildFly's default configuration already defines this session, sending through the `mail-smtp` socket binding to `localhost:25`. Point it at your SMTP server:

```
/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-smtp:write-attribute(name=host,value=smtp.example.com)
/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-smtp:write-attribute(name=port,value=587)
/subsystem=mail/mail-session=default/server=smtp:write-attribute(name=username,value=mailer@example.com)
/subsystem=mail/mail-session=default/server=smtp:write-attribute(name=password,value=app-password)
/subsystem=mail/mail-session=default/server=smtp:write-attribute(name=tls,value=true)
reload
```

Things to know:

- **Sender address.** Emails come from the mail session's `from` attribute, or `noreply@trousseau.app` if it is unset. Many SMTP providers reject senders outside your own domain, so set it:
  ```
  /subsystem=mail/mail-session=default:write-attribute(name=from,value=trousseau@example.com)
  ```
- **Missed runs are skipped.** The timer is non-persistent. If the server is down at 17:00 on Sunday, that week's emails are not sent later.
- **Failures are per user.** A failed send is logged as a `WARNING` and the job moves on to the next user. Each run ends with an `INFO` summary: `Weekly planner emails: sent=N, skipped (no email)=M`.
- **Side effect.** The run saves a plan for next week for every user with an email, so their planner shows that plan when they next open it.
- **No opt-out.** Every user with an email address gets the email. Removing the email address in Profile is the only way to stop it.
- **Timezone.** Set the JVM timezone (e.g. `-Duser.timezone=Europe/London` in `standalone.conf`) if the server's OS timezone isn't the one your users live in.

---

## Password reset emails

**Forgot your password?** emails a single-use link, using the same mail session as the weekly email. It stays switched off, and the page says so, until the app knows its own public address. That address is never taken from the request, because the `Host` header is client-controlled and a forged one would send the victim a link to someone else's server.

Set it as an environment variable of the WildFly process, or as a system property:

```bash
# environment (e.g. in the service unit, or before standalone.sh)
export TROUSSEAU_BASE_URL=https://wardrobe.example.com/trousseau

# or a system property, e.g. in standalone.conf
JAVA_OPTS="$JAVA_OPTS -Dtrousseau.base-url=https://wardrobe.example.com/trousseau"
```

The system property wins if both are set. Users without an email address on their account cannot reset their own password. See [Security → Password reset](security.md#password-reset) for how the flow is protected.

---

## Weather forecasts

Weather-aware planning is per user and off by default. A user turns it on by entering coordinates in **Profile → Location**. The server then calls:

```
https://api.open-meteo.com/v1/forecast?latitude=…&longitude=…
    &daily=temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto&forecast_days=16
```

- No API key or account is needed.
- Responses are cached in memory for **60 minutes** per location (rounded to 3 decimal places), up to 200 locations.
- Requests time out after **6 seconds**. Failures are cached for the same 60 minutes, so an offline server doesn't retry on every page load. Failures are logged at `FINE` level only.
- If the server can't reach the internet, planning silently falls back to scoring without weather. Nothing else is affected.
- The client is `java.net.http.HttpClient` with the JVM's default proxy selector, so the standard `https.proxyHost` / `https.proxyPort` system properties apply if you need an outbound proxy.

---

## Running behind a reverse proxy

Behind a TLS-terminating proxy, enable forwarded-header handling so the redirects WildFly sends (after login, for example) point at `https://your-host/…` and not `http://127.0.0.1:8080/…`:

```
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=proxy-address-forwarding,value=true)
```

Then have the proxy send `X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-Port`. Everything else uses relative URLs.

Allow uploads through the proxy up to WildFly's `max-post-size` (e.g. nginx `client_max_body_size 256m;`). Bulk add accepts photos up to 20 MB, and a data import can be much larger because photos are embedded.

---

## Configuration reference

Trousseau has no external configuration file. Settings live in the deployment descriptors and, for tuning values, in code constants.

### Deployment descriptors

| Setting | Where | Default | Notes |
|---|---|---|---|
| Faces project stage | `web.xml` → `jakarta.faces.PROJECT_STAGE` | `Development` | Set to **`Production`** for real deployments. Development mode shows detailed error pages and stack traces. |
| Session timeout | `web.xml` → `session-timeout` | 30 minutes | |
| PrimeFaces theme | `web.xml` → `primefaces.THEME` | `saga` | One of the themes built into PrimeFaces 16 (e.g. `saga`, `vela`, `arya`) |
| Datasource | `persistence.xml` → `jta-data-source` | `java:comp/DefaultDataSource` | Change to a specific JNDI name to avoid relying on the server default. |
| Schema management | `persistence.xml` → `hibernate.hbm2ddl.auto` | `update` | See [Data model → Schema management](data-model.md#schema-management). |
| SQL logging | `persistence.xml` → `hibernate.show_sql` | `false` | |
| WildFly version | `pom.xml` → `wildfly.version`, and `Dockerfile` → `WILDFLY_VERSION` / `WILDFLY_SHA256` | `41.0.1.Final` | Keep the two in step |
| Public base URL | `TROUSSEAU_BASE_URL` env var or `trousseau.base-url` system property | unset (password reset off) | See [Password reset emails](#password-reset-emails) |
| Orphaned large-object sweep | `TROUSSEAU_LARGE_OBJECT_SWEEP` env var or `trousseau.large-object-sweep` system property | on | PostgreSQL only. Set `false` if the database user is shared with other software that stores large objects. See [Docker → Disk space](docker.md#disk-space). |
| Hibernate version for build-time enhancement | `pom.xml` → `hibernate.version` | `7.4.5.Final` | Must equal the Hibernate ORM bundled in WildFly |
| Max request size | Undertow `http-listener` → `max-post-size` | 10 MB (WildFly default) | Raise it; see [Raise the upload limit](#raise-the-upload-limit). The Docker image and `mvn wildfly:run` set 256 MB. |

### Code constants

| Value | Default | Location |
|---|---|---|
| Password hash cost (bcrypt rounds) | 12 | `util/PasswordUtil` |
| Password-reset link lifetime | 60 minutes | `UserService.RESET_TOKEN_TTL_MINUTES` |
| Password-reset resend interval | 2 minutes per account | `PasswordResetService.RESEND_INTERVAL_MINUTES` |
| Thumbnail longest edge | 400 px (JPEG) | `ImageUtil.THUMBNAIL_MAX_EDGE` |
| Bulk-add queue limit | 40 photos | `BulkAddBean.MAX_DRAFTS` |
| Bulk-add per-file size limit | 20 MB | `bulk-add.xhtml` → `sizeLimit` |
| Default wash threshold | 3 wears | `ClothingItem.washAfterWears` |
| "Gathering dust" cut-off | 90 days | `InsightsService.DORMANT_AFTER_DAYS` |
| Insights list length / activity window | 10 rows / 6 months | `InsightsService` |
| Weather cache TTL / timeout / max locations | 60 min / 6 s / 200 | `WeatherService` |
| Weekly email schedule | Sunday 17:00:00 | `@Schedule` on `WeeklyPlannerEmailScheduler` |
| Email sender fallback | `noreply@trousseau.app` | `WeeklyPlannerEmailScheduler.DEFAULT_FROM`, used when the mail session has no `from` |
| Item categories | 12 fixed values | `ClothingItemService.getCategories()` |
| Outfit occasions | 7 fixed values | `outfits.xhtml` |

Before exposing an instance beyond a trusted network, also work through the [security hardening checklist](security.md#hardening-checklist).

---

## Backups

All state is in the database, including photos and receipts, which are stored as BLOBs.

| Database | Backup method |
|---|---|
| H2 | Stop the server and copy `~/.trousseau/trousseau.mv.db`. Or, while running, use H2's `BACKUP TO 'file.zip'` SQL command over a JDBC connection. |
| PostgreSQL | `pg_dump -Fc trousseau > trousseau.dump` (`pg_dump` includes large objects, where photos live) |

Users can also take their own backups with **Data → Download Export**. This is portable across instances but does not include ratings, comments, shares or planner weeks.

---

## Upgrading

1. **Back up the database** (see above).
2. Deploy the new WAR.
3. On startup, `hbm2ddl=update` adds any new tables and columns. It never drops or alters existing ones.

The entities are written so that rows from older versions still load after new columns are added. For example, a `NULL` `status` reads as `ACTIVE` and a `NULL` `wears_since_wash` reads as 0. Thumbnails for photos uploaded before thumbnails existed are generated the first time each one is displayed.

`hbm2ddl=update` cannot rename or drop columns, change types or migrate data. A release that needs any of those must ship a manual SQL migration.

### Orphaned photo data (PostgreSQL)

Older versions left a copy of an item's photo, thumbnail and receipt behind in PostgreSQL every time the item was saved. On first start, the app deletes these orphans (see [Docker → Disk space](docker.md#disk-space)), including how to shrink the database files afterwards. Back up before upgrading in any case.

### From the Java EE 8 version (WildFly 26) to Jakarta EE 11 (WildFly 41)

- **PostgreSQL (Docker):** no action needed beyond a backup. This upgrade was tested against a database written by the old version: all users, passwords, items, photos and receipts (PostgreSQL large objects), outfits, shares, ratings, comments, wear logs and planned weeks read back correctly. Hibernate 7 adds one constraint, `UNIQUE (outfit_id, season)` on `outfit_seasons`, which existing data already satisfies. Everyone is signed out once.
- **H2:** the file format changed between H2 1.4 and 2.x. Export before upgrading and import afterwards (see above).
- **Builds:** run `mvn clean` once (see above).
