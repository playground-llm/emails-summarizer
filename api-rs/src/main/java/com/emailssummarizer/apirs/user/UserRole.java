package com.emailssummarizer.apirs.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a single role granted to an application user.
 *
 * <p>Each row links a GitHub login to one Spring Security role string
 * (e.g. {@code ROLE_READ}, {@code ROLE_EDIT}, {@code ROLE_DEL}). A user may hold
 * multiple roles, each stored as a separate row. The {@code (login, role)} combination
 * is unique (enforced both by the DB constraint and by {@link UserService}).
 *
 * <p>New users are automatically assigned {@code ROLE_READ} by
 * {@link UserService#findOrRegister} on their first login.
 *
 * @see AppUser
 * @see UserService
 */
@Entity
@Table(name = "ROLES")
public class UserRole {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** GitHub login of the user who holds this role; foreign key to {@code USERS.login}. */
    @Column(name = "login", nullable = false, length = 100)
    private String login;

    /** Spring Security role string, e.g. {@code ROLE_READ}. */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /** Required by JPA. */
    protected UserRole() {}

    /**
     * Constructs a new {@code UserRole} granting the given role to the given user.
     *
     * @param login  GitHub login of the user; must not be {@code null}
     * @param role   Spring Security role string; must not be {@code null}
     */
    public UserRole(String login, String role) {
        this.login = login;
        this.role  = role;
    }

    /**
     * Returns the surrogate identifier of this role row.
     *
     * @return the auto-generated ID
     */
    public Long getId() { return id; }

    /**
     * Returns the GitHub login of the user who holds this role.
     *
     * @return the GitHub login; never {@code null}
     */
    public String getLogin() { return login; }

    /**
     * Returns the Spring Security role string (e.g. {@code ROLE_READ}).
     *
     * @return the role; never {@code null}
     */
    public String getRole() { return role; }
}
