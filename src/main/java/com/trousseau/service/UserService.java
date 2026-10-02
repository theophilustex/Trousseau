package com.trousseau.service;

import com.trousseau.dao.UserDao;
import com.trousseau.model.User;
import com.trousseau.util.PasswordUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Stateless
public class UserService {

    private static final int RESET_TOKEN_BYTES = 32;
    private static final int RESET_TOKEN_TTL_MINUTES = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    private UserDao userDao;

    public User register(String username, String email, String password, String displayName) {
        if (userDao.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        String normalizedEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        if (normalizedEmail != null && userDao.findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException("Email '" + normalizedEmail + "' is already registered.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setDisplayName(displayName);

        return userDao.save(user);
    }

    public User authenticate(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public User findById(Long id) {
        return userDao.findById(id);
    }

    public User findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    public List<User> findAll() {
        return userDao.findAll();
    }

    public List<User> searchUsers(String term) {
        return userDao.search(term);
    }

    public User updateProfile(User user) {
        return userDao.update(user);
    }

    public User update(User user) {
        return userDao.update(user);
    }

    public boolean checkPassword(User user, String plaintext) {
        return PasswordUtil.checkPassword(plaintext, user.getPasswordHash());
    }

    public void updatePassword(User user, String newPlaintext) {
        user.setPasswordHash(PasswordUtil.hashPassword(newPlaintext));
        userDao.update(user);
    }

    /**
     * Generate a single-use password reset token for the user matching the given
     * username or email. Returns the plaintext token to display to the user, or
     * {@code null} if no matching account exists. Callers should always respond
     * with a generic success message to avoid leaking which accounts are registered.
     */
    public String requestPasswordReset(String usernameOrEmail) {
        if (usernameOrEmail == null) {
            return null;
        }
        String trimmed = usernameOrEmail.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        User user = userDao.findByUsername(trimmed);
        if (user == null) {
            user = userDao.findByEmail(trimmed);
        }
        if (user == null) {
            return null;
        }

        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        userDao.update(user);
        return token;
    }

    public User findByValidResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        User user = userDao.findByPasswordResetToken(token.trim());
        if (user == null) {
            return null;
        }
        LocalDateTime expiresAt = user.getPasswordResetExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            return null;
        }
        return user;
    }

    public boolean completePasswordReset(String token, String newPlaintext) {
        User user = findByValidResetToken(token);
        if (user == null) {
            return false;
        }
        user.setPasswordHash(PasswordUtil.hashPassword(newPlaintext));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userDao.update(user);
        return true;
    }
}
