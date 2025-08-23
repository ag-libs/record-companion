package io.github.recordcompanion.tests.records;

import io.github.validcheck.BatchValidator;
import io.github.validcheck.ValidCheck;
import io.github.validcheck.Validator;
import java.util.Map;

/**
 * Generated check class for UserProfile record.
 *
 * <p>This class provides validation methods that map Bean Validation annotations to ValidCheck API
 * calls for components with validation annotations.
 */
public final class UserProfileCheckTemplate {

  private UserProfileCheckTemplate() {
    // Utility class
  }

  /**
   * Creates a batch validator for UserProfile validation.
   *
   * @param username the username to validate (from @NotNull @Size annotations)
   * @param score the score to validate (from @Min @Max annotations)
   * @param metadata the metadata to validate (from @NotEmpty annotation)
   * @return BatchValidator for manual validation control
   */
  public static BatchValidator check(String username, int score, Map<String, String> metadata) {
    return (BatchValidator) buildValidation(ValidCheck.check(), username, score, metadata);
  }

  /**
   * Creates a validator for UserProfile validation with immediate validation.
   *
   * @param username the username to validate (from @NotNull @Size annotations)
   * @param score the score to validate (from @Min @Max annotations)
   * @param metadata the metadata to validate (from @NotEmpty annotation)
   * @return Validator for chaining additional validations
   */
  public static Validator require(String username, int score, Map<String, String> metadata) {
    return buildValidation(ValidCheck.require(), username, score, metadata);
  }

  /**
   * Convenience method that validates UserProfile and throws on failure.
   *
   * @param username the username to validate (from @NotNull @Size annotations)
   * @param score the score to validate (from @Min @Max annotations)
   * @param metadata the metadata to validate (from @NotEmpty annotation)
   */
  public static void validate(String username, int score, Map<String, String> metadata) {
    check(username, score, metadata).validate();
  }

  /**
   * Builds validation chain from Bean Validation annotations.
   *
   * @param validator the base validator instance
   * @param username the username to validate
   * @param score the score to validate
   * @param metadata the metadata to validate
   * @return validator with validation chain applied
   */
  private static Validator buildValidation(
      Validator validator, String username, int score, Map<String, String> metadata) {
    return validator
        .notNull(username, "username") // from @NotNull
        .hasLength(username, 3, 20, "username") // from @Size(min=3, max=20)
        .inRange(score, 0, 100, "score") // from @Min(0) @Max(100)
        .notNullOrEmpty(metadata, "metadata"); // from @NotEmpty
  }
}
