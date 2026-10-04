# User guide

This guide walks through every screen in Trousseau. The nav bar at the top of each page links to the main areas: **Wardrobe, Outfits, Planner, Laundry, Insights, Shared, Profile, Data**. The 🌙/☀️ button switches between light and dark mode, and the choice is remembered in your browser.

Confirmations and errors appear as pop-up notifications ("growls") in the corner of the screen.

- [Getting started](#getting-started)
- [Wardrobe](#wardrobe)
- [Item details](#item-details)
- [Bulk add](#bulk-add)
- [Laundry](#laundry)
- [Outfits](#outfits)
- [Outfit details](#outfit-details)
- [Weekly planner](#weekly-planner)
- [Insights](#insights)
- [Sharing](#sharing)
- [Profile](#profile)
- [Data export and import](#data-export-and-import)
- [Things to know](#things-to-know)

---

## Getting started

### Create an account

1. Open the app and click **Get Started** or **Register**.
2. Fill in:
   - **Username**: 3–50 characters, must be unique. You sign in with this.
   - **Email** (optional): used for the Sunday planner email and as an alternative way to identify you when resetting your password. Must be unique if given.
   - **Display name** (optional): shown in the nav bar and on your comments. Defaults to your username.
   - **Password**: at least 6 characters, entered twice.
3. Click **Register**. You are taken to the login page.

### Sign in and out

Sign in with your username and password. You land on **Wardrobe**. Sessions expire after **30 minutes** of inactivity. Use **Logout** in the nav bar to end a session immediately.

### Home dashboard

Clicking **Trousseau** in the nav bar while signed in shows a small dashboard: how many items and outfits you have, how many items need washing, and shortcuts to the wardrobe and outfits.

### Forgot your password

The **Forgot your password?** link on the login page opens a form where you enter your username or email. If your account has an email address, Trousseau emails you a link. Open it within **60 minutes** to set a new password. Each link works once, and asking again within 2 minutes doesn't send another.

- The page says "Check your email" whatever you enter. That's deliberate: it stops strangers using the form to find out who has an account.
- **No email address on your account means no reset link.** Add one in [Profile](#profile) while you can still sign in, or ask whoever runs the server for help.
- If the page says *Password reset isn't set up*, the server hasn't been configured to send these emails ([Docker](docker.md#configuration): `TROUSSEAU_BASE_URL` and the SMTP settings).

Setting a new password this way signs out every device that was signed in to your account. Changing a password you still know is done from [Profile](#profile).

---

## Wardrobe

The wardrobe shows every item **in rotation** (not retired), newest first, as a grid of photo cards. Each card shows the item's name, category, colour, wear count and tags, and has a **Record Wear** button.

### Adding an item

Click **Add Item** to open the form:

| Field | Notes |
|---|---|
| Name | Required, up to 100 characters |
| Category | Tops, Bottoms, Dresses, Outerwear, Shoes, Accessories, Activewear, Sleepwear, Swimwear, Formal, Underwear, Other |
| Colour, Brand, Size | Free text. **Colour** is used by the planner to vary colours across the week. Plain names such as `navy`, `black` or `beige` work best (see [How it works](how-it-works.md#colour-variety)). |
| Description | Up to 500 characters |
| Purchase price, Purchase date | Optional. Used for [cost per wear](how-it-works.md#cost-per-wear). Can also be added later on the item's detail page. |
| Wash after wears | How many wears before the item is flagged for washing (default 3). Set **0** for items you never want flagged, such as coats or shoes. |
| Photo | GIF, JPEG, PNG or WebP. Large photos are automatically shrunk to a thumbnail for the grid; the original is kept. |

> Name, category, colour, brand, size, description and wash threshold can only be set when the item is created. Choose them carefully, or delete and re-add the item.

To add many items at once, use **Bulk add** (see [Bulk add](#bulk-add)).

### Finding items

- **Category** dropdown + **Filter**: show one category.
- **Search items…**: match on item name (case-insensitive).
- **Clear**: reset the filter and search.

Tags are shown on each card but there is currently no tag filter on this page.

### Wash alerts

When any items need washing, a banner at the top of the wardrobe lists them, each with a **Mark Washed** button, plus a link to the [Laundry basket](#laundry) for washing several at once.

### Retired items

Items you have archived, donated or sold are hidden from the main grid. Click **Show retired items** at the bottom to list them, and **Restore** to put one back into rotation.

---

## Item details

Click any item card to open its detail page.

| Panel | What you can do |
|---|---|
| **Photo** | View it full size; **Download Image** saves the original. |
| **Item Details** | Read-only summary of the item's attributes. |
| **Purchase & Value** | Enter **purchase price** and **purchase date**, then **Save**. With a price set, the item shows its **cost per wear** (see [How it works](how-it-works.md#cost-per-wear)). |
| **Wear Tracking** | Total wears, wears since last wash, wash threshold, last worn and last washed dates. **Record Wear** adds one wear. **Mark Washed** resets the wash counter. |
| **Tags** | Add one of your existing tags, or type a new tag name and click **Create** to make it and attach it in one step. Click × on a tag to remove it. Tags are private to you. |
| **Receipt** | Upload one receipt (image or PDF). **Download Receipt** or **Delete Receipt** afterwards. Uploading again replaces it. |
| **Lifecycle** | **Retire item** takes it out of rotation as *Archived* (kept but stored/off-season), *Donated* or *Sold*, with an optional note (up to 200 characters). A retired item keeps its full wear and cost history. **Return to wardrobe** reverses it. |
| **Share Item** | Type another user's username and click **Share**. See [Sharing](#sharing). |
| **Delete Item** | Permanently deletes the item, its photo and its receipt. Asks for confirmation. |

**Retire or delete?** Retiring is usually better. It keeps the item's history in Insights, especially its final cost per wear, and it can be undone. Deleting cannot be undone, and fails for items that belong to an outfit or have been shared (see [Things to know](#things-to-know)).

---

## Bulk add

**Wardrobe → Add several at once** (or the **Bulk add** button) opens a photo-first way to catalogue many items quickly.

1. **Drop in photos.** Select or drag up to **40** images (max 20 MB each). Each becomes a draft row with a thumbnail.
2. **Names are guessed from filenames.** `IMG_2481 navy wool coat.jpg` becomes **"Navy wool coat"**: camera prefixes like `IMG`, `DSC`, `PXL` or `PHOTO` plus their numbers are dropped, and underscores and dashes become spaces.
3. **Fill in the table.** Each row has name, category, colour, brand, size, wash threshold and purchase price.
4. **Apply to all** (optional). Set a category, brand and/or wash threshold at the top and apply it to every draft. Only the fields you fill in are applied.
5. **Save all N items.** All drafts with a name become wardrobe items. Drafts with an empty name are skipped and listed in a warning.

Nothing is saved until you click save. **Discard drafts** clears the queue. Drafts are lost if you navigate away. Once you reach 40 drafts, save them before adding more.

---

## Laundry

The **Laundry** page is your laundry basket: every in-rotation item currently flagged as needing a wash.

- **Mark all washed (N)**: marks every item in the basket washed (asks for confirmation).
- **Select all / Clear selection**, then **Wash selected**: wash just the ticked items.
- The ✓ button on each row washes a single item.

Washing an item resets its *wears since wash* to 0, clears the flag and sets *last washed* to today. When the basket is empty you'll see *"Nothing needs washing"*.

---

## Outfits

**Outfits** lists every outfit you have created, newest first. Each card shows the outfit's occasion, seasons, item thumbnails, description, star rating (with the number of ratings) and comment count.

### Creating an outfit

Click **Create Outfit** and fill in:

- **Name** (required) and **Description**
- **Occasion**: Casual, Formal, Business, Sport, Date Night, Party or Other
- **Seasons**: any of Spring, Summer, Fall, Winter. The planner uses these to match outfits to the forecast temperature.
- **Items**: tick the items to include. Only in-rotation items are offered.

### Filtering

Search by name/description, and filter by season, occasion, or a specific item ("which outfits use this shirt?"). **Clear** resets the filters.

---

## Outfit details

Click an outfit to open it.

| Panel | What you can do |
|---|---|
| **Items in this Outfit** | Thumbnails of each item, linking to the item. As the owner you can **add** an item from the dropdown or **remove** one. |
| **Wear Tracking** | (Owner only, for recording) Pick a date (defaults to today) and click **Record Wear**. This logs the outfit as worn on that date **and** records one wear on every item in it, which may flag items for washing. The panel shows wears this month, wears this season, and the 10 most recent wear dates. |
| **Ratings** | Give 1–5 stars under any of **Personal**, **Spouse/Partner** and **Friends/Other**, then **Submit Ratings**. Each person has one rating per category per outfit; submitting again updates it. The page shows each category's average and an **Overall Average**, which is the mean of the three category averages. |
| **Comments** | Post a comment. You can delete your own comments. |
| **Share Outfit** | (Owner only) Share with another user by username. |
| **Delete Outfit** | (Owner only) Permanently deletes the outfit with its ratings and comments. The items themselves are not deleted. |

The rating categories are perspectives, not identities: anyone who can open the outfit can rate in any category. A typical use is to ask your partner what they think and enter their score under *Spouse/Partner*, or to share the outfit with them so they can rate it themselves.

---

## Weekly planner

**Planner** shows one card per day, Monday to Sunday, each with a suggested outfit.

### How the plan is made

The first time you open a week, Trousseau picks an outfit for each day and **saves** the plan. It favours:

1. **Fresh outfits**: items that haven't been worn much since their last wash.
2. **Colour variety**: avoiding consecutive days with similar colour families.
3. **The weather** (if you've set a location in Profile): outfits whose season tags suit the forecast temperature, and outfits with outerwear on rainy days.

The full scoring is in [How it works](how-it-works.md#weekly-planner). Each day card shows the outfit's items, and, when weather is on, the forecast high and a rain indicator.

### Changing the plan

- **Change one day**: choose an outfit in the day's dropdown and click ✓. The day is marked as your choice.
- **Clear a day**: the × button empties it.
- **Regenerate**: discards the whole week, **including your manual picks**, and suggests a fresh plan (asks for confirmation). Ties are broken randomly, so regenerating usually gives a different plan.

### Recording what you wore

When you wear the planned outfit, click the calendar-plus button on that day. This logs a wear for the outfit on that date and counts a wear on each item. The day is then marked as worn.

### Navigating weeks

Use ‹ and › to move between weeks, and **This week** to return. Today's card is highlighted.

### Sunday email

Every **Sunday at 17:00** (server time), every user with an email address receives next week's plan by email. The email sends the **same saved plan** you see in the planner. If next week has no plan yet, one is generated and saved at that moment. Email delivery must be set up by whoever runs the server.

### Weather notes

- Weather is **off** until you set coordinates in [Profile](#profile).
- Forecasts cover about **16 days ahead**. Weeks further out, past weeks, and periods when the server can't reach the internet are planned without weather, and the page tells you so.

---

## Insights

**Insights** turns your wear history into reports. It is read-only.

| Section | Shows |
|---|---|
| **Headline numbers** | **Clothing Items** (in rotation), **Outfits**, **Item Wears**, **Outfits Logged**, and **Tracked Spend** (sum of purchase prices) |
| **Wear Activity** | Bar chart of outfit wears logged per month over the last 6 months |
| **Most Worn Items** | Top 10 items by wear count |
| **Most Worn Outfits** | Top 10 outfits by number of logged wears |
| **Never Worn** | Up to 10 in-rotation items with zero wears, oldest first |
| **Gathering Dust** | Up to 10 items worn before but not in the last **90 days** |
| **Wears by Category** | Items, total wears and wears-per-item for each category |
| **Cost Per Wear: Best / Worst Value** | The 10 cheapest and 10 most expensive priced items per wear. Retired items **are** included here on purpose. |

If no item has a purchase price, the cost-per-wear section prompts you to add some. Prices can be added on each item's detail page.

Note that *Item Wears* and *Outfits Logged* measure different things: wearing a 4-item outfit once adds 1 to the second and 4 to the first. See [How it works → Insights](how-it-works.md#insights) for exact definitions.

---

## Sharing

You can share any item or outfit with another Trousseau user by entering their **username** on its detail page. You can't share the same thing with the same person twice.

The **Shared** page has two tabs:

- **Shared With Me**: things others have shared with you, with the owner's name, the date, and a link to open each one.
- **Shared By Me**: things you've shared and who you shared them with. **Unshare** removes the share.

What the other person can do:

| Shared | They can | Only you can |
|---|---|---|
| **Outfit** | See it with its item photos, **rate** it and **comment** on it, and open each item in it | Add or remove items, record wears, share it further, delete it |
| **Item** | See its photo (and download it), details, wear stats and tags | Everything else. Its price, cost per wear and receipt stay private. |

Nobody can open your items or outfits unless you've shared them, even if they guess the address. Unsharing takes effect immediately.

---

## Profile

| Section | Purpose |
|---|---|
| **Profile Information** | Change your display name and email. |
| **Location** | Optional. Enter **latitude**, **longitude** and a label (e.g. "Home"). This switches on weather-aware planning. To find coordinates, right-click a spot in most map apps and copy them. Latitude must be −90…90 and longitude −180…180. **Clear** turns weather off again. Forecasts come from [Open-Meteo](https://open-meteo.com), which needs no account. |
| **Change Password** | Enter your current password and the new one twice. You stay signed in here, and **every other device is signed out**. Use this if you think someone else has your password. |

---

## Data export and import

The **Data** page lets you back up and move your wardrobe.

### Export

**Download Export** produces `trousseau-export-<username>-<date>.json` containing:

- all your tags
- all your clothing items, **including retired ones**, with photos and receipts (base64-encoded)
- all your outfits (with their items and seasons)
- your outfit wear history

Not included: ratings, comments, shares, saved planner weeks, and your profile/location settings.

Because photos are embedded, the file can be large.

### Import

Choose a previously exported `.json` file and click **Import**. Everything in the file is **added** to your account:

- Tags are matched by name. An existing tag with the same name is reused.
- Items, outfits and wear logs are always **created new**. Importing the same file twice gives you duplicates.

Import works with exports from older versions of Trousseau. Details are in [Export format](export-format.md).

---

## Things to know

- **Wear dates on items.** Recording an outfit wear for a past date logs the outfit on that date, but each item's *last worn* is set to **today**.
- **Wash threshold 0** means "never flag for washing".
- **Deleting items that are in an outfit, or that have been shared, fails** with an error in the current version. Remove the item from its outfits and unshare it first, or retire it instead.
- **Deleting an outfit that has been worn, planned or shared also fails** in the current version. The planner saves a plan for every week you open, so most outfits end up planned. See [Development → Known bugs](development.md#known-bugs).
- **Editing item attributes.** Only purchase info, tags, receipt and lifecycle can be changed after an item is created.
