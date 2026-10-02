package com.trousseau.dao;

import com.trousseau.model.Tag;
import com.trousseau.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class TagDao {

    @PersistenceContext
    private EntityManager em;

    public Tag save(Tag tag) {
        if (tag.getId() == null) {
            em.persist(tag);
            return tag;
        } else {
            return em.merge(tag);
        }
    }

    public Tag findById(Long id) {
        return em.find(Tag.class, id);
    }

    public List<Tag> findByOwner(User owner) {
        return em.createNamedQuery("Tag.findByOwner", Tag.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public Tag findByOwnerAndName(User owner, String name) {
        try {
            return em.createNamedQuery("Tag.findByOwnerAndName", Tag.class)
                    .setParameter("owner", owner)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Tag update(Tag tag) {
        return em.merge(tag);
    }

    public void delete(Tag tag) {
        em.remove(em.merge(tag));
    }
}
