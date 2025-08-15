package io.github.recordcompanion.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to generate ValidCheck integration for Java records.
 *
 * <p>When applied to a record class along with @Builder, this annotation will generate a Check
 * class that integrates with the ValidCheck library for field validation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Builder
 * @ValidCheck
 * public record User(String name, int age, String email) {
 *   public User {
 *     UserCheck.check()
 *         .checkName(name, StringValidator::notEmpty)
 *         .checkAge(age, NumericValidator::isPositive)
 *         .validate();
 *   }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ValidCheck {
}