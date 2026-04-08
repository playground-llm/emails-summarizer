package com.emailssummarizer.apirs.category;

import jakarta.persistence.*;

/**
 * JPA entity representing a message category.
 *
 * <p>Maps to the {@code CATEGORY} database table. The {@code code} field is a unique
 * business key used as the URL path parameter in REST endpoints and as a foreign key
 * target from {@link com.emailssummarizer.apirs.message.Message}. Once set,
 * {@code code} must not be changed — it is treated as immutable after creation.
 *
 * <p>The {@code id} is an auto-generated surrogate key used internally by JPA;
 * external callers should always refer to categories by their {@code code}.
 *
 * @see CategoryService
 * @see CategoryRepository
 */
@Entity
@Table(name = "CATEGORY")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column
    private String description;

    /**
     * Required no-argument constructor for JPA.
     */
    public Category() {}

    /**
     * Returns the surrogate primary key of this category.
     *
     * @return the auto-generated {@code id}, or {@code null} before the entity is persisted
     */
    public Long getId() { return id; }

    /**
     * Sets the surrogate primary key of this category.
     *
     * <p>This method exists for JPA and testing purposes; application code should not
     * set the {@code id} directly.
     *
     * @param id  the primary key value to assign
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the display name of this category.
     *
     * @return the human-readable name; never {@code null} for a persisted category
     */
    public String getName() { return name; }

    /**
     * Sets the display name of this category.
     *
     * @param name  the new display name; must not be {@code null}
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the unique business key of this category.
     *
     * <p>The code is immutable after creation and is used as the URL path parameter
     * (e.g. {@code /categories/{code}}) and as the foreign key value in
     * {@link com.emailssummarizer.apirs.message.Message#getCategoryCode()}.
     *
     * @return the unique category code; never {@code null} for a persisted category
     */
    public String getCode() { return code; }

    /**
     * Sets the unique business key of this category.
     *
     * <p>Must only be called during initial creation. Changing the code of an
     * existing category will break foreign key references from messages.
     *
     * @param code  the unique business key to assign; must not be {@code null} or blank
     */
    public void setCode(String code) { this.code = code; }

    /**
     * Returns the optional human-readable description of this category.
     *
     * @return the description, or {@code null} if none was provided
     */
    public String getDescription() { return description; }

    /**
     * Sets the optional human-readable description of this category.
     *
     * @param description  a descriptive text for the category; may be {@code null}
     */
    public void setDescription(String description) { this.description = description; }
}
