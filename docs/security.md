# Security

This page describes how Trousseau handles authentication and access control, lists known security issues in the current code, and gives a hardening checklist for anyone running an instance.

> **Summary.** The serious issues found so far (an authentication bypass, password-reset account takeover, missing ownership checks, and stale sessions undoing password changes) are [fixed](#fixed-issues). Trousseau is suitable for a household or a group of friends. Before opening registration to the public internet, work through the [remaining issues](#known-issues) and the [hardening checklist](#hardening-checklist).

- [Authentication](#authentication)
- [Password reset](#password-reset)
- [Authorization model](#authorization-model)
- [Known issues](#known-issues)
- [Fixed issues](#fixed-issues)
- [Other considerations](#other-considerations)
- [Hardening checklist](#hardening-checklist)

---

## Authentication

| Aspect | Implementation |
|---|---|
| Password storage | bcrypt via jBCrypt, cost factor 12 (`PasswordUtil`) |
| Password policy | Minimum 6 characters (JSF validators on register, reset and change-password forms). No other rules. |
| Login | Username + password form. Failed logins return a generic "Invalid username or password". |
| Brute-force protection | None (no rate limiting, lockout or CAPTCHA) |
| Session | Servlet `HttpSession`, 30-minute idle timeout. `SessionBean` holds the `User`; `AuthFilter` checks the `com.trousseau.loggedIn` attribute. |
| Logout | Invalidates the session |
| Session ID on login | Not rotated (`HttpServletRequest.changeSessionId()` is not called) |
| Password change / reset | Signs out every **other** session on its next request. The session that changed the password stays signed in. See [Sessions and password changes](#sessions-and-password-changes). |
| CSRF | JSF postbacks require a valid `jakarta.faces.ViewState`, which gives some protection for form actions. No explicit CSRF tokens or `<protected-views>`. |

`AuthFilter` lets signed-out visitors reach exactly these pages: `index`, `login`, `register`, `forgot-password` and `reset-password`, plus Faces resources (`/jakarta.faces.resource/…`). Everything else redirects to login. It matches on the container-normalised **servlet path**, never on the raw request URI (see [fixed issue A](#fixed-issues)).

## Sessions and password changes

Each account has a **credentials version** (`app_user.credentials_version`) that goes up whenever the password changes, by Profile or by reset link. At login, the session records the account's id and current version. On every signed-in page and photo request, `AuthFilter` compares the recorded version with the database (a one-column primary-key read), and invalidates the session if they differ. The session that made a Profile password change records the new version straight away, so it stays signed in.

`SessionBean.currentUser` is a snapshot taken at login. It is used for reading and as the owner in queries, and is **never merged back** into the database. Profile, location and password changes load the account fresh by id and set only the fields being changed (`UserService.updateProfile`, `updateLocation`, `changePassword`). Before this fix, a profile save in an old session restored the password from before a reset.

## Password reset

Implemented in `PasswordResetService`, `UserService` and `ForgotPasswordBean`.

1. A visitor enters a username or email on **Forgot your password?**
2. If it matches an account **that has an email address**, a single-use link is emailed to that address. The link is never shown on screen.
3. The page shows the same message whether or not anything matched, so it can't be used to find out which accounts exist. The email is sent in the background, so response time doesn't give it away either.

| Aspect | Implementation |
|---|---|
| Token | 32 bytes from `SecureRandom`, base64url-encoded |
| Storage | Only the token's **SHA-256 hash** is stored (`app_user.password_reset_token`), so a copy of the database contains no working links |
| Lifetime | 60 minutes, single use; cleared whenever the password changes |
| After use | Signs out every session on the account, including any an attacker may have had |
| Link host | Built from the configured **`TROUSSEAU_BASE_URL`**, never from the request's `Host` header, which the client controls. If it is unset, the feature is switched off and the page says so. |
| Throttle | At most one email per account every 2 minutes |
| Accounts without an email | Cannot reset themselves. Whoever runs the server must help. |

## Authorization model

There are no roles. Two mechanisms keep users' data apart:

1. **Lists are scoped by query.** Wardrobe, outfits, laundry, insights, planner, shared and export only ever query rows belonging to `sessionBean.currentUser`.
2. **Anything loaded by an id from the request goes through `AccessService`.** Ids are sequential and easy to guess, so this check is what stops one user from reading another's data.

| Who | Item | Outfit |
|---|---|---|
| **Owner** | Everything | Everything |
| **Someone it was shared with** | View photo, details, wear stats and tags. Price, receipt and lifecycle are hidden. | View, **rate and comment**. Recording wears, editing items, sharing and deleting are hidden. |
| **Someone an outfit containing the item was shared with** | Same as a direct item share, so the outfit's photos render | — |
| **Anyone else** | `404 Not Found`, the same as a missing id | `404 Not Found` |

Where it is enforced:

| Endpoint | Check |
|---|---|
| `clothing-detail.xhtml?id=N` | `ClothingDetailBean.loadItem()` answers 404 unless `canViewItem`. Every mutating action re-checks `ownsItem`, and owner-only controls are not rendered for others. |
| `outfit-detail.xhtml?id=N` | `OutfitDetailBean.loadOutfit()` answers 404 unless `canViewOutfit`. Owner-only actions re-check `ownsOutfit`. |
| `#{imageStreamer.image}` / `.thumbnail` with `itemId=N` | Empty response unless `canViewItem`, including for anonymous requests |
| Comment delete | Only the comment's author |

**Rule for new code:** any page or endpoint that loads a single object from a request parameter must check `AccessService` before using it. See [Development → Conventions](development.md#access-checks).

---

## Known issues

Ordered by severity.

### 1. Uploaded content type is trusted (Medium)

The photo's `Content-Type` comes from the browser and is served back unchanged by `ImageStreamer.getImage()`. `<p:validateFile>` checks the **file name** extension and size on the server (photos must end in `.gif`, `.jpg`, `.jpeg`, `.png` or `.webp`; bulk add caps each at 20 MB), but nothing checks that the bytes really are an image, and the data import accepts whatever it is given. A crafted upload with an image file name could still be served inline as `text/html` from the app's origin, to its owner and anyone it's shared with.

**Fix:** on upload, decode with `ImageIO` (already done for thumbnails) and reject files that fail. Serve photos with a fixed `image/*` type. Add an `X-Content-Type-Options: nosniff` response header.

### 2. No brute-force protection (Medium)

Unlimited login attempts. The reset form is throttled per account but not per client. Mitigate at the reverse proxy (rate limiting, fail2ban), or add per-account backoff in `UserService.authenticate()`.

### 3. Session fixation (Low)

The session ID is not changed at login. Call `request.changeSessionId()` in `LoginBean.login()` after successful authentication.

### 4. Development project stage (Low)

`web.xml` ships with `jakarta.faces.PROJECT_STAGE=Development`, which shows detailed error pages including stack traces and EL expressions. Set it to `Production`.

---

## Fixed issues

| | Issue | Severity | Fix |
|---|---|---|---|
| A | **Authentication bypass.** `AuthFilter` checked `uri.contains("/login.xhtml")` on the raw URI, so `/login.xhtml/../wardrobe.xhtml` passed the check and was then served as the wardrobe page. Combined with B, an anonymous visitor could view any item, download its receipt, and delete it. | Critical | Match exact, normalised servlet paths |
| B | **No ownership check on items.** Any signed-in user could view, edit or delete any item via `clothing-detail.xhtml?id=N`, and fetch any photo by id. | High | `AccessService`; read-only view for share recipients; 404 otherwise |
| C | **Password-reset account takeover.** The reset link was shown on screen to whoever asked, so anyone who knew a username could take over that account. | High | Link emailed to the account's address only; hashed tokens; base URL from config; throttle |
| D | **Outfits readable by anyone.** Any signed-in user could view, rate and comment on any outfit. Recipients could also re-share it and record wears against the owner's items. | Medium | Visible to creator and recipients only; share and record-wear owner-only |
| E | **A stale session could undo a password change.** Profile saves merged the user snapshot from login back into the database, restoring the old password hash after a reset. Password changes also left other sessions signed in. | Medium | Field-level updates on a fresh entity; credentials version checked by `AuthFilter` |

---

## Other considerations

- **Output escaping.** All dynamic text goes through JSF components, which HTML-escape by default. No `escape="false"` is used. Emails built by hand escape user-supplied text with `MailService.escapeHtml()`.
- **SQL injection.** All queries are JPQL with bound parameters.
- **HTML comments.** `FACELETS_SKIP_COMMENTS` is on, so developer comments in `.xhtml` files are not sent to browsers.
- **Data at rest.** Photos, receipts and all personal data are stored unencrypted in the database. The local H2 setup uses WildFly's `ExampleDS` default credentials (`sa`/`sa`). With `AUTO_SERVER=TRUE`, H2 also opens a TCP port bound to the local machine.
- **Third parties.** If a user sets a location, their coordinates are sent to Open-Meteo (`api.open-meteo.com`) by the server. Browsers fetch the Inter font from Google Fonts on every page.
- **Email.** The weekly email lists outfit and item names in plain text, so treat it as personal data in transit through your SMTP provider. Reset emails carry a live credential for 60 minutes; use an SMTP connection with TLS.
- **Cookies.** No `<cookie-config>` is set, so `Secure` is not added to the session cookie. Set it when serving over HTTPS.

---

## Hardening checklist

Before exposing an instance beyond people you trust:

- [ ] Set `TROUSSEAU_BASE_URL` to the public `https://` address and configure SMTP over TLS, so password reset works and its links are protected in transit
- [ ] Serve only over **HTTPS** (TLS at a reverse proxy) and enable `proxy-address-forwarding`. In Docker, set `BEHIND_PROXY=true`; enable it only when a proxy is actually in front. See [Setup](setup-and-deployment.md#running-behind-a-reverse-proxy) and [Docker](docker.md#behind-a-reverse-proxy).
- [ ] Add to `web.xml`:
  ```xml
  <session-config>
      <session-timeout>30</session-timeout>
      <cookie-config>
          <http-only>true</http-only>
          <secure>true</secure>
      </cookie-config>
      <tracking-mode>COOKIE</tracking-mode>
  </session-config>
  ```
- [ ] Set `jakarta.faces.PROJECT_STAGE` to `Production`
- [ ] Rate-limit `/trousseau/login.xhtml` and `/trousseau/forgot-password.xhtml` at the proxy
- [ ] Use PostgreSQL with a dedicated, least-privilege database user and a strong password
- [ ] Protect the WildFly management interface (port 9990): bind it to localhost or firewall it
- [ ] Back up the database regularly ([Setup → Backups](setup-and-deployment.md#backups))
