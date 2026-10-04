package com.trousseau.dao;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.Outfit;
import com.trousseau.model.Share;
import com.trousseau.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

@ApplicationScoped
public class ShareDao {

    @PersistenceContext
    private EntityManager em;

    public Share save(Share share) {
        if (share.getId() == null) {
            em.persist(share);
            return share;
        } else {
            return em.merge(share);
        }
    }

    public Share findById(Long id) {
        return em.find(Share.class, id);
    }

    public List<Share> findByOwner(User owner) {
        return em.createQuery(
                "SELECT s FROM Share s JOIN FETCH s.sharedWith LEFT JOIN FETCH s.clothingItem LEFT JOIN FETCH s.outfit WHERE s.owner = :owner ORDER BY s.createdAt DESC",
                Share.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public List<Share> findSharedWithUser(User user) {
        return em.createQuery(
                "SELECT s FROM Share s JOIN FETCH s.owner LEFT JOIN FETCH s.clothingItem LEFT JOIN FETCH s.outfit WHERE s.sharedWith = :user ORDER BY s.createdAt DESC",
                Share.class)
                .setParameter("user", user)
                .getResultList();
    }

    public List<Share> findByClothingItem(ClothingItem item) {
        return em.createNamedQuery("Share.findByClothingItem", Share.class)
                .setParameter("item", item)
                .getResultList();
    }

    public List<Share> findByOutfit(Outfit outfit) {
        return em.createNamedQuery("Share.findByOutfit", Share.class)
                .setParameter("outfit", outfit)
                .getResultList();
    }

    public boolean exists(User owner, User sharedWith, ClothingItem item, Outfit outfit) {
        try {
            if (item != null) {
                em.createNamedQuery("Share.findExistingItem", Share.class)
                        .setParameter("owner", owner)
                        .setParameter("sharedWith", sharedWith)
                        .setParameter("item", item)
                        .getSingleResult();
                return true;
            } else if (outfit != null) {
                em.createNamedQuery("Share.findExistingOutfit", Share.class)
                        .setParameter("owner", owner)
                        .setParameter("sharedWith", sharedWith)
                        .setParameter("outfit", outfit)
                        .getSingleResult();
                return true;
            }
            return false;
        } catch (NoResultException e) {
            return false;
        }
    }

    public void delete(Share share) {
        em.remove(em.merge(share));
    }
}
