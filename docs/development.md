# Development

Notes for anyone changing the Trousseau code. Read [Architecture](architecture.md) first for the overall structure.

- [Environment](#environment)
- [Build and run](#build-and-run)
- [Conventions](#conventions)
- [Recipes](#recipes)
- [Testing](#testing)
- [Known bugs](#known-bugs)
- [Repository housekeeping](#repository-housekeeping)

---

## Environment

- **JDK 21+ with `javac`.** The compiler uses `--release 21`, so code must stick to the Java 21 API even if you build on a newer JDK (JDK 25 works). A JRE alone cannot build.
- **Maven 3.6+.**
- **Lombok.** Entities and projections use `@Getter`, `@Setter`, `@EqualsAndHashCode` etc. `pom.xml` registers Lombok as an annotation processor explicitly, because newer JDKs no longer run processors found on the plain classpath. In your IDE, enable annotation processing or install the Lombok plugin.
- **Jakarta namespaces.** All Jakarta EE APIs are `jakarta.*`. Only Java SE packages (e.g. `javax.imageio`) keep `javax`. XHTML pages use the `jakarta.faces.*` / `jakarta.tags.core` namespace URNs.

## Build and run

| Task | Command |
|---|---|
| Compile and package | `mvn clean package` → `target/trousseau.war` (about 5 MB) |
| Run with a fresh WildFly | `mvn package wildfly:run` (see [Setup](setup-and-deployment.md#running-locally)) |
| Redeploy to the running server | From a second terminal: `mvn package wildfly:deploy` |
| Offline build | `mvn -o package` once dependencies are cached |
| Run the production-like stack (WildFly + PostgreSQL) | `docker compose up -d --build` (see [Docker](docker.md)) |

Code must work on both H2 (Maven run) and PostgreSQL (Docker), so try changes that touch queries on both.

Facelets pages are re-read in `Development` project stage, but Java changes need a redeploy.

---

## Conventions

### Layering

- **Pages** bind only to beans.
- **Beans** (`@Named`, usually `@ViewScoped` and `Serializable`) get the user from `SessionBean.getCurrentUser()` and call **services**, never DAOs. User feedback goes through `FacesContext.addMessage(null, new FacesMessage(…))` and appears in the layout's growl.
- **Services** are `@Stateless` EJBs. Every public method is a transaction. Put business rules here, not in beans.
- **DAOs** are `@ApplicationScoped`, hold an `@PersistenceContext EntityManager`, and contain only queries and persist/merge/remove.
- **Entity behaviour** that is purely about the entity's own fields belongs on the entity (`ClothingItem.recordWear()`, `markWashed()`, `retire()`, `getCostPerWear()`).

### Access checks

Anything that loads a single item or outfit from a request parameter must ask **`AccessService`** before using it. Ids are sequential, so an unchecked `findById(param)` lets any user read or change anyone's data.

- On a detail page, check in the `preRenderView` listener and call `PageResponses.notFound()` when it fails, as `ClothingDetailBean.loadItem()` does. Use 404 both for "doesn't exist" and "not yours".
- Hide owner-only controls with `rendered="#{bean.owner}"`, **and** re-check ownership at the top of each action (`refuseUnlessOwner()`). JSF won't invoke an unrendered component, but the second check means a rendering mistake can't become a hole.
- Endpoints that stream data (photos, receipts, exports) need the same check. They are easy to forget because they aren't pages.
- List pages are safe as long as their queries filter on the current user.

### The session's user is read-only

`SessionBean.getCurrentUser()` is a snapshot from login. Use it to read and to pass as the owner in queries, but **never merge it or pass it to a save**. Its fields can be stale, and merging writes every one of them back. To change the account, call an id-based `UserService` method that loads the entity fresh and sets only the fields being changed, then `sessionBean.refresh(result)`.

A password change must go through `User.changePasswordHash()`, which bumps the credentials version so `AuthFilter` signs out other sessions.

### Working with photos and BLOBs

- Attach photos only through `ClothingItemService.attachImage()`, so that a thumbnail is always generated.
- In any grid, card or list, use `#{imageStreamer.thumbnail}` with an `itemId` param. Reserve `#{imageStreamer.image}` for single large views.
- **Never** return `ClothingItem` entities from a query that covers many items for reporting purposes. Project into a small class with `SELECT NEW …` (see `ItemWearStat`). Load BLOB bytes with a single-column query (`ClothingItemDao.loadImageData`).

### Lazy loading

Views render after the service transaction has ended. If a page needs a lazy collection, touch it in the service before returning (`outfit.getItems().size()`), as `OutfitService` and `WeeklyPlannerService` do. Alternatively, use `JOIN FETCH` in the query.

### Database portability

Code must run unchanged on **H2 and PostgreSQL**. Avoid database-specific functions (date truncation, `GREATEST`, string functions beyond JPQL's). Where a calculation can't be written portably, fetch the raw values and compute in Java, as `InsightsService` does.

### Schema evolution

`hbm2ddl=update` adds columns to existing rows as `NULL`. Any field added to an existing entity must:

1. Use a boxed type (`Integer`, `Boolean`, enum) if a primitive default would be wrong for old rows.
2. Have a null-safe getter (`ClothingItem.getStatus()`, `getWearsSinceWash()`).
3. Be handled as `IS NULL OR …` in JPQL where it filters.

See [Data model → Schema management](data-model.md#schema-management).

### EL and JSF gotchas

- Don't name a bean property `empty`, `not`, `and`, `or`, `div`, `mod`, `eq`, `ne`, `lt`, `gt`, `le`, `ge`, `true`, `false`, `null` or `instanceof`. These are EL keywords (see `LaundryBean.isBasketEmpty()`).
- Lists held by `@ViewScoped` beans must be serializable. Copy `subList()` results into a new `ArrayList`.
- Detail pages take their id through `<f:viewParam>` and load in a `preRenderView` listener. Faces requires `<f:metadata>` directly under the view, so put it in `<ui:define name="metadata">`, which `layout.xhtml` inserts, and not inside `content`.
- **PrimeFaces 16 drops attributes without warning at build time.** An attribute a component no longer has fails only when the page renders (`Setter not found for property …`). After upgrading PrimeFaces, check every `p:` tag against the new `META-INF/primefaces.taglib.xml`. During the move to 16, `p:fileUpload`'s `allowTypes`/`sizeLimit` moved to a `<p:validateFile>` child, `dragDropSupport` became `dragDrop`, and checkbox columns became `<p:column selectionBox="true">`.
- **Forms with a non-AJAX file upload need `enctype="multipart/form-data"`.** Without it the browser never sends the file. PrimeFaces 16 logs a warning naming the component.

### JPQL

WildFly runs Hibernate in strict Jakarta Persistence compliance mode. Named queries are validated when the app deploys, and a non-compliant one fails the whole deployment. Common traps: an alias on a `JOIN FETCH` (filter with `MEMBER OF` or a separate join instead), and Hibernate-only functions. Queries built with `createQuery` are only checked when they run, so exercise them in a test.

### Style

- `java.util.logging` for logs.
- Explain **why** in Javadoc and comments, as the existing code does (e.g. why cost per wear treats 0 wears as 1).
- Keep code constants (limits, thresholds) as named `static final` fields near the top of the class.

---

## Recipes

### Add a page

1. Create `src/main/webapp/my-page.xhtml`:
   ```xml
   <ui:composition template="/templates/layout.xhtml" …namespaces…>
       <ui:define name="title">My Page</ui:define>
       <ui:define name="content">
           <h:form> … </h:form>
       </ui:define>
   </ui:composition>
   ```
2. Create `bean/MyPageBean.java`: `@Named @ViewScoped implements Serializable`, inject `SessionBean` and the services you need, load data in `@PostConstruct`.
3. Add a nav link in `templates/layout.xhtml` inside the `sessionBean.loggedIn` fragment.
4. `AuthFilter` protects the page automatically. Add it to `AuthFilter.PUBLIC_PAGES` only if it must be reachable when signed out.
5. If the page loads anything by id from the URL, follow [Access checks](#access-checks).

### Add an entity

1. Create the class in `model/` with `@Entity`, `@Table`, Lombok annotations, `@EqualsAndHashCode(of = "id")`, and any `@NamedQueries`.
2. **Add a `<class>` line to `src/main/resources/META-INF/persistence.xml`.** The persistence unit lists classes explicitly.
3. Create a DAO and a `@Stateless` service.
4. Decide how deletes of related entities should treat it (see [Data model → Cascades](data-model.md#cascades-and-deletion)), and whether it belongs in the export.

### Add a field to `ClothingItem`

1. Add the field following [schema evolution](#schema-evolution).
2. Expose it in `wardrobe.xhtml` (add form), `bulk-add.xhtml` / `BulkAddBean.Draft` if relevant, and `clothing-detail.xhtml`.
3. Add it to `DataExportImportService`, export and import. On import, read it with a default so older files still import. Bump `VERSION` and update [Export format](export-format.md).

### Add an item category or outfit occasion

- Categories: `ClothingItemService.getCategories()`. Note that `"Outerwear"` has special meaning for rain scoring in the planner.
- Occasions: the `<f:selectItem>` lists in `outfits.xhtml` (create dialog **and** filter).

---

## Testing

There is **no automated test suite** and no test dependencies in `pom.xml`. Good first candidates for unit tests, since they are pure logic:

- `ClothingItem.recordWear()` / `markWashed()` / `getCostPerWear()`
- `WeeklyPlannerService` scoring (`seasonFit`, `freshnessScore`, `jaccardDistance`). These are private and would need to be extracted or made package-private.
- `BulkAddBean.prettifyFileName()`
- `ImageUtil.createThumbnail()`
- `DataExportImportService` round trip

For integration tests, Arquillian with a managed WildFly, or Testcontainers PostgreSQL, fit the stack.

Manual smoke test after changes: register → bulk add 3 photos → create an outfit → record a wear on it → check the laundry basket → open the planner → open insights → export, then import into a second account.

---

## Known bugs

Functional issues in the current code. Security issues are listed separately in [Security → Known issues](security.md#known-issues).

| # | Area | Problem | Suggested fix |
|---|---|---|---|
| 1 | **Memory** | **Pages that list items keep full `ClothingItem` entities, with photo, thumbnail and receipt bytes, in view-scoped beans.** `@Basic(fetch = LAZY)` has no effect without bytecode enhancement, so every list query loads every BLOB. Measured: about **85 MB retained per signed-in session** after the home, wardrobe and planner pages, for a wardrobe with one 6 MB photo and one 12 MB receipt. About ten such sessions exhaust the default 1 GB heap (`OutOfMemoryError`), and a realistic wardrobe of a hundred photos would exhaust it in one. Present before and after the Jakarta EE migration. | Move the BLOBs to their own entity (e.g. `ItemMedia`, `@OneToOne(fetch = LAZY)`), or enable Hibernate bytecode enhancement so `LAZY` is honoured; and make list pages use summary projections, as insights already do. |
| 2 | Items | Deleting an item that belongs to any outfit or has been shared fails with a foreign-key violation (`outfit_items`, `share`). The user sees an error page. | In `ClothingItemService.delete()`, remove the item from outfits and delete its shares before removing it. Or block the delete with a friendly message suggesting Retire. |
| 3 | Outfits | Deleting an outfit that has wear logs, planned days or shares fails the same way (`outfit_wear_log`, `planned_outfit`, `share`). Most outfits end up planned once the planner has been opened. | Delete `planned_outfit` and `share` rows first. Decide whether wear logs are deleted or the outfit is soft-deleted. |
| 4 | Items | Name, category, colour, brand, size, description and wash threshold can't be edited after creation. `WardrobeBean.updateItem()` exists but no page uses it. | Add an edit form to `clothing-detail.xhtml`. |
| 5 | Wardrobe | No tag filter in the UI, although `WardrobeBean.filterByTag()` exists. | Add a tag `selectOneMenu` using `tagConverter`. |
| 6 | Wear tracking | Recording an outfit wear for a past date sets each item's `lastWornDate` to today. | Pass the date through `ClothingItemService.recordWear(item, date)` and keep the later of the two dates. |
| 7 | Performance | `outfit-detail.xhtml` uses `#{imageStreamer.image}` (full size) for item tiles. | Switch to `#{imageStreamer.thumbnail}`. |
| 8 | Performance | The signed-in home page builds `WardrobeBean` and `OutfitListBean` just to show three counts, loading every item with its tags. | Use `InsightsService` counts. |
| 9 | Planner | The same outfit can be planned on several days of one week, and outfits that contain retired items are still suggested. | Penalise outfits already chosen this week; filter out outfits with retired items. |
| 10 | Ratings | The outfit list's stars (mean of all ratings, truncated) can disagree with the detail page's Overall Average (mean of per-type averages). | Use one definition in both places. |
| 11 | Seasons | The Winter range ends on 28 Feb, so 29 Feb wears in leap years are not counted in seasonal stats. | Use `YearMonth.of(year, 2).atEndOfMonth()`. |
| 12 | i18n | `messages.properties` is registered as `#{msg}` in `faces-config.xml`, but no page uses it. All UI text is hard-coded in the XHTML. | Move strings to the bundle, or delete it. |
| 13 | Uploads | Outside Docker, WildFly's default 10 MB request limit rejects bulk-add photos over 10 MB (the page allows 20 MB) and most data imports. | Documented in [Setup → Raise the upload limit](setup-and-deployment.md#raise-the-upload-limit); the Docker image sets 256 MB. |
| 14 | Errors | Business-rule failures thrown as `IllegalArgumentException` from `@Stateless` services reach beans wrapped in `EJBException`, so the beans' `catch (IllegalArgumentException)` never matches and the user gets an HTTP 500 with a stack trace. Affects registering a taken username or email, sharing something twice, and creating a duplicate tag. Sharing with yourself is also allowed. | Throw an exception class annotated `@ApplicationException(rollback = true)`, which EJB passes through unwrapped, and catch that. Reject sharing with your own username. |
| 15 | Images | When a photo is too small to need a thumbnail, `ImageStreamer.getThumbnail()` serves the original but always labels it `image/jpeg`, even for PNG/GIF/WebP. Browsers sniff and render it anyway. | Return the item's stored content type when serving the original. |

---

## Repository housekeeping

- There is no **`LICENSE`** file, although the app footer says Apache License 2.0.
- `.gitignore` excludes `target/` and `.env`. Never commit `.env`; it holds the database password.
