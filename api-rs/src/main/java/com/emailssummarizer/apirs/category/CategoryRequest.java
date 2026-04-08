package com.emailssummarizer.apirs.category;

/**
 * Immutable DTO carrying the data required to create or update a {@link Category}.
 *
 * <p>Used as the request body for {@code POST /categories} and
 * {@code PUT /categories/{code}}. On update, the {@code code} component is ignored
 * by the service layer — the path variable is used as the lookup key and the code
 * itself remains immutable.
 *
 * <p>Validation of individual fields (non-blank {@code name}, unique {@code code})
 * is performed by {@link CategoryService}, not at the DTO level.
 *
 * @param name         the human-readable display name for the category; must not be {@code null}
 * @param code         the unique business key for the category; must not be {@code null} or blank
 *                     on creation; ignored on update
 * @param description  an optional human-readable description; may be {@code null}
 *
 * @see CategoryService
 */
public record CategoryRequest(String name, String code, String description) {}
