package com.emailssummarizer.apirs.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link UserRole} entities.
 *
 * <p>Provides standard CRUD operations and a custom query to fetch all roles belonging
 * to a specific GitHub login.
 *
 * <p>This repository is called only from {@link UserService}; no other class should
 * inject it directly.
 *
 * @see UserService
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Returns all role rows associated with the given GitHub login.
     *
     * @param login  the GitHub login to look up; must not be {@code null}
     * @return       a list of {@link UserRole} entries; never {@code null}, may be empty
     */
    List<UserRole> findAllByLogin(String login);
}
