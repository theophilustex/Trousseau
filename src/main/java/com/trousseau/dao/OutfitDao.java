package com.trousseau.dao;

import com.trousseau.model.Outfit;
import com.trousseau.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class OutfitDao {

    @PersistenceContext
    private EntityManager em;

    public Outfit save(Outfit outfit) {
        if (outfit.getId() == null) {
            em.persist(outfit);
            return outfit;
        } else {
            return em.merge(outfit);
        }
    }

    public Outfit findById(Long id) {
        try {
            return em.createQuery(
                    "SELECT o FROM Outfit o JOIN FETCH o.creator WHERE o.id = :id",
                    Outfit.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Outfit> findByCreator(User creator) {
        return em.createQuery(
                "SELECT DISTINCT o FROM Outfit o LEFT JOIN FETCH o.items WHERE o.creator = :creator ORDER BY o.createdAt DESC",
                Outfit.class)
                .setParameter("creator", creator)
                .getResultList();
    }

    public List<Outfit> findSharedWith(User user) {
        return em.createNamedQuery("Outfit.findSharedWith", Outfit.class)
                .setParameter("user", user)
                .getResultList();
    }

    public Outfit update(Outfit outfit) {
        return em.merge(outfit);
    }

    public void delete(Outfit outfit) {
        em.remove(em.merge(outfit));
    }

    public long countByCreator(User creator) {
        return em.createQuery(
                "SELECT COUNT(o) FROM Outfit o WHERE o.creator = :creator", Long.class)
                .setParameter("creator", creator)
                .getSingleResult();
    }
}
