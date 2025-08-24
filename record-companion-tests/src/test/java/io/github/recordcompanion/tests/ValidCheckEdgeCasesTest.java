package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.recordcompanion.tests.records.ComprehensiveValidationRecord;
import io.github.recordcompanion.tests.records.ComprehensiveValidationRecordBuilder;
import io.github.validcheck.ValidationException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Edge case and boundary condition tests for ValidCheck integration. */
public class ValidCheckEdgeCasesTest {

  @Test
  public void testBoundaryValuesPrecise() {
    // Test exact boundary values work correctly
    assertDoesNotThrow(
        () -> {
          ComprehensiveValidationRecord record =
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("A") // Exactly min length (1)
                  .optionalDescription("12345") // Exactly min length (5)
                  .optionalCode("AB") // Exactly min length (2)
                  .requiredScore(1) // Exactly min value
                  .optionalMinOnly(10) // Exactly min value
                  .optionalMaxOnly(1000) // Exactly max value
                  .positiveCount(1) // Minimum positive value
                  .negativeOffset(-1) // Maximum negative value
                  .nonNegativeAmount(0) // Boundary zero
                  .nonPositiveBalance(0) // Boundary zero
                  .requiredPrice(new BigDecimal("0.01")) // Exactly min decimal
                  .optionalMinRating(5.0) // Exactly min decimal
                  .optionalMaxPercentage(100.0) // Exactly max decimal
                  .requiredTags(List.of("x")) // Exactly min size (1)
                  .optionalCategories(Set.of("a", "b")) // Exactly min size (2)
                  .requiredMetadata(Map.of("k", "v"))
                  .validatedUsername("abc") // Exactly min length (3)
                  .ageRange(18) // Exactly min value
                  .starRating(0.0) // Exactly min value
                  .build();

          assertNotNull(record);
          assertEquals("A", record.requiredName());
        });
  }

  @Test
  public void testUpperBoundaryValues() {
    // Test exact upper boundary values
    assertDoesNotThrow(
        () -> {
          new ComprehensiveValidationRecordBuilder()
              .requiredName("A".repeat(50)) // Exactly max length (50)
              .optionalDescription("x".repeat(100)) // Exactly max length (100)
              .optionalCode("ABCD") // Exactly max length (4)
              .requiredScore(100) // Exactly max value
              .optionalMinOnly(Integer.MAX_VALUE) // No upper bound
              .optionalMaxOnly(1000) // Exactly max value
              .positiveCount(Integer.MAX_VALUE) // Maximum positive
              .negativeOffset(-1) // Just negative
              .nonNegativeAmount(Integer.MAX_VALUE) // Maximum non-negative
              .nonPositiveBalance(0) // Maximum non-positive
              .requiredPrice(new BigDecimal("999.99")) // Exactly max decimal
              .optionalMinRating(Double.MAX_VALUE) // No upper bound
              .optionalMaxPercentage(100.0) // Exactly max decimal
              .requiredTags(List.of("a", "b", "c", "d", "e")) // Exactly max size (5)
              .optionalCategories(
                  Set.of(
                      "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")) // Exactly max size (10)
              .requiredMetadata(Map.of("key1", "val1", "key2", "val2"))
              .validatedUsername("a".repeat(20)) // Exactly max length (20)
              .ageRange(120) // Exactly max value
              .starRating(5.0) // Exactly max decimal and positive
              .build();
        });
  }

