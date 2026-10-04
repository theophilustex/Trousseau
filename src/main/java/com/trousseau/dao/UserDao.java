package com.trousseau.dao;

import com.trousseau.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
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

    /** The credentials version (NULL read as 0), or -1 when there is no such user. */
    public int findCredentialsVersion(Long id) {
        List<Integer> rows = em.createQuery(
                "SELECT u.credentialsVersion FROM User u WHERE u.id = :id", Integer.class)
                .setParameter("id", id)
                .getResultList();
        if (rows.isEmpty()) {
            return -1;
        }
        return rows.get(0) == null ? 0 : rows.get(0);
    }

    public User update(User user) {
        return em.merge(user);
    }

    public void delete(User user) {
        em.remove(em.merge(user));
    }
}
