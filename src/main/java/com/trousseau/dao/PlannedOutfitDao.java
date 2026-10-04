package com.trousseau.dao;

import com.trousseau.model.PlannedOutfit;
import com.trousseau.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class PlannedOutfitDao {

    @PersistenceContext
    private EntityManager em;

    public PlannedOutfit save(PlannedOutfit plan) {
        if (plan.getId() == null) {
            em.persist(plan);
            return plan;
        }
        return em.merge(plan);
    }

    public List<PlannedOutfit> findByUserAndDateRange(User user, LocalDate start, LocalDate end) {
        return em.createNamedQuery("PlannedOutfit.findByUserAndDateRange", PlannedOutfit.class)
                .setParameter("user", user)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    public PlannedOutfit findByUserAndDate(User user, LocalDate date) {
        List<PlannedOutfit> results =
                em.createNamedQuery("PlannedOutfit.findByUserAndDate", PlannedOutfit.class)
                        .setParameter("user", user)
                        .setParameter("date", date)
                        .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /** Removes every plan row for the user inside the range. Used when regenerating a week. */
    public int deleteByUserAndDateRange(User user, LocalDate start, LocalDate end) {
        return em.createNamedQuery("PlannedOutfit.deleteByUserAndDateRange")
                .setParameter("user", user)
                .setParameter("start", start)
                .setParameter("end", end)
                .executeUpdate();
    }

    public void delete(PlannedOutfit plan) {
        em.remove(em.contains(plan) ? plan : em.merge(plan));
    }
}
