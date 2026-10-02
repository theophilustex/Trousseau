package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One day of a user's outfit plan, persisted so that the planner page, a manual
 * override, and the Sunday email all agree on what was planned.
 *
 * <p>A user has at most one plan row per date, enforced by a unique constraint.</p>
 */
@Entity
@Table(name = "planned_outfit", uniqueConstraints =
    @UniqueConstraint(columnNames = {"user_id", "plan_date"}))
@NamedQueries({
    @NamedQuery(name = "PlannedOutfit.findByUserAndDateRange",
        query = "SELECT p FROM PlannedOutfit p JOIN FETCH p.outfit "
              + "WHERE p.user = :user AND p.planDate >= :start AND p.planDate <= :end "
              + "ORDER BY p.planDate"),
    @NamedQuery(name = "PlannedOutfit.findByUserAndDate",
        query = "SELECT p FROM PlannedOutfit p JOIN FETCH p.outfit "
              + "WHERE p.user = :user AND p.planDate = :date"),
    @NamedQuery(name = "PlannedOutfit.deleteByUserAndDateRange",
        query = "DELETE FROM PlannedOutfit p "
              + "WHERE p.user = :user AND p.planDate >= :start AND p.planDate <= :end")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "planDate"})
public class PlannedOutfit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @NotNull
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    /** True when the user picked this outfit themselves rather than accepting a suggestion. */
    @Column(name = "manually_chosen")
    private boolean manuallyChosen = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
