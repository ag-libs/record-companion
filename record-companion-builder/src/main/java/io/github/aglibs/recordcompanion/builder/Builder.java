package io.github.aglibs.recordcompanion.builder;

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
public @interface Builder {

  /**
   * Whether to copy annotations from the record and its components to the generated builder class.
   *
   * <p>When set to {@code true}, annotations present on the record class and its components will be
   * copied to the corresponding generated builder class and its methods.
   *
   * @return {@code true} if annotations should be copied, {@code false} otherwise
   */
  boolean copyAnnotations() default false;
}
