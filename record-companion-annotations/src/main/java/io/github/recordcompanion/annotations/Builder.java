package io.github.recordcompanion.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to generate a builder pattern implementation for Java records.
 *
 * <p>When applied to a record class, this annotation will generate a companion class with builder
 * methods for all record components.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Builder
 * public record User(String name, int age, String email) {}
 *
 * // Generates UserCompanion class with:
 * // - UserCompanion.builder()
 * // - UserCompanion.builder(User existing)
 *
 * // Usage:
 * User user = UserCompanion.builder()
 *     .name("John")
 *     .age(30)
 *     .email("john@example.com")
 *     .build();
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {}
