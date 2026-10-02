package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.User;
import com.trousseau.service.ClothingItemService;
import com.trousseau.util.ImageUtil;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

/**
 * Photo-first bulk cataloguing.
 *
 * <p>Adding a wardrobe one modal dialog at a time is the thing that stops people ever
 * finishing. Here you drop in every photo at once, each becomes a draft row with its
 * name guessed from the filename, and you fill in the details in a single table before
 * saving the lot.</p>
 *
 * <p>Drafts live in view state until saved, so nothing is written until you commit —
 * which also means the photos sit in memory. {@link #MAX_DRAFTS} caps how many can be
 * queued at once so a stray multi-select cannot exhaust the heap.</p>
 */
@Named
@ViewScoped
public class BulkAddBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Upper bound on queued drafts, to keep the photos in view state bounded. */
    public static final int MAX_DRAFTS = 40;

    @Inject
    private ClothingItemService clothingItemService;

    @Inject
    private SessionBean sessionBean;

    private List<Draft> drafts = new ArrayList<>();
    private List<String> categories;

    /** Applied to every draft at once by the "apply to all" controls. */
    private String bulkCategory;
    private String bulkBrand;
    private Integer bulkWashAfterWears;

    private int savedCount;

    @PostConstruct
    public void init() {
        categories = clothingItemService.getCategories();
    }

    /** One row per uploaded photo. */
    public void handleUpload(FileUploadEvent event) {
        UploadedFile file = event.getFile();
        if (file == null || file.getContent() == null || file.getContent().length == 0) {
            return;
        }
        if (drafts.size() >= MAX_DRAFTS) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Queue is full at " + MAX_DRAFTS + " photos — save these first, then add more", null));
            return;
        }

        Draft draft = new Draft();
        draft.id = System.nanoTime() + ":" + file.getFileName();
        draft.name = prettifyFileName(file.getFileName());
        draft.fileName = file.getFileName();
        draft.contentType = file.getContentType();
        draft.imageData = file.getContent();
        // Generate now, while the bytes are already in hand.
        draft.thumbnailData = ImageUtil.createThumbnail(file.getContent());
        draft.washAfterWears = 3;
        drafts.add(draft);
    }

    /**
     * "IMG_2481 navy wool coat.jpg" becomes "Navy wool coat" — a starting point that
     * is usually quicker to edit than to type from scratch.
     */
    private String prettifyFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "Untitled item";
        }
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        base = base.replaceAll("[_-]+", " ").replaceAll("\\s+", " ").trim();
        // Drop a leading camera prefix like "IMG 2481" or "DSC 0031".
        base = base.replaceFirst("(?i)^(img|dsc|photo|pxl)\\s*\\d+\\s*", "").trim();
        if (base.isEmpty()) {
            return "Untitled item";
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    public void removeDraft(String draftId) {
        for (Iterator<Draft> it = drafts.iterator(); it.hasNext(); ) {
            if (it.next().id.equals(draftId)) {
                it.remove();
                return;
            }
        }
    }

    public void clearDrafts() {
        drafts = new ArrayList<>();
    }

    /** Copies the "apply to all" values onto every draft that has none set. */
    public void applyToAll() {
        for (Draft d : drafts) {
            if (bulkCategory != null && !bulkCategory.isEmpty()) {
                d.category = bulkCategory;
            }
            if (bulkBrand != null && !bulkBrand.trim().isEmpty()) {
                d.brand = bulkBrand.trim();
            }
            if (bulkWashAfterWears != null && bulkWashAfterWears > 0) {
                d.washAfterWears = bulkWashAfterWears;
            }
        }
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Applied to " + drafts.size() + (drafts.size() == 1 ? " draft" : " drafts"), null));
    }

    /** Writes every draft as a clothing item. */
    public void saveAll() {
        if (drafts.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Add some photos first", null));
            return;
        }

        User owner = sessionBean.getCurrentUser();
        int saved = 0;
        List<String> skipped = new ArrayList<>();

        for (Draft d : drafts) {
            if (d.name == null || d.name.trim().isEmpty()) {
                skipped.add(d.fileName);
                continue;
            }
            ClothingItem item = new ClothingItem();
            item.setOwner(owner);
            item.setName(d.name.trim());
            item.setCategory(emptyToNull(d.category));
            item.setColor(emptyToNull(d.color));
            item.setBrand(emptyToNull(d.brand));
            item.setSize(emptyToNull(d.size));
            item.setWashAfterWears(d.washAfterWears > 0 ? d.washAfterWears : 3);
            item.setPurchasePrice(d.purchasePrice);
            item.setImageData(d.imageData);
            item.setImageContentType(d.contentType);
            item.setImageName(d.fileName);
            item.setThumbnailData(d.thumbnailData);

            clothingItemService.addItem(item);
            saved++;
        }

        savedCount = saved;
        drafts = new ArrayList<>();

        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Added " + saved + (saved == 1 ? " item" : " items") + " to your wardrobe", null));
        if (!skipped.isEmpty()) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Skipped " + skipped.size() + " without a name: " + String.join(", ", skipped), null));
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    public boolean isHasDrafts() {
        return !drafts.isEmpty();
    }

    public int getDraftCount() {
        return drafts.size();
    }

    public int getMaxDrafts() {
        return MAX_DRAFTS;
    }

    public List<Draft> getDrafts() { return drafts; }
    public List<String> getCategories() { return categories; }
    public int getSavedCount() { return savedCount; }

    public String getBulkCategory() { return bulkCategory; }
    public void setBulkCategory(String bulkCategory) { this.bulkCategory = bulkCategory; }
    public String getBulkBrand() { return bulkBrand; }
    public void setBulkBrand(String bulkBrand) { this.bulkBrand = bulkBrand; }
    public Integer getBulkWashAfterWears() { return bulkWashAfterWears; }
    public void setBulkWashAfterWears(Integer v) { this.bulkWashAfterWears = v; }

    /**
     * An unsaved item. Holds its own photo bytes because nothing is written until
     * {@link #saveAll()}, so there is no row to stream an image from yet.
     */
    public static class Draft implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String category;
        private String color;
        private String brand;
        private String size;
        private int washAfterWears = 3;
        private BigDecimal purchasePrice;

        private String fileName;
        private String contentType;
        private byte[] imageData;
        private byte[] thumbnailData;

        /** Data URI so the preview renders without a round trip to a not-yet-saved row. */
        public String getPreviewDataUri() {
            byte[] source = (thumbnailData != null && thumbnailData.length > 0) ? thumbnailData : imageData;
            if (source == null || source.length == 0) {
                return null;
            }
            String type = (thumbnailData != null && thumbnailData.length > 0)
                    ? ImageUtil.THUMBNAIL_CONTENT_TYPE
                    : (contentType == null ? "image/jpeg" : contentType);
            return "data:" + type + ";base64," + java.util.Base64.getEncoder().encodeToString(source);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public int getWashAfterWears() { return washAfterWears; }
        public void setWashAfterWears(int v) { this.washAfterWears = v; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
        public String getFileName() { return fileName; }
    }
}
