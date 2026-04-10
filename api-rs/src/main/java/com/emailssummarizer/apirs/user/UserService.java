package com.emailssummarizer.apirs.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that manages application user registration and role retrieval.
 *
 * <p>On every successful GitHub token introspection, the security layer calls
 * {@link #findOrRegister} with the authenticated user's GitHub profile data.
 * The method implements a "find-or-create" pattern:
 * <ul>
 *   <li>If the user's {@code login} is already present in the {@code USERS} table,
 *       the user's current roles are loaded from the {@code ROLES} table and returned.</li>
 *   <li>If the {@code login} is not found, a new {@link AppUser} row is inserted and a
 *       single {@link UserRole} row granting {@code ROLE_READ} is created automatically.</li>
 * </ul>
 *
 * <p>All database operations run within a single transaction to prevent partial writes.
 *
 * @see AppUserRepository
 * @see UserRoleRepository
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_ROLE = "ROLE_READ";

    private final AppUserRepository userRepository;
    private final UserRoleRepository roleRepository;

    /**
     * Constructs the service with the required repositories.
     *
     * @param userRepository  repository for {@link AppUser} entities; must not be {@code null}
     * @param roleRepository  repository for {@link UserRole} entities; must not be {@code null}
     */
    public UserService(AppUserRepository userRepository, UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Returns the roles for the given GitHub user, registering them with {@code ROLE_READ}
     * if this is their first login.
     *
     * <p>The entire operation is transactional. If the user already exists, only a SELECT
     * on the {@code ROLES} table is issued. If the user is new, an INSERT into {@code USERS}
     * and an INSERT into {@code ROLES} are issued atomically.
     *
     * @param login      the lower-cased GitHub login; must not be {@code null}
     * @param githubId   numeric GitHub user ID extracted from the introspection response;
     *                   may be {@code null}
     * @param name       display name from GitHub; may be {@code null}
     * @param avatarUrl  GitHub avatar URL; may be {@code null}
     * @return           a non-null, non-empty list of Spring Security role strings
     *                   (e.g. {@code ["ROLE_READ"]}); never empty because every registered
     *                   user holds at least {@code ROLE_READ}
     */
    @Transactional
    public List<String> findOrRegister(String login, Long githubId, String name, String avatarUrl) {
        if (!userRepository.existsById(login)) {
            log.info("First login for GitHub user '{}' — registering with {}", login, DEFAULT_ROLE);
            userRepository.save(new AppUser(login, githubId, name, avatarUrl));
            roleRepository.save(new UserRole(login, DEFAULT_ROLE));
            return List.of(DEFAULT_ROLE);
        }

        List<String> roles = roleRepository.findAllByLogin(login)
                .stream()
                .map(UserRole::getRole)
                .toList();

        log.debug("GitHub user '{}' authenticated — roles: {}", login, roles);
        return roles;
    }
}
