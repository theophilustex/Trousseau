package com.trousseau.dao;

import com.trousseau.model.Outfit;
import com.trousseau.model.Rating;
import com.trousseau.model.RatingType;
import com.trousseau.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class RatingDao {

    @PersistenceContext
    private EntityManager em;

    public Rating save(Rating rating) {
        if (rating.getId() == null) {
            em.persist(rating);
            return rating;
        } else {
            return em.merge(rating);
        }
    }

    public Rating findById(Long id) {
        return em.find(Rating.class, id);
    }

    public List<Rating> findByOutfit(Outfit outfit) {
        return em.createNamedQuery("Rating.findByOutfit", Rating.class)
                .setParameter("outfit", outfit)
                .getResultList();
    }

    public List<Rating> findByOutfitAndType(Outfit outfit, RatingType type) {
        return em.createNamedQuery("Rating.findByOutfitAndType", Rating.class)
                .setParameter("outfit", outfit)
                .setParameter("type", type)
                .getResultList();
    }

    public List<Rating> findByOutfitAndRater(Outfit outfit, User rater) {
        return em.createNamedQuery("Rating.findByOutfitAndRater", Rating.class)
                .setParameter("outfit", outfit)
                .setParameter("rater", rater)
                .getResultList();
    }

    public Double getAverageByOutfitAndType(Outfit outfit, RatingType type) {
        return em.createNamedQuery("Rating.avgByOutfitAndType", Double.class)
                .setParameter("outfit", outfit)
                .setParameter("type", type)
                .getSingleResult();
    }

    public Rating update(Rating rating) {
        return em.merge(rating);
    }

    public void delete(Rating rating) {
        em.remove(em.merge(rating));
    }
}