  @Test
  public void testOffByOneBoundaryViolations() {
    // Test values just outside boundaries (off-by-one errors)
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("") // Below min length (0 < 1)
                  .optionalDescription("1234") // Below min length (4 < 5)
                  .requiredScore(0) // Below min value (0 < 1)
                  .optionalMinOnly(9) // Below min value (9 < 10)
                  .optionalMaxOnly(1001) // Above max value (1001 > 1000)
                  .positiveCount(0) // Not positive (0 <= 0)
                  .negativeOffset(0) // Not negative (0 >= 0)
                  .nonNegativeAmount(-1) // Below zero (-1 < 0)
                  .nonPositiveBalance(1) // Above zero (1 > 0)
                  .requiredPrice(new BigDecimal("0.00")) // Below min decimal (0.00 < 0.01)
                  .optionalMinRating(4.9) // Below min decimal (4.9 < 5.0)
                  .optionalMaxPercentage(100.1) // Above max decimal (100.1 > 100.0)
                  .requiredTags(List.of()) // Below min size (0 < 1)
                  .optionalCategories(Set.of("single")) // Below min size (1 < 2)
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("ab") // Below min length (2 < 3)
                  .ageRange(17) // Below min value (17 < 18)
                  .starRating(-0.1) // Below zero (-0.1 < 0)
                  .build();
            });

    // Should have multiple off-by-one boundary violations
    assertTrue(
        exception.getErrors().size() >= 10,
        "Expected at least 10 boundary violations, got " + exception.getErrors().size());
    System.out.println("Boundary violation errors: " + exception.getErrors().size());
  }

  @Test
  public void testEmptyCollections() {
    // Test empty collections where not allowed
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("Test")
                  .requiredScore(50)
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(Collections.emptyList()) // Invalid: NotEmpty + Size min 1
                  .requiredMetadata(Collections.emptyMap()) // Invalid: NotEmpty
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(3, exception.getErrors().size());
    assertTrue(exception.getErrors().stream().anyMatch(e -> e.contains("requiredTags")));
    assertTrue(exception.getErrors().stream().anyMatch(e -> e.contains("requiredMetadata")));
  }

  @Test
  public void testNullRequiredFields() {
    // Test null values for required (@NotNull) fields
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName(null) // Invalid: NotNull
                  .requiredScore(null) // Invalid: NotNull
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(null) // Invalid: NotNull
                  .requiredTags(null) // Invalid: NotNull
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername(null) // Invalid: NotNull
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(14, exception.getErrors().size());
  }

  @Test
  public void testBlankStringsVsEmptyStrings() {
    // Test difference between blank and empty string validation
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("   ") // Invalid: NotBlank (whitespace only)
                  .optionalTitle(
                      "   ") // Invalid: NotBlank (whitespace only) - nullable but has NotBlank
                  .requiredScore(50)
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername(
                      "   ") // Invalid: NotBlank + Pattern (whitespace doesn't match [a-zA-Z]+)
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertTrue(exception.getErrors().size() >= 2);
    // Check that blank validation is properly triggered
    assertTrue(
        exception.getErrors().stream()
            .anyMatch(
                e ->
                    e.contains("requiredName")
                        && (e.contains("blank") || e.contains("not blank"))));
  }

  @Test
  public void testDecimalPrecisionBoundaries() {
    // Test decimal precision at boundaries
    assertDoesNotThrow(
        () -> {
          new ComprehensiveValidationRecordBuilder()
              .requiredName("Test")
              .requiredScore(50)
              .positiveCount(10)
              .negativeOffset(-5)
              .nonNegativeAmount(0)
              .nonPositiveBalance(-10)
              .requiredPrice(new BigDecimal("0.01")) // Exactly at minimum
              .optionalMinRating(5.000000001) // Just above minimum
              .optionalMaxPercentage(99.999999999) // Just below maximum
              .requiredTags(List.of("tag1"))
              .requiredMetadata(Map.of("key", "value"))
              .validatedUsername("username")
              .ageRange(25)
              .starRating(0.0000001) // Just above zero
              .build();
        });
  }

  @Test
  public void testRegexPatternEdgeCases() {
    // Test regex pattern validation with various edge cases
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("Test")
                  .optionalCode("12345") // Invalid: pattern expects [A-Z]{2,4}
                  .requiredScore(50)
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername(
                      "USER_NAME") // Invalid: pattern expects [a-zA-Z]+ (no underscore)
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(2, exception.getErrors().size());
    assertTrue(
        exception.getErrors().stream()
            .anyMatch(e -> e.contains("optionalCode") && e.contains("pattern")));
    assertTrue(
        exception.getErrors().stream()
            .anyMatch(e -> e.contains("validatedUsername") && e.contains("pattern")));
  }

  @Test
  public void testLargeCollections() {
    // Test collections at maximum allowed size
    assertDoesNotThrow(
        () -> {
          new ComprehensiveValidationRecordBuilder()
              .requiredName("Test")
              .requiredScore(50)
              .positiveCount(10)
              .negativeOffset(-5)
              .nonNegativeAmount(0)
              .nonPositiveBalance(-10)
              .requiredPrice(new BigDecimal("99.99"))
              .requiredTags(List.of("tag1", "tag2", "tag3", "tag4", "tag5")) // Max size (5)
              .optionalCategories(
                  Set.of(
                      "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10")) // Max size (10)
              .requiredMetadata(Map.of("key", "value"))
              .validatedUsername("username")
              .ageRange(25)
              .starRating(4.5)
              .build();
        });
  }

  @Test
  public void testSpecialNumericValues() {
    // Test special numeric values like MAX_VALUE, MIN_VALUE
    assertDoesNotThrow(
        () -> {
          new ComprehensiveValidationRecordBuilder()
              .requiredName("Test")
              .requiredScore(50)
              .positiveCount(Integer.MAX_VALUE) // Should work for positive
              .negativeOffset(Integer.MIN_VALUE) // Should work for negative
              .nonNegativeAmount(Integer.MAX_VALUE) // Should work for >= 0
              .nonPositiveBalance(Integer.MIN_VALUE) // Should work for <= 0
              .requiredPrice(new BigDecimal("999.99"))
              .requiredTags(List.of("tag1"))
              .requiredMetadata(Map.of("key", "value"))
              .validatedUsername("username")
              .ageRange(25)
              .starRating(5.0) // Max allowed
              .build();
        });
  }
}
