package io.github.recordcompanion.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to generate a builder pattern implementation for Java records.
 *
 * <p>When applied to a record class, this annotation will generate separate Builder class and
 * Updater interface with builder pattern support for all record components.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Builder
 * public record User(String name, int age, String email) {}
 *
 * // Generates UserBuilder class and UserUpdater interface with:
 * // - UserBuilder.builder()
 * // - UserBuilder.builder(User existing)
 * // - UserBuilder.with(User existing, Consumer<UserUpdater> updater)
 *
 * // Usage:
 * User user = UserBuilder.builder()
 *     .name("John")
 *     .age(30)
 *     .email("john@example.com")
 *     .build();
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {}
