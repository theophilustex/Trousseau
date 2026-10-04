# Architecture

Trousseau is a classic server-rendered Java EE 8 application: one WAR, JSF pages backed by CDI beans, a layer of stateless EJB services, and JPA DAOs over a single relational database.

- [Layers](#layers)
- [Packages](#packages)
- [Request lifecycle](#request-lifecycle)
- [Authentication and session state](#authentication-and-session-state)
- [Pages and their beans](#pages-and-their-beans)
- [Services](#services)
- [Background jobs](#background-jobs)
- [Serving images](#serving-images)
- [Key design decisions](#key-design-decisions)

---

## Layers

```mermaid
flowchart TD
    Browser -->|HTTP / JSF postback / AJAX| AuthFilter
    AuthFilter --> FacesServlet
    FacesServlet --> XHTML["Facelets pages (*.xhtml)<br/>templates/layout.xhtml"]
    XHTML -->|EL #{...}| Beans["Backing beans<br/>com.trousseau.bean<br/>@ViewScoped / @SessionScoped / @RequestScoped"]
    Beans --> Services["EJB services<br/>com.trousseau.service<br/>@Stateless (transactional)"]
    Scheduler["WeeklyPlannerEmailScheduler<br/>@Singleton @Schedule"] --> Services
    Services --> DAOs["DAOs<br/>com.trousseau.dao<br/>@ApplicationScoped + EntityManager"]
    Services --> Weather["WeatherService<br/>@Singleton"] -->|HTTPS| OpenMeteo[(Open-Meteo)]
    Scheduler -->|JavaMail| SMTP[(SMTP)]
    DAOs -->|JPA / Hibernate 5.6| DB[(H2 or PostgreSQL<br/>java:comp/DefaultDataSource)]
```

| Layer | Responsibility | Rules |
|---|---|---|
| **Pages** (`webapp/*.xhtml`) | Markup, PrimeFaces components, EL bindings | All pages use `templates/layout.xhtml` |
| **Beans** (`bean/`) | Page state, user actions, growl messages | Get the current user from `SessionBean`. Call services, never DAOs. |
| **Services** (`service/`) | Business rules, transactions | `@Stateless` EJBs, so each public method runs in a JTA transaction. May call other services and any DAO. |
| **DAOs** (`dao/`) | JPQL queries, persist/merge/remove | `@ApplicationScoped` CDI beans with an injected `EntityManager`. They take part in the caller's transaction. |
| **Model** (`model/`) | JPA entities, enums, read-only projections | Lombok getters/setters. Named queries live on the entity. |

---

## Packages

| Package | Contents |
|---|---|
| `bean` | One backing bean per page, plus `SessionBean` (logged-in user) and `ImageStreamer` (photo endpoint) |
| `converter` | `clothingItemConverter`, `tagConverter`: JSF converters between entity and id string, for select components |
| `dao` | `UserDao`, `ClothingItemDao`, `TagDao`, `OutfitDao`, `RatingDao`, `CommentDao`, `ShareDao`, `OutfitWearLogDao`, `PlannedOutfitDao` |
| `filter` | `AuthFilter`: redirects unauthenticated requests to the login page |
| `model` | **Entities**: `User`, `ClothingItem`, `Tag`, `Outfit`, `Rating`, `Comment`, `Share`, `OutfitWearLog`, `PlannedOutfit`<br/>**Enums**: `ItemStatus`, `RatingType`<br/>**Projections / value objects**: `ClothingItemSummary`, `ItemWearStat`, `CategoryWearStat`, `OutfitWearStat`, `MonthlyWearStat`, `DayForecast` |
| `scheduler` | `WeeklyPlannerEmailScheduler` |
| `service` | `UserService`, `ClothingItemService`, `TagService`, `OutfitService`, `OutfitWearLogService`, `RatingService`, `CommentService`, `ShareService`, `InsightsService`, `WeeklyPlannerService`, `WeatherService`, `DataExportImportService` |
| `util` | `PasswordUtil` (bcrypt), `ImageUtil` (thumbnails) |

The full entity model is in [Data model](data-model.md).

---

## Request lifecycle

1. **`AuthFilter`** (`@WebFilter("*.xhtml")`) lets the request through if it is for `login`, `register` or `index`, for a JSF/static resource, or comes from a session with the `com.trousseau.loggedIn` attribute set. Otherwise it redirects to `/login.xhtml`.
2. **`PrimeFaces FileUpload Filter`** (from `web.xml`) parses multipart bodies for `<p:fileUpload>`.
3. **`FacesServlet`** (mapped to `*.xhtml`) runs the JSF lifecycle. View-scoped beans are created on first access and their `@PostConstruct` loads the page data.
4. Pages that take an id (`clothing-detail.xhtml?id=…`, `outfit-detail.xhtml?id=…`) bind it with `<f:viewParam>` and load with a `preRenderView` event listener (`loadItem()` / `loadOutfit()`).
5. Actions are mostly AJAX `p:commandButton`s. They call a bean method, which calls a service, adds a `FacesMessage` and refreshes the bean's lists. The page shows messages through the `<p:growl id="growl">` in the layout.

Navigation uses implicit outcomes (`"wardrobe?faces-redirect=true"`). The two rules in `faces-config.xml` (login→wardrobe, register→login) do the same thing in the old style.

---

## Authentication and session state

- **Registration**: `UserService.register()` checks that the username and email are unique and stores a **bcrypt** hash (cost 12) in `app_user.password_hash`.
- **Login**: `LoginBean` → `UserService.authenticate()` → `SessionBean.login(user)`. This stores the `User` entity in the session-scoped bean and sets the `com.trousseau.loggedIn` session attribute that `AuthFilter` checks.
- **Current user**: every bean injects `SessionBean` and calls `getCurrentUser()`. The `User` held there is a **detached** entity. Services merge it when they update it.
- **Logout**: invalidates the HTTP session.
- **Password reset**: a 32-byte `SecureRandom` token, base64url-encoded, stored on the user with a 60-minute expiry. The link is shown on screen (there is no email). See [Security](security.md).

There are no roles. All authenticated users have the same permissions, and ownership is checked in individual beans (see [Security → Authorization model](security.md#authorization-model)).

---

## Pages and their beans

| Page | Bean (scope) | Purpose |
|---|---|---|
| `index.xhtml` | `wardrobeBean`, `outfitListBean` | Landing hero (anonymous) / mini dashboard (signed in) |
| `login.xhtml` | `LoginBean` (request) | Sign in |
| `register.xhtml` | `RegisterBean` (request) | Create account |
| `forgot-password.xhtml` | `ForgotPasswordBean` (request) | Generate a reset link |
| `reset-password.xhtml` | `ResetPasswordBean` (view) | Set a new password from `?token=` |
| `wardrobe.xhtml` | `WardrobeBean` (view) | Item grid, filters, add dialog, wash alerts, retired list |
| `bulk-add.xhtml` | `BulkAddBean` (view) | Multi-photo upload into editable drafts |
| `clothing-detail.xhtml` | `ClothingDetailBean` (view) | One item: purchase info, wear, tags, receipt, lifecycle, share, delete |
| `laundry.xhtml` | `LaundryBean` (view) | Items needing a wash; batch wash |
| `outfits.xhtml` | `OutfitListBean` (view) | Outfit list, filters, create dialog |
| `outfit-detail.xhtml` | `OutfitDetailBean` (view) | One outfit: items, wear log, ratings, comments, share, delete |
| `weekly-planner.xhtml` | `WeeklyPlannerBean` (view) | Saved week plan, overrides, weather, record wear |
| `insights.xhtml` | `InsightsBean` (view) | Read-only reports, all loaded once in `@PostConstruct` |
| `shared.xhtml` | `SharedItemsBean` (view) | Shared-with-me / shared-by-me |
| `profile.xhtml` | `ProfileBean` (view) | Display name, email, location, password |
| `data-management.xhtml` | `DataManagementBean` (view) | JSON export download / import upload |

Bean names in EL are the class name with a lowercase first letter (`@Named` with no value).

---

## Services

| Service | Type | Notes |
|---|---|---|
| `UserService` | `@Stateless` | Register, authenticate, profile and password updates, reset tokens |
| `ClothingItemService` | `@Stateless` | Item CRUD, wear/wash, batch wash, retire/reactivate, receipts, tags. **The only place photos should be attached** (`attachImage()`), so a thumbnail is always generated. Also holds the category list. |
| `TagService` | `@Stateless` | Per-user tags; `findOrCreate()` |
| `OutfitService` | `@Stateless` | Outfit CRUD. `findById` / `findByCreator` initialise lazy collections inside the transaction. |
| `OutfitWearLogService` | `@Stateless` | Records outfit wears (and item wears), monthly and seasonal counts |
| `RatingService` | `@Stateless` | Upserts one rating per (outfit, rater, type) |
| `CommentService` | `@Stateless` | Add, list, delete comments |
| `ShareService` | `@Stateless` | Share items/outfits by user; rejects duplicates |
| `InsightsService` | `@Stateless` | Read-only aggregates via projection queries |
| `WeeklyPlannerService` | `@Stateless` | Scoring algorithm and stored-plan management |
| `WeatherService` | `@Singleton`, bean-managed concurrency | Open-Meteo client with an in-memory LRU cache. Never throws. |
| `DataExportImportService` | `@Stateless` | JSON export/import ([format](export-format.md)) |

Algorithms are described in [How it works](how-it-works.md).

---

## Background jobs

There is one: **`WeeklyPlannerEmailScheduler`**, a `@Singleton @Startup` EJB with

```java
@Schedule(dayOfWeek = "Sun", hour = "17", minute = "0", second = "0", persistent = false)
```

For each user with an email address it calls `WeeklyPlannerService.getOrCreateWeekPlan(user, nextMonday)` and sends an inline-styled HTML email through the `java:jboss/mail/Default` session. Errors are caught per user. Operational details are in [Setup → Weekly email](setup-and-deployment.md#weekly-email-smtp).

---

## Serving images

`ImageStreamer` is an `@ApplicationScoped` bean used with PrimeFaces `<p:graphicImage value="#{imageStreamer.thumbnail}">` plus `<f:param name="itemId" …>`.

PrimeFaces streams images in two requests. During the page's `RENDER_RESPONSE` phase the bean returns empty content, and PrimeFaces only writes a URL. The browser then fetches that URL, and on that request the bean reads `itemId` and returns the bytes.

- `getThumbnail()` serves the stored thumbnail (generating and saving it if missing), or the original if it is already small. Every list or grid view should use this.
- `getImage()` serves the full-size original. Only the item detail page should use it.

Receipts and full image downloads go through `ClothingDetailBean.downloadReceipt()` / `downloadImage()` with `p:fileDownload`.

---

## Key design decisions

These explain code that might otherwise look odd.

**Never load BLOBs in list or aggregate queries.** `ClothingItem` holds the photo, thumbnail and receipt as `@Lob @Basic(fetch = LAZY)` columns. Pickers use `ClothingItemSummary` (id, name, category). Insights use `ItemWearStat` / `CategoryWearStat` / `OutfitWearStat` built with JPQL `SELECT NEW …`. BLOB bytes are fetched one column at a time with `SELECT c.imageData FROM ClothingItem c WHERE c.id = :id`. Without this, an insights page over a large wardrobe would pull every photo into memory.

**Thumbnails are stored, not computed per request.** A wardrobe page with 50 items would otherwise send 50 full-size photos to draw 200 px tiles. See [How it works → Photos](how-it-works.md#photos-and-thumbnails).

**Database-portable queries.** The same WAR runs on H2 and PostgreSQL, with no `hibernate.dialect` set. Anything whose SQL differs between them is done in Java: monthly bucketing of wear dates in `InsightsService.getRecentActivity()`, and the `max(wearCount, 1)` in cost-per-wear ranking.

**Columns added after first release are nullable, with null-safe getters.** `hbm2ddl=update` adds new columns as `NULL` on existing rows. `ClothingItem.status` and `wearsSinceWash` are therefore boxed types with getters that map `NULL` to `ACTIVE` / `0`, and "in rotation" queries read `status IS NULL OR status = ACTIVE`.

**The weekly plan is persisted.** Earlier, the plan was regenerated on each page view, so manual overrides vanished on reload and the Sunday email could differ from the page. `PlannedOutfit` rows are now the source of truth. Generation is the fallback.

**Weather never breaks anything.** `WeatherService` returns an empty map on any failure and caches the failure. The planner then falls back to its original two-factor scoring. Trousseau is meant to run on a LAN without internet.

**Lazy collections are initialised in services.** The view renders after the EJB transaction has committed, so services touch the collections the view needs (`outfit.getItems().size()` etc.) before returning, to avoid `LazyInitializationException`.

**`@ViewScoped` lists must be serializable.** For example, `InsightsService` copies `subList()` results into a new `ArrayList`, because the container may passivate view state and `subList` views aren't serializable.

**EL reserved words.** `LaundryBean.isBasketEmpty()` is not called `isEmpty()`, because `empty` is an EL operator and `#{laundryBean.empty}` would fail to parse.
