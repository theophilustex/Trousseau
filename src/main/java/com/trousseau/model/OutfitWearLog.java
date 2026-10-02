package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "outfit_wear_log")
@NamedQueries({
    @NamedQuery(name = "OutfitWearLog.findByOutfit",
        query = "SELECT w FROM OutfitWearLog w JOIN FETCH w.user WHERE w.outfit = :outfit ORDER BY w.wornDate DESC"),
    @NamedQuery(name = "OutfitWearLog.findByOutfitRecent",
        query = "SELECT w FROM OutfitWearLog w JOIN FETCH w.user WHERE w.outfit = :outfit ORDER BY w.wornDate DESC"),
    @NamedQuery(name = "OutfitWearLog.countByOutfitAndDateRange",
        query = "SELECT COUNT(w) FROM OutfitWearLog w WHERE w.outfit = :outfit AND w.wornDate >= :start AND w.wornDate <= :end"),
    @NamedQuery(name = "OutfitWearLog.findByUserAndDateRange",
        query = "SELECT w FROM OutfitWearLog w JOIN FETCH w.outfit WHERE w.user = :user AND w.wornDate >= :start AND w.wornDate <= :end ORDER BY w.wornDate DESC")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "wornDate"})
public class OutfitWearLog implements Serializable {

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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "worn_date", nullable = false)
    private LocalDate wornDate;

    @PrePersist
    protected void onCreate() {
        if (wornDate == null) {
            wornDate = LocalDate.now();
        }
    }
}
