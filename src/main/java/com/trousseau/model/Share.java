package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "share")
@NamedQueries({
    @NamedQuery(name = "Share.findByOwner",
        query = "SELECT s FROM Share s WHERE s.owner = :owner ORDER BY s.createdAt DESC"),
    @NamedQuery(name = "Share.findSharedWithUser",
        query = "SELECT s FROM Share s WHERE s.sharedWith = :user ORDER BY s.createdAt DESC"),
    @NamedQuery(name = "Share.findByClothingItem",
        query = "SELECT s FROM Share s WHERE s.clothingItem = :item"),
    @NamedQuery(name = "Share.findByOutfit",
        query = "SELECT s FROM Share s WHERE s.outfit = :outfit"),
    @NamedQuery(name = "Share.findExistingItem",
        query = "SELECT s FROM Share s WHERE s.owner = :owner AND s.sharedWith = :sharedWith AND s.clothingItem = :item"),
    @NamedQuery(name = "Share.findExistingOutfit",
        query = "SELECT s FROM Share s WHERE s.owner = :owner AND s.sharedWith = :sharedWith AND s.outfit = :outfit")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id"})
public class Share implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothing_item_id")
    private ClothingItem clothingItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id")
    private Outfit outfit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
