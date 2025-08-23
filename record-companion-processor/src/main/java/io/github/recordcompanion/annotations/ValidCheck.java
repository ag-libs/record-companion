package io.github.recordcompanion.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to generate ValidCheck integration for Java records with Bean Validation support.
 *
 * <p>When applied to a record class, this annotation will generate a Check class that automatically
 * maps Bean Validation annotations to ValidCheck API calls. The generated class provides static
 * methods for validation using the ValidCheck library.
 *
 * <p><strong>Supported Bean Validation Annotations:</strong>
 *
 * <ul>
 *   <li>{@code @NotNull} - Maps to {@code .notNull(value, fieldName)}
 *   <li>{@code @NotEmpty} - Maps to {@code .notNullOrEmpty(value, fieldName)}
 *   <li>{@code @Size(min, max)} - Maps to {@code .hasLength(value, min, max, fieldName)}
 *   <li>{@code @Pattern(regexp)} - Maps to {@code .matches(value, pattern, fieldName)}
 *   <li>{@code @Min + @Max} (combined) - Maps to {@code .inRange(value, min, max, fieldName)}
 *   <li>{@code @Positive} - Maps to {@code .inRange(value, 1, Integer.MAX_VALUE, fieldName)}
 *   <li>{@code @Negative} - Maps to {@code .inRange(value, Integer.MIN_VALUE, -1, fieldName)}
 *   <li>{@code @PositiveOrZero} - Maps to {@code .inRange(value, 0, Integer.MAX_VALUE, fieldName)}
 *   <li>{@code @NegativeOrZero} - Maps to {@code .inRange(value, Integer.MIN_VALUE, 0, fieldName)}
 * </ul>
 *
 * <p><strong>Note:</strong> Individual {@code @Min} and {@code @Max} annotations (without the
 * other) and {@code @NotBlank} are currently not supported but may be added in future releases.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * @Builder
 * @ValidCheck
 * public record UserProfile(
 *     @NotNull @Size(min = 3, max = 20) String username,
 *     @Min(0) @Max(100) int score,
 *     @NotEmpty Map<String, String> metadata,
 *     @Pattern(regexp = ".*") String address) {
 *
 *   public UserProfile {
 *     // Validate using generated check methods
 *     UserProfileCheck.validate(username, score, metadata, address);
 *   }
 * }
 * }</pre>
 *
 * <p>The generated {@code UserProfileCheck} class provides three static methods:
 *
 * <ul>
 *   <li>{@code check(...)} - Returns {@code BatchValidator} for manual validation control
 *   <li>{@code require(...)} - Returns {@code Validator} for immediate validation with chaining
 *   <li>{@code validate(...)} - Convenience method that validates and throws on failure
 * </ul>
 *
 * <p><strong>Generated validation chain example:</strong>
 *
 * <pre>{@code
 * return validator
 *     .notNull(username, "username")
 *     .hasLength(username, 3, 20, "username")
 *     .inRange(score, 0, 100, "score")
 *     .notNullOrEmpty(metadata, "metadata")
 *     .matches(address, ".*", "address");
 * }</pre>
 *
 * @since 0.1.1
 * @see <a href="https://github.com/validcheck/validcheck">ValidCheck Library</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ValidCheck {}
