package com.trousseau.dao;

import com.trousseau.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class UserDao {

    @PersistenceContext
    private EntityManager em;

    public User save(User user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        } else {
            return em.merge(user);
        }
    }

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public User findByUsername(String username) {
        try {
            return em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public User findByEmail(String email) {
        try {
            return em.createNamedQuery("User.findByEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public User findByPasswordResetToken(String token) {
        try {
            return em.createNamedQuery("User.findByPasswordResetToken", User.class)
                    .setParameter("token", token)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<User> findAll() {
        return em.createNamedQuery("User.findAll", User.class)
                .getResultList();
    }

    public List<User> search(String term) {
        return em.createNamedQuery("User.search", User.class)
                .setParameter("term", "%" + term + "%")
                .getResultList();
    }

    public User update(User user) {
        return em.merge(user);
    }

    public void delete(User user) {
        em.remove(em.merge(user));
    }
}
