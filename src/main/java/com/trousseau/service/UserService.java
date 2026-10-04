package com.trousseau.service;

import com.trousseau.dao.UserDao;
import com.trousseau.model.User;
import com.trousseau.util.PasswordUtil;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Stateless
public class UserService {

    private static final int RESET_TOKEN_BYTES = 32;
    static final int RESET_TOKEN_TTL_MINUTES = 60;
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

    // --- account changes ------------------------------------------------------
    //
    // These take an id and change only the named fields on a freshly loaded entity.
    // They must never merge the User held in SessionBean: that copy was loaded at login,
    // and merging it would write back every field as it was then. A profile save in an
    // old session used to restore the password from before a reset.

    /**
     * Updates display name and email, returning the fresh entity for the session.
     *
     * @throws IllegalArgumentException if the email belongs to another account
     */
    public User updateProfile(Long userId, String displayName, String email) {
        User user = userDao.findById(userId);
        String normalizedEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        if (normalizedEmail != null) {
            User holder = userDao.findByEmail(normalizedEmail);
            if (holder != null && !holder.getId().equals(userId)) {
                throw new IllegalArgumentException("That email address is used by another account.");
            }
        }
        user.setDisplayName(displayName);
        user.setEmail(normalizedEmail);
        return user;
    }

    /** True when another account already uses this email address. */
    public boolean isEmailUsedByOther(Long userId, String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        User holder = userDao.findByEmail(email.trim());
        return holder != null && !holder.getId().equals(userId);
    }

    /** Sets or clears the home location used for weather. Returns the fresh entity. */
    public User updateLocation(Long userId, Double latitude, Double longitude, String locationName) {
        User user = userDao.findById(userId);
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        user.setLocationName(locationName);
        return user;
    }

    /**
     * Changes the password if {@code currentPlaintext} is right. Signs out every other
     * session (see {@link User#getCredentialsVersion()}); the caller must give its own
     * session the returned user's new version to stay signed in.
     *
     * @return the fresh entity, or null if the current password was wrong
     */
    public User changePassword(Long userId, String currentPlaintext, String newPlaintext) {
        User user = userDao.findById(userId);
        if (user == null || !PasswordUtil.checkPassword(currentPlaintext, user.getPasswordHash())) {
            return null;
        }
        user.changePasswordHash(PasswordUtil.hashPassword(newPlaintext));
        return user;
    }

    /**
     * The account's current credentials version, or -1 if the account no longer exists.
     * AuthFilter calls this on every signed-in page request, so it reads one column.
     */
    public int getCredentialsVersion(Long userId) {
        return userDao.findCredentialsVersion(userId);
    }

    // --- password reset (see PasswordResetService) --------------------------

    /** The account whose username, or failing that whose email, matches. */
    public User findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            return null;
        }
        String trimmed = usernameOrEmail.trim();
        User user = userDao.findByUsername(trimmed);
        return user != null ? user : userDao.findByEmail(trimmed);
    }

    /** True when a reset token was issued for this user within the last {@code minutes}. */
    public boolean isResetRequestedWithin(User user, int minutes) {
        LocalDateTime expiresAt = user.getPasswordResetExpiresAt();
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime issuedAt = expiresAt.minusMinutes(RESET_TOKEN_TTL_MINUTES);
        return issuedAt.isAfter(LocalDateTime.now().minusMinutes(minutes));
    }

    /**
     * Issues a single-use reset token, valid for {@value #RESET_TOKEN_TTL_MINUTES}
     * minutes, replacing any earlier one. Returns the token to put in the emailed link.
     * Only its SHA-256 hash is stored, so a copy of the database does not contain
     * working reset links.
     */
    public String issuePasswordResetToken(User user) {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setPasswordResetToken(hashToken(token));
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        userDao.update(user);
        return token;
    }

    public User findByValidResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        User user = userDao.findByPasswordResetToken(hashToken(token.trim()));
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
        user.changePasswordHash(PasswordUtil.hashPassword(newPlaintext));
        userDao.update(user);
        return true;
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }
}
