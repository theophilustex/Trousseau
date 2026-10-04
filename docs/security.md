# Security

This page describes how Trousseau handles authentication and access control, lists known security issues in the current code, and gives a hardening checklist for anyone running an instance.

> **Summary.** Trousseau is currently suitable for a **trusted group** (a household, or friends on a private network). It is not ready for open registration on the public internet until the [known issues](#known-issues) marked *High* are fixed.

- [Authentication](#authentication)
- [Authorization model](#authorization-model)
- [Known issues](#known-issues)
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
| CSRF | JSF postbacks require a valid `javax.faces.ViewState`, which gives some protection for form actions. No explicit CSRF tokens or `<protected-views>`. |
| Password reset | 32-byte `SecureRandom` token, base64url, valid 60 minutes, single use, stored in plain text in `app_user.password_reset_token` |

## Authorization model

There are no roles. `AuthFilter` only decides whether a request is **authenticated**. It lets everyone through to `index`, `login`, `register` and static/JSF resources, and redirects all other pages to login unless the session is signed in.

Ownership is enforced **by query scope**, not by per-object checks. List pages (wardrobe, outfits, laundry, insights, planner, shared, export) only query rows belonging to `sessionBean.currentUser`, so users never see each other's data **in lists**.

Pages that load a single object **by id from the URL** do not check ownership consistently:

| Endpoint | Ownership check |
|---|---|
| `clothing-detail.xhtml?id=N` | **None.** Any signed-in user can view, edit, retire, tag, share, delete, and download the photo/receipt of any item. |
| `outfit-detail.xhtml?id=N` | Edit, share and delete controls render only for the creator (`OutfitDetailBean.isOwner()`), and JSF will not invoke actions on unrendered components. Viewing, rating and commenting are open to **any** signed-in user, not just people it was shared with. |
| `#{imageStreamer.image}` / `.thumbnail` with `itemId=N` | **None.** Any signed-in user can fetch any item's photo by id. |
| Comment delete | Checks that the current user is the author |

Sharing therefore works as a way to *tell* someone about an item or outfit. It is not an access-control boundary.

---

## Known issues

Ordered by severity.

### 1. Password reset allows account takeover (High)

`ForgotPasswordBean` displays the generated reset link **on screen to whoever requested it**. Anyone who knows (or guesses) a username or email can generate a link for that account, open it, and set a new password.

The page is currently only reachable by signed-in users, because `AuthFilter` does not whitelist `forgot-password.xhtml` and `reset-password.xhtml` (see [Development → Known bugs](development.md#known-bugs)). In practice this means **any registered user can take over any other account**. If the filter is "fixed" by whitelisting those pages without fixing the flow, **anonymous visitors** can take over accounts too.

**Fix:** deliver the link by email only (the app already has a mail session), always show the same generic "if the account exists, we've sent a link" response, store only a hash of the token, and only then add the two pages to the `AuthFilter` whitelist. Until then, consider removing the "Forgot your password?" link.

### 2. Item pages have no ownership check (High)

See the [authorization table](#authorization-model). Ids are sequential, so they are easy to enumerate. Any signed-in user can open `clothing-detail.xhtml?id=1`, `?id=2`, … to view, edit or **delete** other users' items and download their receipts.

**Fix:** in `ClothingDetailBean.loadItem()`, load the item only if `item.owner == currentUser` or a `Share` exists for (item, currentUser). Make every mutating action require ownership. Apply the same rule in `ImageStreamer`.

### 3. Outfits are readable by any user (Medium)

`OutfitDetailBean.loadOutfit()` loads any outfit by id. Its items' photos, ratings and comments are visible, and anyone can add ratings and comments.

**Fix:** allow access only to the creator or a user the outfit has been shared with.

### 4. Uploaded content type is trusted (Medium)

The photo's `Content-Type` and filename come from the browser and are served back unchanged by `ImageStreamer.getImage()`. Type restrictions (`allowTypes`) are declared on the PrimeFaces upload components, and receipts accept `image/*` and PDF. Nothing on the server checks that the bytes really are an image. A crafted upload could be served inline as `text/html` from the app's origin.

**Fix:** on upload, decode with `ImageIO` (already done for thumbnails) and reject files that fail. Serve photos with a fixed `image/*` type. Add an `X-Content-Type-Options: nosniff` response header.

### 5. No brute-force protection (Medium)

Unlimited login attempts. Mitigate at the reverse proxy (rate limiting, fail2ban), or add a per-account backoff in `UserService.authenticate()`.

### 6. Session fixation (Low)

The session ID is not changed at login. Call `request.changeSessionId()` in `LoginBean.login()` after successful authentication.

### 7. Development project stage (Low)

`web.xml` ships with `javax.faces.PROJECT_STAGE=Development`, which shows detailed error pages including stack traces and EL expressions. Set it to `Production`.

---

## Other considerations

- **Output escaping.** All dynamic text goes through JSF components, which HTML-escape by default. No `escape="false"` is used. The weekly email builds HTML by hand and escapes outfit and item names with `escapeHtml()`.
- **SQL injection.** All queries are JPQL with bound parameters.
- **Data at rest.** Photos, receipts and all personal data are stored unencrypted in the database. The local H2 setup uses WildFly's `ExampleDS` default credentials (`sa`/`sa`) and listens only on the local file. With `AUTO_SERVER=TRUE`, H2 also opens a TCP port bound to the local machine.
- **Third parties.** If a user sets a location, their coordinates are sent to Open-Meteo (`api.open-meteo.com`) by the server. Browsers fetch the Inter font from Google Fonts on every page.
- **Email.** The weekly email lists outfit and item names in plain text, so treat it as personal data in transit through your SMTP provider.
- **Cookies.** No `<cookie-config>` is set, so `Secure` is not added to the session cookie. Set it when serving over HTTPS.

---

## Hardening checklist

Before exposing an instance beyond people you trust:

- [ ] Fix or disable the password-reset flow ([issue 1](#1-password-reset-allows-account-takeover-high))
- [ ] Add ownership checks to item and outfit detail pages and `ImageStreamer` ([2](#2-item-pages-have-no-ownership-check-high), [3](#3-outfits-are-readable-by-any-user-medium))
- [ ] Serve only over **HTTPS** (TLS at a reverse proxy) and enable `proxy-address-forwarding` ([Setup](setup-and-deployment.md#running-behind-a-reverse-proxy))
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
- [ ] Set `javax.faces.PROJECT_STAGE` to `Production`
- [ ] Rate-limit `/trousseau/login.xhtml` at the proxy
- [ ] Use PostgreSQL with a dedicated, least-privilege database user and a strong password
- [ ] Protect the WildFly management interface (port 9990): bind it to localhost or firewall it
- [ ] Back up the database regularly ([Setup → Backups](setup-and-deployment.md#backups))
