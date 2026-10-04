# Export format

**Data → Download Export** produces a single UTF-8, pretty-printed JSON file named `trousseau-export-<username>-<yyyy-mm-dd>.json`. **Data → Import** reads the same format. Both are implemented in `DataExportImportService`.

The current format is **version 3**.

## Top level

```json
{
  "version": "3",
  "exportDate": "2026-10-03",
  "username": "alex",
  "tags":          [ … ],
  "clothingItems": [ … ],
  "outfits":       [ … ],
  "wearLogs":      [ … ]
}
```

| Field | Type | Notes |
|---|---|---|
| `version` | string | `"3"`. Informational only: import does not check it. |
| `exportDate` | string (ISO date) | Informational |
| `username` | string | Informational. Import always goes into the **signed-in** user's account. |
| `tags`, `clothingItems`, `outfits`, `wearLogs` | arrays | Each is optional on import |

The `id` values in the file are the **exporting** database's ids. They only link records within the file. On import every record gets a new id, and the links are remapped.

## `tags[]`

```json
{ "id": 4, "name": "work" }
```

## `clothingItems[]`

Retired items are included.

```json
{
  "id": 17,
  "name": "Navy wool coat",
  "category": "Outerwear",
  "color": "navy",
  "brand": "Acme",
  "size": "M",
  "description": "",
  "wearCount": 12,
  "wearsSinceWash": 1,
  "washAfterWears": 0,
  "needsWash": false,
  "lastWornDate": "2026-09-30",
  "lastWashedDate": "",
  "purchasePrice": "149.00",
  "purchaseDate": "2024-11-02",
  "status": "ACTIVE",
  "retiredOn": "",
  "retiredNote": "",
  "imageName": "IMG_2481 navy wool coat.jpg",
  "imageContentType": "image/jpeg",
  "receiptName": "",
  "receiptContentType": "",
  "tagIds": [4],
  "imageData": "<base64>",
  "receiptData": ""
}
```

| Field | Type | Notes |
|---|---|---|
| `name` | string | **Required** on import |
| `category`, `color`, `brand`, `size`, `description` | string | `""` means none. **Required keys** on import (they may be empty). |
| `wearCount`, `wearsSinceWash`, `washAfterWears` | int | Default to 0, 0, 3 if missing |
| `needsWash` | bool | Default false |
| `lastWornDate`, `lastWashedDate`, `purchaseDate`, `retiredOn` | string (ISO date) or `""` | |
| `purchasePrice` | **string** decimal or `""` | A string so currency values round-trip exactly (no binary float) |
| `status` | string | `ACTIVE`, `ARCHIVED`, `DONATED` or `SOLD`. Unknown values import as `ACTIVE` with a warning in the log. |
| `retiredNote` | string | |
| `imageName`, `imageContentType`, `receiptName`, `receiptContentType` | string | |
| `tagIds` | int[] | References `tags[].id` in the same file. Unknown ids are ignored. |
| `imageData`, `receiptData` | base64 string or `""` | Standard (not URL-safe) base64 |

Thumbnails are not exported. They are regenerated on import.

## `outfits[]`

```json
{
  "id": 9,
  "name": "Rainy commute",
  "description": "",
  "occasion": "Business",
  "seasons": ["Fall", "Winter"],
  "itemIds": [17, 21, 30]
}
```

`itemIds` references `clothingItems[].id` in the same file. Unknown ids are ignored. `name` is required. The other fields are optional.

## `wearLogs[]`

```json
{ "outfitId": 9, "wornDate": "2026-09-30" }
```

All the user's outfit wear-log entries dated from 2000-01-01 to the export date. `outfitId` references `outfits[].id`. Entries for unknown outfits are skipped.

## What is not exported

- Ratings and comments (on your outfits or anyone else's)
- Shares
- Saved planner weeks (`planned_outfit`)
- Profile: display name, email, location, password

## Import behaviour

Import runs in a single transaction. If anything throws (malformed JSON, a missing required key, an unparsable date), **nothing** is imported, and the error message is shown.

| Record | Behaviour |
|---|---|
| Tag | `findOrCreate` by name: an existing tag with the same name is reused |
| Clothing item | Always created new |
| Outfit | Always created new, linked to the newly created items |
| Wear log | Always created new. Item wear counters are **not** incremented again; they come from the item's own fields. |

Importing the same file twice therefore duplicates items, outfits and wear logs. The success message counts what was processed: `Import complete: N tag(s), N clothing item(s), N outfit(s), N wear log(s).`

### Compatibility with older exports

| Fields | Added in | Missing on import → |
|---|---|---|
| `purchasePrice`, `purchaseDate` | v2 | No price/date |
| `status`, `retiredOn`, `retiredNote` | v3 | Item is `ACTIVE` |

## Size

Photos and receipts are embedded as base64, which adds about 33% to their size. A wardrobe of 200 items with 3 MB photos makes an export of about 800 MB. Import reads the whole file into memory, so large imports need a matching JVM heap (`-Xmx`) and a reverse proxy that allows large request bodies.
