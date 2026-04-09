package com.emailssummarizer.apirs.user;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AppUser} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}. The primary
 * key type is {@code String} (the GitHub login).
 *
 * <p>This repository is called only from {@link UserService}; no other class should
 * inject it directly.
 *
 * @see UserService
 */
public interface AppUserRepository extends JpaRepository<AppUser, String> {
}
