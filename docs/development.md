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

- **JDK 11+.** The compiler uses `--release 11`, so code must stick to the Java 11 API even if you build on a newer JDK.
- **Maven 3.6+.**
- **Lombok.** Entities and projections use `@Getter`, `@Setter`, `@EqualsAndHashCode` etc. Enable annotation processing in your IDE, or install the Lombok plugin.
- **PrimeFaces repository.** `pom.xml` adds `https://repository.primefaces.org` for the `all-themes` artifact.

## Build and run

| Task | Command |
|---|---|
| Compile and package | `mvn clean package` → `target/trousseau.war` |
| Run with a fresh WildFly | `mvn package wildfly:run` (see [Setup](setup-and-deployment.md#running-locally)) |
| Redeploy to the running server | From a second terminal: `mvn package wildfly:deploy` |
| Offline build | `mvn -o package` once dependencies are cached |

Facelets pages are re-read in `Development` project stage, but Java changes need a redeploy.

---

## Conventions

### Layering

- **Pages** bind only to beans.
- **Beans** (`@Named`, usually `@ViewScoped` and `Serializable`) get the user from `SessionBean.getCurrentUser()` and call **services**, never DAOs. User feedback goes through `FacesContext.addMessage(null, new FacesMessage(…))` and appears in the layout's growl.
- **Services** are `@Stateless` EJBs. Every public method is a transaction. Put business rules here, not in beans.
- **DAOs** are `@ApplicationScoped`, hold an `@PersistenceContext EntityManager`, and contain only queries and persist/merge/remove.
- **Entity behaviour** that is purely about the entity's own fields belongs on the entity (`ClothingItem.recordWear()`, `markWashed()`, `retire()`, `getCostPerWear()`).

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
- Detail pages take their id through `<f:viewParam>` and load in a `preRenderView` listener.

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
4. `AuthFilter` protects the page automatically. Add it to the filter's public list only if it must be reachable when signed out.

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
| 1 | Auth | `forgot-password.xhtml` and `reset-password.xhtml` are not in `AuthFilter`'s public list, so signed-out users are redirected to login and can't reset a password. | Fix the reset flow's [security issue](security.md#1-password-reset-allows-account-takeover-high) **first**, then whitelist both pages. |
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
| 13 | Email | The sender `noreply@trousseau.app` is hard-coded. | Make it configurable (system property or JNDI env entry). |

---

## Repository housekeeping

- There is no **`.gitignore`**. Add one with at least `target/`, `.idea/`, `*.iml`, `.vscode/`, `.settings/`, `.classpath`, `.project`, so the built WAR and the downloaded WildFly are not committed.
- There is no **`LICENSE`** file, although the app footer says Apache License 2.0.
- Comments in `pom.xml` and `persistence.xml` refer to a Docker image (Temurin 11) and a Docker/PostgreSQL stack. These files are not in this repository.
