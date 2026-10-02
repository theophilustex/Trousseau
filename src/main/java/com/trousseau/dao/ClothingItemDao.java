package com.trousseau.dao;

import com.trousseau.model.CategoryWearStat;
import com.trousseau.model.ClothingItem;
import com.trousseau.model.ClothingItemSummary;
import com.trousseau.model.ItemWearStat;
import com.trousseau.model.Tag;
import com.trousseau.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ClothingItemDao {

    @PersistenceContext
    private EntityManager em;

    public ClothingItem save(ClothingItem item) {
        if (item.getId() == null) {
            em.persist(item);
            return item;
        } else {
            return em.merge(item);
        }
    }

    public ClothingItem findById(Long id) {
        List<ClothingItem> results = em.createQuery(
                "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags WHERE c.id = :id", ClothingItem.class)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<ClothingItem> findByOwner(User owner) {
        return em.createNamedQuery("ClothingItem.findByOwner", ClothingItem.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public List<ClothingItem> findByOwnerAndCategory(User owner, String category) {
        return em.createNamedQuery("ClothingItem.findByOwnerAndCategory", ClothingItem.class)
                .setParameter("owner", owner)
                .setParameter("category", category)
                .getResultList();
    }

    public List<ClothingItem> findNeedingWash(User owner) {
        return em.createNamedQuery("ClothingItem.findNeedingWash", ClothingItem.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    /** Every item the user owns, retired ones included. */
    public List<ClothingItem> findAllByOwner(User owner) {
        return em.createNamedQuery("ClothingItem.findAllByOwner", ClothingItem.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    /** Items no longer in rotation, most recently retired first. */
    public List<ClothingItem> findRetiredByOwner(User owner) {
        return em.createNamedQuery("ClothingItem.findRetiredByOwner", ClothingItem.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public byte[] loadThumbnailData(Long id) {
        return em.createQuery("SELECT c.thumbnailData FROM ClothingItem c WHERE c.id = :id", byte[].class)
                .setParameter("id", id)
                .getSingleResult();
    }

    public List<ClothingItem> findByTag(Tag tag) {
        return em.createNamedQuery("ClothingItem.findByTag", ClothingItem.class)
                .setParameter("tag", tag)
                .getResultList();
    }

    public ClothingItem update(ClothingItem item) {
        return em.merge(item);
    }

    public void delete(ClothingItem item) {
        em.remove(em.merge(item));
    }

    /** Pickers only offer items you could actually wear, so retired ones are excluded. */
    public List<ClothingItemSummary> findSummariesByOwner(User owner) {
        return em.createQuery(
                "SELECT NEW com.trousseau.model.ClothingItemSummary(c.id, c.name, c.category) " +
                "FROM ClothingItem c WHERE c.owner = :owner " +
                "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE) ORDER BY c.name",
                ClothingItemSummary.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public byte[] loadImageData(Long id) {
        return em.createQuery("SELECT c.imageData FROM ClothingItem c WHERE c.id = :id", byte[].class)
                .setParameter("id", id)
                .getSingleResult();
    }

    public byte[] loadReceiptData(Long id) {
        return em.createQuery("SELECT c.receiptData FROM ClothingItem c WHERE c.id = :id", byte[].class)
                .setParameter("id", id)
                .getSingleResult();
    }

    // -------------------------------------------------------------------------
    // Insights queries
    //
    // These all project into ItemWearStat / CategoryWearStat rather than returning
    // entities: a wardrobe-wide report that loaded ClothingItem rows would drag every
    // image and receipt BLOB into memory with it.
    // -------------------------------------------------------------------------

    private static final String ITEM_STAT_SELECT =
            "SELECT NEW com.trousseau.model.ItemWearStat("
          + "c.id, c.name, c.category, c.wearCount, c.lastWornDate, c.purchasePrice) "
          + "FROM ClothingItem c WHERE c.owner = :owner ";

    /**
     * Restricts a stat query to items still in rotation. Retired items are excluded
     * from the "you are not wearing this" reports — you already decided that — but
     * deliberately kept in the cost-per-wear ranking, where a retired item's final
     * cost per wear is the most instructive number there is.
     */
    private static final String IN_ROTATION =
            "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE) ";

    /** Most-worn items first. */
    public List<ItemWearStat> findMostWorn(User owner, int limit) {
        return em.createQuery(ITEM_STAT_SELECT + IN_ROTATION + "ORDER BY c.wearCount DESC, c.name", ItemWearStat.class)
                .setParameter("owner", owner)
                .setMaxResults(limit)
                .getResultList();
    }

    /** Items that have never been worn at all. */
    public List<ItemWearStat> findNeverWorn(User owner, int limit) {
        return em.createQuery(ITEM_STAT_SELECT + IN_ROTATION + "AND c.wearCount = 0 ORDER BY c.createdAt", ItemWearStat.class)
                .setParameter("owner", owner)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Items worn at least once, but not since the cutoff date. Never-worn items are
     * excluded deliberately so they are reported once, by {@link #findNeverWorn}.
     */
    public List<ItemWearStat> findNotWornSince(User owner, LocalDate cutoff, int limit) {
        return em.createQuery(
                ITEM_STAT_SELECT + IN_ROTATION + "AND c.wearCount > 0 AND c.lastWornDate < :cutoff "
                        + "ORDER BY c.lastWornDate", ItemWearStat.class)
                .setParameter("owner", owner)
                .setParameter("cutoff", cutoff)
                .setMaxResults(limit)
                .getResultList();
    }

    /** Every item that has a purchase price recorded; cost-per-wear is ranked in the service. */
    public List<ItemWearStat> findPricedItems(User owner) {
        return em.createQuery(
                ITEM_STAT_SELECT + "AND c.purchasePrice IS NOT NULL", ItemWearStat.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public List<CategoryWearStat> countWearsByCategory(User owner) {
        return em.createQuery(
                "SELECT NEW com.trousseau.model.CategoryWearStat(c.category, COUNT(c), SUM(c.wearCount)) "
                        + "FROM ClothingItem c WHERE c.owner = :owner "
                        + "GROUP BY c.category ORDER BY SUM(c.wearCount) DESC",
                CategoryWearStat.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    /** Items still in rotation. */
    public long countByOwner(User owner) {
        return em.createQuery("SELECT COUNT(c) FROM ClothingItem c WHERE c.owner = :owner "
                        + "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE)", Long.class)
                .setParameter("owner", owner)
                .getSingleResult();
    }

    /** Items taken out of rotation, by status. */
    public long countRetired(User owner) {
        return em.createQuery("SELECT COUNT(c) FROM ClothingItem c WHERE c.owner = :owner "
                        + "AND c.status IS NOT NULL AND c.status <> com.trousseau.model.ItemStatus.ACTIVE", Long.class)
                .setParameter("owner", owner)
                .getSingleResult();
    }

    /** Total lifetime wears across the wardrobe. Zero when the wardrobe is empty. */
    public long sumWearCount(User owner) {
        Long total = em.createQuery(
                "SELECT SUM(c.wearCount) FROM ClothingItem c WHERE c.owner = :owner", Long.class)
                .setParameter("owner", owner)
                .getSingleResult();
        return total == null ? 0L : total;
    }

    /** Total recorded spend. Null when no item has a price. */
    public BigDecimal sumPurchasePrice(User owner) {
        return em.createQuery(
                "SELECT SUM(c.purchasePrice) FROM ClothingItem c WHERE c.owner = :owner", BigDecimal.class)
                .setParameter("owner", owner)
                .getSingleResult();
    }
}
