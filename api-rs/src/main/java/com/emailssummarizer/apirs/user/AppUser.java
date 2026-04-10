package com.emailssummarizer.apirs.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a registered application user.
 *
 * <p>Each row corresponds to a GitHub user who has authenticated at least once.
 * The GitHub {@code login} is used as the natural primary key because it is unique
 * across GitHub and is already used as the principal name throughout the security layer.
 *
 * <p>Instances are created automatically by {@link UserService#findOrRegister} on first
 * login; they are never created manually.
 *
 * @see UserRole
 * @see UserService
 */
@Entity
@Table(name = "USERS")
public class AppUser {

    /** GitHub login name; serves as the primary key. */
    @Id
    @Column(name = "login", nullable = false, length = 100)
    private String login;

    /** Numeric GitHub user ID returned by the GitHub user-info endpoint. */
    @Column(name = "github_id")
    private Long githubId;

    /** Display name as returned by GitHub; may be {@code null} if the user has not set one. */
    @Column(name = "name", length = 255)
    private String name;

    /** URL of the user's GitHub avatar image; may be {@code null}. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Required by JPA. */
    protected AppUser() {}

    /**
     * Constructs a new {@code AppUser} with the given GitHub profile data.
     *
     * @param login      GitHub login name; must not be {@code null}
     * @param githubId   numeric GitHub user ID; may be {@code null}
     * @param name       display name from GitHub; may be {@code null}
     * @param avatarUrl  GitHub avatar URL; may be {@code null}
     */
    public AppUser(String login, Long githubId, String name, String avatarUrl) {
        this.login     = login;
        this.githubId  = githubId;
        this.name      = name;
        this.avatarUrl = avatarUrl;
    }

    /**
     * Returns the GitHub login of this user, which is also the primary key.
     *
     * @return the GitHub login; never {@code null}
     */
    public String getLogin() { return login; }

    /**
     * Returns the numeric GitHub user ID.
     *
     * @return the GitHub user ID; may be {@code null}
     */
    public Long getGithubId() { return githubId; }

    /**
     * Returns the display name of the user as provided by GitHub.
     *
     * @return the display name; may be {@code null}
     */
    public String getName() { return name; }

    /**
     * Returns the URL of the user's GitHub avatar image.
     *
     * @return the avatar URL; may be {@code null}
     */
    public String getAvatarUrl() { return avatarUrl; }
}
