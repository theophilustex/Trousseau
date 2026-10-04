package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_user")
@NamedQueries({
    @NamedQuery(name = "User.findByUsername", query = "SELECT u FROM User u WHERE u.username = :username"),
    @NamedQuery(name = "User.findByEmail", query = "SELECT u FROM User u WHERE u.email = :email"),
    @NamedQuery(name = "User.findAll", query = "SELECT u FROM User u ORDER BY u.displayName"),
    @NamedQuery(name = "User.search", query = "SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(:term) OR LOWER(u.displayName) LIKE LOWER(:term)"),
    @NamedQuery(name = "User.findByPasswordResetToken", query = "SELECT u FROM User u WHERE u.passwordResetToken = :token")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "username", "displayName"})
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Email
    @Column(unique = true, length = 100)
    private String email;

    @NotNull
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional home coordinates, used only to fetch a forecast for the weekly planner.
     * Leaving them unset disables weather scoring entirely — which is also what keeps
     * the app usable on a network with no route to the internet.
     */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Size(max = 100)
    @Column(name = "location_name", length = 100)
    private String locationName;

    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    /**
     * Incremented whenever the password changes. A session remembers the value it saw
     * at login, and AuthFilter ends any session whose value is out of date, so a
     * password change or reset signs out every other device.
     *
     * <p>A counter rather than a timestamp, so there is no precision to lose between
     * Java and the database. Nullable because hbm2ddl adds it as NULL to existing rows;
     * {@link #getCredentialsVersion()} reads NULL as 0.</p>
     */
    @Getter(lombok.AccessLevel.NONE)
    @Column(name = "credentials_version")
    private Integer credentialsVersion = 0;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothingItem> clothingItems = new ArrayList<>();

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Outfit> outfits = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (displayName == null || displayName.isEmpty()) {
            displayName = username;
        }
    }

    public int getCredentialsVersion() {
        return credentialsVersion == null ? 0 : credentialsVersion;
    }

    /** Sets a new password hash and invalidates every outstanding session and reset link. */
    public void changePasswordHash(String newHash) {
        this.passwordHash = newHash;
        this.credentialsVersion = getCredentialsVersion() + 1;
        this.passwordResetToken = null;
        this.passwordResetExpiresAt = null;
    }
}
