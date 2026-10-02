package com.trousseau.service;

import com.trousseau.dao.ClothingItemDao;
import com.trousseau.dao.TagDao;
import com.trousseau.model.ClothingItem;
import com.trousseau.model.ClothingItemSummary;
import com.trousseau.model.ItemStatus;
import com.trousseau.model.Tag;
import com.trousseau.model.User;
import com.trousseau.util.ImageUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Stateless
public class ClothingItemService {

    @Inject
    private ClothingItemDao clothingItemDao;

    @Inject
    private TagDao tagDao;

    public ClothingItem addItem(ClothingItem item) {
        ensureThumbnail(item);
        return clothingItemDao.save(item);
    }

    public ClothingItem save(ClothingItem item) {
        ensureThumbnail(item);
        return clothingItemDao.save(item);
    }

    /**
     * Attaches the photo plus a generated thumbnail. The single place uploads should
     * go through, so no caller can add an image and forget the scaled copy.
     */
    public ClothingItem attachImage(ClothingItem item, byte[] data, String contentType, String fileName) {
        item.setImageData(data);
        item.setImageContentType(contentType);
        item.setImageName(fileName);
        item.setThumbnailData(ImageUtil.createThumbnail(data));
        return item;
    }

    /** Builds the thumbnail if there is a photo but no scaled copy yet. */
    private void ensureThumbnail(ClothingItem item) {
        if (item.getImageData() != null && item.getImageData().length > 0 && !item.hasThumbnail()) {
            item.setThumbnailData(ImageUtil.createThumbnail(item.getImageData()));
        }
    }

    /**
     * Thumbnail bytes for an item, generating and storing them on first request when
     * the item predates thumbnail support. Returns null when there is no photo at all;
     * callers fall back to the original image.
     */
    public byte[] getThumbnailData(Long id) {
        byte[] thumb = clothingItemDao.loadThumbnailData(id);
        if (thumb != null && thumb.length > 0) {
            return thumb;
        }
        // Backfill: items catalogued before thumbnails existed get one the first time
        // a grid asks for it, rather than needing a migration pass.
        byte[] original = clothingItemDao.loadImageData(id);
        if (original == null || original.length == 0) {
            return null;
        }
        byte[] generated = ImageUtil.createThumbnail(original);
        if (generated != null) {
            ClothingItem item = clothingItemDao.findById(id);
            if (item != null) {
                item.setThumbnailData(generated);
                clothingItemDao.update(item);
            }
            return generated;
        }
        // Small enough that a thumbnail would be pointless — serve the original.
        return original;
    }

    public ClothingItem update(ClothingItem item) {
        return clothingItemDao.update(item);
    }

    public void delete(ClothingItem item) {
        clothingItemDao.delete(item);
    }

    public List<ClothingItem> findByTag(Tag tag) {
        return clothingItemDao.findByTag(tag);
    }

    public ClothingItem findById(Long id) {
        return clothingItemDao.findById(id);
    }

    public List<ClothingItem> findByOwner(User owner) {
        return clothingItemDao.findByOwner(owner);
    }

    public List<ClothingItemSummary> findSummariesByOwner(User owner) {
        return clothingItemDao.findSummariesByOwner(owner);
    }

    public List<ClothingItem> findByCategory(User owner, String category) {
        return clothingItemDao.findByOwnerAndCategory(owner, category);
    }

    public List<ClothingItem> findNeedingWash(User owner) {
        return clothingItemDao.findNeedingWash(owner);
    }

    public ClothingItem updateItem(ClothingItem item) {
        return clothingItemDao.update(item);
    }

    public void deleteItem(ClothingItem item) {
        clothingItemDao.delete(item);
    }

    public ClothingItem recordWear(ClothingItem item) {
        item.recordWear();
        return clothingItemDao.update(item);
    }

    public ClothingItem markWashed(ClothingItem item) {
        item.markWashed();
        return clothingItemDao.update(item);
    }

    /**
     * Marks a whole basket washed in one transaction. Doing them one at a time through
     * the UI was the single most repetitive thing in the app.
     *
     * @return how many items were updated
     */
    public int markWashedBatch(List<ClothingItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ClothingItem item : items) {
            if (item == null) continue;
            item.markWashed();
            clothingItemDao.update(item);
            count++;
        }
        return count;
    }

    /** Everything currently flagged for washing, for the laundry basket. */
    public List<ClothingItem> findLaundryBasket(User owner) {
        return clothingItemDao.findNeedingWash(owner);
    }

    // --- lifecycle -----------------------------------------------------------

    public ClothingItem retire(ClothingItem item, ItemStatus status, String note) {
        item.retire(status, note);
        return clothingItemDao.update(item);
    }

    public ClothingItem reactivate(ClothingItem item) {
        item.reactivate();
        return clothingItemDao.update(item);
    }

    public List<ClothingItem> findRetired(User owner) {
        return clothingItemDao.findRetiredByOwner(owner);
    }

    public List<ClothingItem> findAllIncludingRetired(User owner) {
        return clothingItemDao.findAllByOwner(owner);
    }

    /** Statuses offered when retiring an item — everything except ACTIVE. */
    public List<ItemStatus> getRetirementStatuses() {
        return Arrays.asList(ItemStatus.ARCHIVED, ItemStatus.DONATED, ItemStatus.SOLD);
    }

    public byte[] getImageData(Long id) {
        return clothingItemDao.loadImageData(id);
    }

    public ClothingItem saveReceipt(ClothingItem item, byte[] data, String contentType, String fileName) {
        item.setReceiptData(data);
        item.setReceiptContentType(contentType);
        item.setReceiptName(fileName);
        return clothingItemDao.update(item);
    }

    public ClothingItem deleteReceipt(ClothingItem item) {
        item.setReceiptData(null);
        item.setReceiptContentType(null);
        item.setReceiptName(null);
        return clothingItemDao.update(item);
    }

    public byte[] getReceiptData(Long id) {
        return clothingItemDao.loadReceiptData(id);
    }

    public ClothingItem addTagToItem(ClothingItem item, Tag tag) {
        item.getTags().add(tag);
        return clothingItemDao.update(item);
    }

    public ClothingItem removeTagFromItem(ClothingItem item, Tag tag) {
        item.getTags().remove(tag);
        return clothingItemDao.update(item);
    }

    public List<String> getCategories() {
        return Arrays.asList(
                "Tops",
                "Bottoms",
                "Dresses",
                "Outerwear",
                "Shoes",
                "Accessories",
                "Activewear",
                "Sleepwear",
                "Swimwear",
                "Formal",
                "Underwear",
                "Other"
        );
    }
}
