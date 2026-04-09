package com.emailssummarizer.apirs.message;

/**
 * Immutable DTO carrying the data required to create or update a {@link Message}.
 *
 * <p>Used as the request body for {@code POST /messages} and
 * {@code PUT /messages/{id}}. On update, the {@code categoryCode} component is
 * accepted but currently ignored by the service layer — only {@code title} and
 * {@code body} are modified.
 *
 * <p>Validation of individual fields (non-blank {@code title}, existence of the
 * referenced {@code categoryCode}) is performed by {@link MessageService}, not at
 * the DTO level.
 *
 * @param title         the subject line of the message; must not be {@code null}
 * @param body          the full content of the message; may be {@code null}
 * @param categoryCode  the code of the category this message belongs to; must reference
 *                      an existing {@link com.emailssummarizer.apirs.category.Category}
 *                      on creation
 *
 * @see MessageService
 */
public record MessageRequest(String title, String body, String categoryCode) {}
