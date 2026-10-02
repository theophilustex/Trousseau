package com.trousseau.service;

import com.trousseau.dao.ClothingItemDao;
import com.trousseau.dao.OutfitDao;
import com.trousseau.dao.OutfitWearLogDao;
import com.trousseau.dao.TagDao;
import com.trousseau.model.*;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class DataExportImportService {

    private static final Logger LOG = Logger.getLogger(DataExportImportService.class.getName());
    private static final String VERSION = "3";

    @Inject private ClothingItemService clothingItemService;
    @Inject private ClothingItemDao clothingItemDao;
    @Inject private OutfitService outfitService;
    @Inject private OutfitDao outfitDao;
    @Inject private TagService tagService;
    @Inject private TagDao tagDao;
    @Inject private OutfitWearLogDao outfitWearLogDao;
    @Inject private UserService userService;

    // -------------------------------------------------------------------------
    // EXPORT
    // -------------------------------------------------------------------------

    public String exportUserData(User user) {
        JsonObjectBuilder root = Json.createObjectBuilder()
                .add("version", VERSION)
                .add("exportDate", LocalDate.now().toString())
                .add("username", user.getUsername());

        // Tags
        JsonArrayBuilder tagsArr = Json.createArrayBuilder();
        for (Tag tag : tagService.findByOwner(user)) {
            tagsArr.add(Json.createObjectBuilder()
                    .add("id", tag.getId())
                    .add("name", tag.getName()));
        }
        root.add("tags", tagsArr);

        // Clothing items — load imageData separately to avoid lazy issues
        JsonArrayBuilder itemsArr = Json.createArrayBuilder();
        for (ClothingItem item : clothingItemService.findAllIncludingRetired(user)) {
            JsonObjectBuilder ib = Json.createObjectBuilder()
                    .add("id", item.getId())
                    .add("name", item.getName())
                    .add("category", nullSafe(item.getCategory()))
                    .add("color", nullSafe(item.getColor()))
                    .add("brand", nullSafe(item.getBrand()))
                    .add("size", nullSafe(item.getSize()))
                    .add("description", nullSafe(item.getDescription()))
                    .add("wearCount", item.getWearCount())
                    .add("wearsSinceWash", item.getWearsSinceWash())
                    .add("washAfterWears", item.getWashAfterWears())
                    .add("needsWash", item.isNeedsWash())
                    .add("lastWornDate", item.getLastWornDate() != null ? item.getLastWornDate().toString() : "")
                    .add("lastWashedDate", item.getLastWashedDate() != null ? item.getLastWashedDate().toString() : "")
                    // Money as a string, not a JSON number: binary floats do not round-trip currency.
                    .add("purchasePrice", item.getPurchasePrice() != null ? item.getPurchasePrice().toPlainString() : "")
                    .add("purchaseDate", item.getPurchaseDate() != null ? item.getPurchaseDate().toString() : "")
                    .add("status", item.getStatus().name())
                    .add("retiredOn", item.getRetiredOn() != null ? item.getRetiredOn().toString() : "")
                    .add("retiredNote", nullSafe(item.getRetiredNote()))
                    .add("imageName", nullSafe(item.getImageName()))
                    .add("imageContentType", nullSafe(item.getImageContentType()))
                    .add("receiptName", nullSafe(item.getReceiptName()))
                    .add("receiptContentType", nullSafe(item.getReceiptContentType()));

            // Tags on this item
            JsonArrayBuilder itemTagIds = Json.createArrayBuilder();
            for (Tag t : item.getTags()) {
                itemTagIds.add(t.getId());
            }
            ib.add("tagIds", itemTagIds);

            // Image data as Base64
            try {
                byte[] img = clothingItemService.getImageData(item.getId());
                if (img != null) {
                    ib.add("imageData", Base64.getEncoder().encodeToString(img));
                } else {
                    ib.add("imageData", "");
                }
            } catch (Exception e) {
                ib.add("imageData", "");
            }

            // Receipt data as Base64
            try {
                byte[] receipt = clothingItemService.getReceiptData(item.getId());
                if (receipt != null) {
                    ib.add("receiptData", Base64.getEncoder().encodeToString(receipt));
                } else {
                    ib.add("receiptData", "");
                }
            } catch (Exception e) {
                ib.add("receiptData", "");
            }

            itemsArr.add(ib);
        }
        root.add("clothingItems", itemsArr);

        // Outfits
        JsonArrayBuilder outfitsArr = Json.createArrayBuilder();
        for (Outfit outfit : outfitService.findByCreator(user)) {
            JsonObjectBuilder ob = Json.createObjectBuilder()
                    .add("id", outfit.getId())
                    .add("name", outfit.getName())
                    .add("description", nullSafe(outfit.getDescription()))
                    .add("occasion", nullSafe(outfit.getOccasion()));

            JsonArrayBuilder seasons = Json.createArrayBuilder();
            for (String s : outfit.getSeasons()) seasons.add(s);
            ob.add("seasons", seasons);

            JsonArrayBuilder itemIds = Json.createArrayBuilder();
            for (ClothingItem ci : outfit.getItems()) itemIds.add(ci.getId());
            ob.add("itemIds", itemIds);

            outfitsArr.add(ob);
        }
        root.add("outfits", outfitsArr);

        // Wear logs
        JsonArrayBuilder logsArr = Json.createArrayBuilder();
        List<OutfitWearLog> logs = outfitWearLogDao.findByUserAndDateRange(
                user, LocalDate.of(2000, 1, 1), LocalDate.now());
        for (OutfitWearLog log : logs) {
            logsArr.add(Json.createObjectBuilder()
                    .add("outfitId", log.getOutfit().getId())
                    .add("wornDate", log.getWornDate().toString()));
        }
        root.add("wearLogs", logsArr);

        StringWriter sw = new StringWriter();
        try (JsonWriter jw = Json.createWriterFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(sw)) {
            jw.write(root.build());
        }
        return sw.toString();
    }

    // -------------------------------------------------------------------------
    // IMPORT
    // -------------------------------------------------------------------------

    /** Returns a summary message of what was imported. */
    public String importUserData(User user, String json) {
        JsonObject root;
        try (JsonReader jr = Json.createReader(new StringReader(json))) {
            root = jr.readObject();
        }

        int tagsCreated = 0, itemsCreated = 0, outfitsCreated = 0, logsCreated = 0;

        // --- Tags ---
        // Map: exported tag id → new Tag entity
        Map<Long, Tag> tagMap = new HashMap<>();
        if (root.containsKey("tags")) {
            for (JsonValue v : root.getJsonArray("tags")) {
                JsonObject jt = (JsonObject) v;
                long exportedId = jt.getJsonNumber("id").longValue();
                String name = jt.getString("name");
                Tag tag = tagService.findOrCreate(user, name);
                tagMap.put(exportedId, tag);
                tagsCreated++;
            }
        }

        // --- Clothing items ---
        // Map: exported item id → new ClothingItem entity
        Map<Long, ClothingItem> itemMap = new HashMap<>();
        if (root.containsKey("clothingItems")) {
            for (JsonValue v : root.getJsonArray("clothingItems")) {
                JsonObject ji = (JsonObject) v;
                long exportedId = ji.getJsonNumber("id").longValue();

                ClothingItem item = new ClothingItem();
                item.setOwner(user);
                item.setName(ji.getString("name"));
                item.setCategory(emptyToNull(ji.getString("category")));
                item.setColor(emptyToNull(ji.getString("color")));
                item.setBrand(emptyToNull(ji.getString("brand")));
                item.setSize(emptyToNull(ji.getString("size")));
                item.setDescription(emptyToNull(ji.getString("description")));
                item.setWearCount(ji.getInt("wearCount", 0));
                item.setWearsSinceWash(ji.getInt("wearsSinceWash", 0));
                item.setWashAfterWears(ji.getInt("washAfterWears", 3));
                item.setNeedsWash(ji.getBoolean("needsWash", false));
                item.setImageName(emptyToNull(ji.getString("imageName", "")));
                item.setImageContentType(emptyToNull(ji.getString("imageContentType", "")));
                item.setReceiptName(emptyToNull(ji.getString("receiptName", "")));
                item.setReceiptContentType(emptyToNull(ji.getString("receiptContentType", "")));

                String lastWorn = ji.getString("lastWornDate", "");
                if (!lastWorn.isEmpty()) item.setLastWornDate(LocalDate.parse(lastWorn));

                String lastWashed = ji.getString("lastWashedDate", "");
                if (!lastWashed.isEmpty()) item.setLastWashedDate(LocalDate.parse(lastWashed));

                // Absent in version 1 exports; the defaults keep those files importable.
                String price = ji.getString("purchasePrice", "");
                if (!price.isEmpty()) item.setPurchasePrice(new BigDecimal(price));

                String purchased = ji.getString("purchaseDate", "");
                if (!purchased.isEmpty()) item.setPurchaseDate(LocalDate.parse(purchased));

                // Absent before version 3; those items import as ACTIVE, which is right.
                String status = ji.getString("status", "");
                if (!status.isEmpty()) {
                    try {
                        item.setStatus(ItemStatus.valueOf(status));
                    } catch (IllegalArgumentException e) {
                        LOG.warning("Unknown item status '" + status + "' in import; defaulting to ACTIVE");
                    }
                }
                String retiredOn = ji.getString("retiredOn", "");
                if (!retiredOn.isEmpty()) item.setRetiredOn(LocalDate.parse(retiredOn));
                item.setRetiredNote(emptyToNull(ji.getString("retiredNote", "")));

                String imgB64 = ji.getString("imageData", "");
                if (!imgB64.isEmpty()) {
                    // The thumbnail is derived, so it is regenerated on save rather than
                    // carried in the file — that keeps exports smaller and self-healing.
                    item.setImageData(Base64.getDecoder().decode(imgB64));
                }

                String receiptB64 = ji.getString("receiptData", "");
                if (!receiptB64.isEmpty()) item.setReceiptData(Base64.getDecoder().decode(receiptB64));

                // Resolve tags
                if (ji.containsKey("tagIds")) {
                    Set<Tag> tags = new HashSet<>();
                    for (JsonValue tv : ji.getJsonArray("tagIds")) {
                        long tid = ((JsonNumber) tv).longValue();
                        Tag t = tagMap.get(tid);
                        if (t != null) tags.add(t);
                    }
                    item.setTags(tags);
                }

                ClothingItem saved = clothingItemService.addItem(item);
                itemMap.put(exportedId, saved);
                itemsCreated++;
            }
        }

        // --- Outfits ---
        Map<Long, Outfit> outfitMap = new HashMap<>();
        if (root.containsKey("outfits")) {
            for (JsonValue v : root.getJsonArray("outfits")) {
                JsonObject jo = (JsonObject) v;
                long exportedId = jo.getJsonNumber("id").longValue();

                Outfit outfit = new Outfit();
                outfit.setCreator(user);
                outfit.setName(jo.getString("name"));
                outfit.setDescription(emptyToNull(jo.getString("description", "")));
                outfit.setOccasion(emptyToNull(jo.getString("occasion", "")));

                Set<String> seasons = new HashSet<>();
                if (jo.containsKey("seasons")) {
                    for (JsonValue sv : jo.getJsonArray("seasons")) {
                        seasons.add(((JsonString) sv).getString());
                    }
                }
                outfit.setSeasons(seasons);

                List<ClothingItem> items = new ArrayList<>();
                if (jo.containsKey("itemIds")) {
                    for (JsonValue iv : jo.getJsonArray("itemIds")) {
                        long iid = ((JsonNumber) iv).longValue();
                        ClothingItem ci = itemMap.get(iid);
                        if (ci != null) items.add(ci);
                    }
                }
                outfit.setItems(items);

                Outfit saved = outfitService.createOutfit(outfit);
                outfitMap.put(exportedId, saved);
                outfitsCreated++;
            }
        }

        // --- Wear logs ---
        if (root.containsKey("wearLogs")) {
            for (JsonValue v : root.getJsonArray("wearLogs")) {
                JsonObject jl = (JsonObject) v;
                long outfitId = jl.getJsonNumber("outfitId").longValue();
                Outfit outfit = outfitMap.get(outfitId);
                if (outfit == null) continue;

                LocalDate date = LocalDate.parse(jl.getString("wornDate"));
                OutfitWearLog log = new OutfitWearLog();
                log.setOutfit(outfit);
                log.setUser(user);
                log.setWornDate(date);
                outfitWearLogDao.save(log);
                logsCreated++;
            }
        }

        return String.format("Import complete: %d tag(s), %d clothing item(s), %d outfit(s), %d wear log(s).",
                tagsCreated, itemsCreated, outfitsCreated, logsCreated);
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
