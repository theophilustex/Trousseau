package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clothing_item")
@NamedQueries({
    // The wardrobe views scope to items still in rotation. "In rotation" is
    // (status = ACTIVE OR status IS NULL): hbm2ddl adds the column to existing rows
    // as NULL, and those items must not vanish from the wardrobe on upgrade.
    @NamedQuery(name = "ClothingItem.findByOwner",
        query = "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags WHERE c.owner = :owner "
              + "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE) ORDER BY c.createdAt DESC"),
    @NamedQuery(name = "ClothingItem.findAllByOwner",
        query = "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags WHERE c.owner = :owner ORDER BY c.createdAt DESC"),
    @NamedQuery(name = "ClothingItem.findRetiredByOwner",
        query = "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags WHERE c.owner = :owner "
              + "AND c.status IS NOT NULL AND c.status <> com.trousseau.model.ItemStatus.ACTIVE ORDER BY c.retiredOn DESC"),
    @NamedQuery(name = "ClothingItem.findByOwnerAndCategory",
        query = "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags WHERE c.owner = :owner AND c.category = :category "
              + "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE) ORDER BY c.name"),
    @NamedQuery(name = "ClothingItem.findNeedingWash",
        query = "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags WHERE c.owner = :owner AND c.needsWash = true "
              + "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE) ORDER BY c.name"),
    @NamedQuery(name = "ClothingItem.findByTag",
        query = "SELECT DISTINCT c FROM ClothingItem c LEFT JOIN FETCH c.tags t WHERE t = :tag "
              + "AND (c.status IS NULL OR c.status = com.trousseau.model.ItemStatus.ACTIVE)")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "category"})
public class ClothingItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 50)
    @Column(length = 50)
    private String category;

    @Size(max = 50)
    @Column(length = 50)
    private String color;

    @Size(max = 100)
    @Column(length = 100)
    private String brand;

    @Size(max = 20)
    @Column(length = 20)
    private String size;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(name = "image_name", length = 255)
    private String imageName;

    /**
     * Down-scaled copy of {@link #imageData}, generated on upload. Grids and pickers
     * serve this instead of the original: a wardrobe page rendering fifty full-size
     * photos to draw 200px tiles is the difference between the app feeling fast and
     * feeling broken.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "thumbnail_data")
    private byte[] thumbnailData;

    @Column(name = "wear_count")
    private int wearCount = 0;

    // Integer (not int) so Hibernate can load NULL from existing rows added before this column existed
    @Getter(lombok.AccessLevel.NONE)
    @Column(name = "wears_since_wash")
    private Integer wearsSinceWash = 0;

    @Column(name = "last_worn_date")
    private LocalDate lastWornDate;

    @Column(name = "last_washed_date")
    private LocalDate lastWashedDate;

    @Column(name = "wash_after_wears")
    private int washAfterWears = 3;

    @Column(name = "needs_wash")
    private boolean needsWash = false;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /**
     * Lifecycle state. Nullable in the database so rows written before this column
     * existed still load; {@link #getStatus()} maps NULL to {@link ItemStatus#ACTIVE}.
     */
    @Getter(lombok.AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ItemStatus status = ItemStatus.ACTIVE;

    /** When the item left rotation. Null while it is active. */
    @Column(name = "retired_on")
    private LocalDate retiredOn;

    @Size(max = 200)
    @Column(name = "retired_note", length = 200)
    private String retiredNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "receipt_data")
    private byte[] receiptData;

    @Column(name = "receipt_content_type", length = 100)
    private String receiptContentType;

    @Column(name = "receipt_name", length = 255)
    private String receiptName;

    @ManyToMany
    @JoinTable(name = "clothing_item_tags",
        joinColumns = @JoinColumn(name = "clothing_item_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Returns 0 for legacy rows that have NULL stored in the database. */
    public int getWearsSinceWash() {
        return wearsSinceWash == null ? 0 : wearsSinceWash;
    }

    /** Returns ACTIVE for legacy rows that have NULL stored in the database. */
    public ItemStatus getStatus() {
        return status == null ? ItemStatus.ACTIVE : status;
    }

    public boolean isRetired() {
        return getStatus().isRetired();
    }

    /** Moves the item out of rotation, stamping the date. Clears any wash alert. */
    public void retire(ItemStatus newStatus, String note) {
        this.status = newStatus == null ? ItemStatus.ARCHIVED : newStatus;
        this.retiredOn = LocalDate.now();
        this.retiredNote = note;
        this.needsWash = false;
    }

    /** Returns the item to rotation, clearing the retirement stamp. */
    public void reactivate() {
        this.status = ItemStatus.ACTIVE;
        this.retiredOn = null;
        this.retiredNote = null;
    }

    /** True when a scaled-down copy of the photo is stored. */
    public boolean hasThumbnail() {
        return thumbnailData != null && thumbnailData.length > 0;
    }

    public void recordWear() {
        wearCount++;
        wearsSinceWash = (wearsSinceWash == null ? 0 : wearsSinceWash) + 1;
        lastWornDate = LocalDate.now();
        if (washAfterWears > 0 && wearsSinceWash >= washAfterWears) {
            needsWash = true;
        }
    }

    public void markWashed() {
        needsWash = false;
        wearsSinceWash = 0;
        lastWashedDate = LocalDate.now();
    }

    /**
     * Purchase price divided by lifetime wear count, rounded to two decimals, or
     * {@code null} when no price has been recorded.
     *
     * <p>An item that has never been worn is costed as though it had been worn once,
     * so it reports its full price rather than dividing by zero. That is deliberate:
     * on a cost-per-wear leaderboard the unworn items are exactly the ones that
     * should sort to the expensive end.</p>
     */
    public BigDecimal getCostPerWear() {
        if (purchasePrice == null) {
            return null;
        }
        int wears = Math.max(wearCount, 1);
        return purchasePrice.divide(BigDecimal.valueOf(wears), 2, RoundingMode.HALF_UP);
    }
}
