package io.github.recordcompanion.tests;

import static io.github.recordcompanion.tests.records.UserProfileCheck.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.validcheck.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for static field-specific check methods with check prefix. */
public class StaticValidationTest {

  @Test
  public void testStaticStringValidation() {
    // Test static username validation - should pass
    assertDoesNotThrow(
        () -> {
          checkUsername("john_doe").notNull().lengthBetween(3, 20);
        });

    // Test static username validation - should fail
    assertThrows(
        ValidationException.class,
        () -> {
          checkUsername("x").notNull().lengthBetween(3, 20);
        });
  }

  @Test
  public void testStaticNumericValidation() {
    // Test static score validation - should pass
    assertDoesNotThrow(
        () -> {
          checkScore(95).isPositive().max(100);
        });

    // Test static score validation - should fail
    assertThrows(
        ValidationException.class,
        () -> {
          checkScore(-5).isPositive().max(100);
        });
  }

  @Test
  public void testStaticMapValidation() {
    // Test static metadata validation - should pass
    assertDoesNotThrow(
        () -> {
          checkMetadata(Map.of("role", "admin")).notNull().notEmpty();
        });

    // Test static metadata validation - should fail
    assertThrows(
        ValidationException.class,
        () -> {
          checkMetadata(Map.of()).notNull().notEmpty();
        });
  }

  @Test
  public void testConditionalValidation() {
    String username = "alice";
    int score = 75;

    // Conditional validation using static methods
    if (username.length() > 0) {
      assertDoesNotThrow(
          () -> {
            checkUsername(username).notNull().lengthBetween(3, 20);
          });
    }

    if (score >= 0) {
      assertDoesNotThrow(
          () -> {
            checkScore(score).isPositive().max(100);
          });
    }
  }

  @Test
  public void testMethodChaining() {
    // Demonstrate clean method chaining with static imports
    assertDoesNotThrow(
        () -> {
          checkUsername("john").notNull().lengthBetween(3, 20);
          checkScore(85).isPositive().max(100);
          checkMetadata(Map.of("status", "active")).notNull().notEmpty();
        });
  }

  @Test
  public void testNoNamingConflicts() {
    // Demonstrate that static imports don't conflict with local variables
    String username = "john_doe";
    int score = 95;
    Map<String, String> metadata = Map.of("role", "admin");

    // These static method calls don't conflict with the local variables
    assertDoesNotThrow(
        () -> {
          checkUsername(username).notNull().lengthBetween(3, 20);
          checkScore(score).isPositive().max(100);
          checkMetadata(metadata).notNull().notEmpty();
        });
  }
}
