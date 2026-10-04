# Trousseau

**Your smart wardrobe manager.** Trousseau is a self-hosted web app for cataloguing the clothes you own, putting them together into outfits, planning what to wear each week, and learning from what you actually wear.

It is a Java EE 8 / JSF application (PrimeFaces UI, JPA/Hibernate persistence) packaged as a WAR for WildFly. It runs on a LAN with no internet access; the only outbound call it makes is an optional weather lookup.

## Features

| Area | What it does |
|---|---|
| **Wardrobe** | Catalogue items with photo, category, colour, brand, size and tags. Filter by category or tag, or search by name. Add one item at a time, or drop in up to 40 photos at once with **Bulk add**. |
| **Wear & wash tracking** | Record each wear. Every item has a "wash after N wears" threshold. Items that reach it are flagged and collected in the **Laundry basket**, where you can wash them all with one click. |
| **Outfits** | Combine items into outfits tagged with an occasion and seasons. Log the days you wear them (which also counts a wear on every item in the outfit). |
| **Weekly planner** | Suggests an outfit for each day of the week, favouring clean clothes, colour variety and, if you set a location, the weather forecast. You can override any day. The plan is saved, and emailed to you every Sunday. |
| **Ratings, comments & sharing** | Share items or outfits with other users. Outfits can be rated 1–5 from three perspectives (Personal, Spouse/Partner, Friends/Other) and commented on. |
| **Insights** | Most and least worn items, items gathering dust, wears by category, monthly activity, and cost per wear from purchase prices. |
| **Lifecycle** | Retire items as Archived, Donated or Sold. They leave the wardrobe but keep their history. Attach a purchase receipt (image or PDF). |
| **Data export/import** | Download your whole wardrobe as one JSON file, photos included, and import it into any Trousseau instance. |
| **Dark mode** | Toggle in the nav bar. The choice is stored in the browser. |

## Quick start

Prerequisites: **JDK 11 or newer** and **Maven 3.6+**.

```bash
mvn clean package wildfly:run
```

The first run downloads WildFly 26.1.3 (about 200 MB). It configures the server's default datasource to use an H2 database file at `~/.trousseau/trousseau.mv.db`, then deploys the app.

Open **http://localhost:8080/trousseau/**, register an account and sign in.

Stop the server with `Ctrl+C`. Your data stays in `~/.trousseau/` and is still there on the next start.

Production deployment, PostgreSQL, email and weather setup are covered in [Setup & deployment](docs/setup-and-deployment.md).

## Documentation

| Document | Audience | Contents |
|---|---|---|
| [User guide](docs/user-guide.md) | People using the app | Every page and feature, step by step |
| [Setup & deployment](docs/setup-and-deployment.md) | Self-hosters / operators | Running locally, WildFly, PostgreSQL, SMTP, weather, configuration reference |
| [How it works](docs/how-it-works.md) | Everyone curious | Planner scoring, wash logic, cost per wear, insights definitions |
| [Architecture](docs/architecture.md) | Developers | Layers, packages, request flow, key design decisions |
| [Data model](docs/data-model.md) | Developers | Entities, relationships, schema management |
| [Export format](docs/export-format.md) | Developers / power users | The JSON export/import file format |
| [Security](docs/security.md) | Operators / developers | Auth model, known security issues, hardening checklist |
| [Development](docs/development.md) | Contributors | Conventions, adding features, known bugs |

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | WildFly 26.1.3 (Java EE 8), Java 11 bytecode |
| UI | JSF 2.3 (Facelets), PrimeFaces 12 (Saga theme), custom CSS |
| Business logic | EJB 3.2 (`@Stateless`, `@Singleton`, `@Schedule`), CDI 2.0 |
| Persistence | JPA 2.2 with Hibernate 5.6 (bundled in the WAR), schema managed by `hbm2ddl=update` |
| Database | H2 (local/dev) or PostgreSQL (production). The dialect is auto-detected. |
| Other | jBCrypt (password hashing), JavaMail (weekly email), Open-Meteo (forecast), Lombok |

## Project layout

```
pom.xml
src/main/
├── java/com/trousseau/
│   ├── bean/         JSF backing beans (one per page, plus SessionBean & ImageStreamer)
│   ├── converter/    JSF converters for ClothingItem and Tag
│   ├── dao/          JPA data-access objects
│   ├── filter/       AuthFilter (login gate)
│   ├── model/        JPA entities, enums and read-only stat projections
│   ├── scheduler/    Sunday weekly-plan email job
│   ├── service/      EJB business services
│   └── util/         Password hashing, thumbnail generation
├── resources/
│   ├── META-INF/persistence.xml
│   └── messages.properties
├── scripts/configure-ds.cli   WildFly CLI script used by `mvn wildfly:run`
└── webapp/
    ├── *.xhtml                One Facelets page per screen
    ├── templates/layout.xhtml Shared nav bar, growl and footer
    ├── resources/css/trousseau.css
    └── WEB-INF/               web.xml, faces-config.xml, beans.xml, jboss-deployment-structure.xml
```

## Status and known issues

Trousseau is at `1.0.0-SNAPSHOT`. There is no automated test suite. Before exposing an instance to people you don't trust, read [Security → Known issues](docs/security.md#known-issues). In particular, the password-reset flow and the item detail pages have authorization gaps. Functional bugs are listed in [Development → Known bugs](docs/development.md#known-bugs).

## License

The app footer states **Apache License 2.0**, but this repository does not yet include a `LICENSE` file. Add one before distributing the project.
