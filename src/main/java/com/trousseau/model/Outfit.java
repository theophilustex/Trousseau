package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "outfit")
@NamedQueries({
    @NamedQuery(name = "Outfit.findByCreator",
        query = "SELECT o FROM Outfit o WHERE o.creator = :creator ORDER BY o.createdAt DESC"),
    @NamedQuery(name = "Outfit.findSharedWith",
        query = "SELECT DISTINCT o FROM Outfit o JOIN Share s ON s.outfit = o WHERE s.sharedWith = :user ORDER BY o.createdAt DESC")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "occasion"})
public class Outfit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100)
    @Column(length = 100)
    private String occasion;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "outfit_seasons", joinColumns = @JoinColumn(name = "outfit_id"))
    @Column(name = "season", length = 50)
    private Set<String> seasons = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(name = "outfit_items",
        joinColumns = @JoinColumn(name = "outfit_id"),
        inverseJoinColumns = @JoinColumn(name = "clothing_item_id"))
    private List<ClothingItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "outfit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "outfit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Double getAverageRating(RatingType type) {
        double sum = 0;
        int count = 0;
        for (Rating r : ratings) {
            if (r.getRatingType() == type) {
                sum += r.getScore();
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }

    public Double getOverallAverageRating() {
        if (ratings.isEmpty()) return null;
        double sum = 0;
        for (Rating r : ratings) {
            sum += r.getScore();
        }
        return sum / ratings.size();
    }
}
