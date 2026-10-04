package com.trousseau.dao;

import com.trousseau.model.Outfit;
import com.trousseau.model.OutfitWearLog;
import com.trousseau.model.OutfitWearStat;
import com.trousseau.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class OutfitWearLogDao {

    @PersistenceContext
    private EntityManager em;

    public OutfitWearLog save(OutfitWearLog log) {
        em.persist(log);
        return log;
    }

    public List<OutfitWearLog> findByOutfit(Outfit outfit) {
        return em.createNamedQuery("OutfitWearLog.findByOutfit", OutfitWearLog.class)
                .setParameter("outfit", outfit)
                .getResultList();
    }

    public List<OutfitWearLog> findByOutfitRecent(Outfit outfit, int limit) {
        return em.createNamedQuery("OutfitWearLog.findByOutfitRecent", OutfitWearLog.class)
                .setParameter("outfit", outfit)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countByOutfitAndDateRange(Outfit outfit, LocalDate start, LocalDate end) {
        return em.createNamedQuery("OutfitWearLog.countByOutfitAndDateRange", Long.class)
                .setParameter("outfit", outfit)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
    }

    public List<OutfitWearLog> findByUserAndDateRange(User user, LocalDate start, LocalDate end) {
        return em.createNamedQuery("OutfitWearLog.findByUserAndDateRange", OutfitWearLog.class)
                .setParameter("user", user)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    // -------------------------------------------------------------------------
    // Insights queries
    // -------------------------------------------------------------------------

    /**
     * Just the dates of the user's wear entries in a range. The activity chart buckets
     * these by month in Java rather than in SQL, because date-truncation functions
     * differ between H2 and PostgreSQL and this app has to run on both.
     */
    public List<LocalDate> findWearDates(User user, LocalDate start, LocalDate end) {
        return em.createQuery(
                "SELECT w.wornDate FROM OutfitWearLog w "
                        + "WHERE w.user = :user AND w.wornDate >= :start AND w.wornDate <= :end",
                LocalDate.class)
                .setParameter("user", user)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    /** Outfits the user has logged most often, most-worn first. */
    public List<OutfitWearStat> findMostWornOutfits(User user, int limit) {
        return em.createQuery(
                "SELECT NEW com.trousseau.model.OutfitWearStat(o.id, o.name, o.occasion, COUNT(w)) "
                        + "FROM OutfitWearLog w JOIN w.outfit o WHERE w.user = :user "
                        + "GROUP BY o.id, o.name, o.occasion ORDER BY COUNT(w) DESC, o.name",
                OutfitWearStat.class)
                .setParameter("user", user)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countByUser(User user) {
        return em.createQuery(
                "SELECT COUNT(w) FROM OutfitWearLog w WHERE w.user = :user", Long.class)
                .setParameter("user", user)
                .getSingleResult();
    }
}
