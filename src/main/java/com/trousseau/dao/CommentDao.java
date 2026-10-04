package com.trousseau.dao;

import com.trousseau.model.Comment;
import com.trousseau.model.Outfit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class CommentDao {

    @PersistenceContext
    private EntityManager em;

    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            em.persist(comment);
            return comment;
        } else {
            return em.merge(comment);
        }
    }

    public Comment findById(Long id) {
        return em.find(Comment.class, id);
    }

    public List<Comment> findByOutfit(Outfit outfit) {
        return em.createNamedQuery("Comment.findByOutfit", Comment.class)
                .setParameter("outfit", outfit)
                .getResultList();
    }

    public Comment update(Comment comment) {
        return em.merge(comment);
    }

    public void delete(Comment comment) {
        em.remove(em.merge(comment));
    }
}
