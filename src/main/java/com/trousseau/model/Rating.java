package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "rating", uniqueConstraints =
    @UniqueConstraint(columnNames = {"outfit_id", "rater_id", "rating_type"}))
@NamedQueries({
    @NamedQuery(name = "Rating.findByOutfit",
        query = "SELECT r FROM Rating r WHERE r.outfit = :outfit ORDER BY r.createdAt DESC"),
    @NamedQuery(name = "Rating.findByOutfitAndType",
        query = "SELECT r FROM Rating r WHERE r.outfit = :outfit AND r.ratingType = :type"),
    @NamedQuery(name = "Rating.findByOutfitAndRater",
        query = "SELECT r FROM Rating r WHERE r.outfit = :outfit AND r.rater = :rater"),
    @NamedQuery(name = "Rating.avgByOutfitAndType",
        query = "SELECT AVG(r.score) FROM Rating r WHERE r.outfit = :outfit AND r.ratingType = :type")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "ratingType", "score"})
public class Rating implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rating_type", nullable = false, length = 30)
    private RatingType ratingType;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
