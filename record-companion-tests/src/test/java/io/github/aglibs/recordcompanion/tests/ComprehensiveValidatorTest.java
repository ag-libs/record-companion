package io.github.aglibs.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.aglibs.recordcompanion.tests.records.ComprehensiveValidationRecord;
import io.github.aglibs.recordcompanion.tests.records.ComprehensiveValidationRecordBuilder;
import io.github.aglibs.recordcompanion.tests.records.ComprehensiveValidationRecordCheck;
import io.github.aglibs.validcheck.BatchValidator;
import io.github.aglibs.validcheck.ValidationException;
import io.github.aglibs.validcheck.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Comprehensive test coverage for all ValidCheck integration features. */
public class ComprehensiveValidatorTest {

  @Test
  public void testValidComprehensiveRecord() {
    // Test valid record with all required fields and some optional fields
    assertDoesNotThrow(
        () -> {
          ComprehensiveValidationRecord record =
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("TestName")
                  .optionalDescription("Valid description text")
                  .optionalCode("ABC")
                  .optionalTitle("Title")
                  .requiredScore(50)
                  .optionalMinOnly(15)
                  .optionalMaxOnly(500)
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .optionalMinRating(7.5)
                  .optionalMaxPercentage(85.0)
                  .requiredTags(List.of("tag1", "tag2"))
                  .optionalCategories(Set.of("cat1", "cat2"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();

          assertNotNull(record);
          assertEquals("TestName", record.requiredName());
          assertEquals(Integer.valueOf(50), record.requiredScore());
        });
  }

  @Test
  public void testValidRecordWithNullOptionalFields() {
    // Test valid record with null optional fields (should use nullOr* methods)
    assertDoesNotThrow(
        () -> {
          ComprehensiveValidationRecord record =
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("TestName")
                  .optionalDescription(null) // nullable - should use nullOrHasLength
                  .optionalCode(null) // nullable - should use nullOrMatches
                  .optionalTitle(null) // nullable - should use nullOrNotBlank
                  .requiredScore(50)
                  .optionalMinOnly(null) // nullable - should use nullOrMin
                  .optionalMaxOnly(null) // nullable - should use nullOrMax
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .optionalMinRating(null) // nullable - should use nullOrMin
                  .optionalMaxPercentage(null) // nullable - should use nullOrMax
                  .requiredTags(List.of("tag1"))
                  .optionalCategories(null) // nullable - should use nullOrHasSize
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();

          assertNotNull(record);
        });
  }

  @Test
  public void testGeneratedCheckMethod() {
    // Test the generated check() method returns BatchValidator
    BatchValidator batchValidator =
        ComprehensiveValidationRecordCheck.check(
            "TestName",
            "Valid description",
            "ABC",
            "Title",
            50,
            15,
            500,
            10,
            -5,
            0,
            -10,
            new BigDecimal("99.99"),
            7.5,
            85.0,
            List.of("tag1"),
            Set.of("cat1", "cat2"),
            Map.of("key", "value"),
            "username",
            25,
            4.5);

    assertNotNull(batchValidator);
    // BatchValidator should validate successfully for valid data
    assertDoesNotThrow(() -> batchValidator.validate());
  }

  @Test
  public void testGeneratedRequireMethod() {
    // Test the generated require() method returns Validator
    Validator validator =
        ComprehensiveValidationRecordCheck.require(
            "TestName",
            "Valid description",
            "ABC",
            "Title",
            50,
            15,
            500,
            10,
            -5,
            0,
            -10,
            new BigDecimal("99.99"),
            7.5,
            85.0,
            List.of("tag1"),
            Set.of("cat1", "cat2"),
            Map.of("key", "value"),
            "username",
            25,
            4.5);

    assertNotNull(validator);
    // Validator should be valid for valid data - no exception thrown
  }

  @Test
  public void testPositiveValidation() {
    // Test @Positive validation
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("Test")
                  .requiredScore(50)
                  .positiveCount(-1) // Invalid: must be positive
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
    assertTrue(exception.getErrors().get(0).contains("positiveCount"));
  }

  @Test
  public void testNegativeValidation() {
    // Test @Negative validation
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("Test")
                  .requiredScore(50)
                  .positiveCount(10)
                  .negativeOffset(1) // Invalid: must be negative
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
    assertTrue(exception.getErrors().get(0).contains("negativeOffset"));
  }

  @Test
  public void testPositiveOrZeroValidation() {
    // Test @PositiveOrZero validation
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("Test")
                  .requiredScore(50)
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(-1) // Invalid: must be >= 0
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
    assertTrue(exception.getErrors().get(0).contains("nonNegativeAmount"));
  }

  @Test
  public void testNegativeOrZeroValidation() {
    // Test @NegativeOrZero validation
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
                  .nonPositiveBalance(1) // Invalid: must be <= 0
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
    assertTrue(exception.getErrors().get(0).contains("nonPositiveBalance"));
  }

  @Test
  public void testBoundaryValues() {
    // Test boundary values for Min/Max validations
    assertDoesNotThrow(
        () -> {
          new ComprehensiveValidationRecordBuilder()
              .requiredName("A") // Minimum length (1)
              .requiredScore(1) // Minimum value
              .optionalMinOnly(10) // Exactly at minimum
              .optionalMaxOnly(1000) // Exactly at maximum
              .positiveCount(1) // Minimum positive
              .negativeOffset(-1) // Maximum negative
              .nonNegativeAmount(0) // Boundary: zero
              .nonPositiveBalance(0) // Boundary: zero
              .requiredPrice(new BigDecimal("0.01")) // Minimum decimal
              .optionalMinRating(5.0) // Exactly at minimum
              .optionalMaxPercentage(100.0) // Exactly at maximum
              .requiredTags(List.of("x")) // Minimum size (1)
              .optionalCategories(Set.of("a", "b")) // Minimum size (2)
              .requiredMetadata(Map.of("k", "v")) // Minimum non-empty
              .validatedUsername("abc") // Minimum length (3)
              .ageRange(18) // Minimum age
              .starRating(0.0) // Minimum rating
              .build();
        });
  }

  @Test
  public void testComplexPatternValidation() {
    // Test complex regex pattern validation
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
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("user123") // Invalid: contains numbers, pattern is [a-zA-Z]+
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
    assertTrue(exception.getErrors().get(0).contains("validatedUsername"));
    assertTrue(exception.getErrors().get(0).contains("pattern"));
  }

  @Test
  public void testMultipleValidationErrors() {
    // Test multiple validation errors across different annotation types
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("") // Invalid: blank, too short
                  .optionalDescription("abc") // Invalid: too short (min 5)
                  .optionalCode("X") // Invalid: pattern mismatch
                  .requiredScore(0) // Invalid: below minimum (1)
                  .optionalMinOnly(5) // Invalid: below minimum (10)
                  .optionalMaxOnly(2000) // Invalid: above maximum (1000)
                  .positiveCount(-10) // Invalid: not positive
                  .negativeOffset(10) // Invalid: not negative
                  .nonNegativeAmount(-5) // Invalid: negative
                  .nonPositiveBalance(5) // Invalid: positive
                  .requiredPrice(new BigDecimal("0.00")) // Invalid: below minimum (0.01)
                  .optionalMinRating(2.0) // Invalid: below minimum (5.0)
                  .optionalMaxPercentage(150.0) // Invalid: above maximum (100.0)
                  .requiredTags(List.of()) // Invalid: empty, below minimum size
                  .optionalCategories(Set.of("single")) // Invalid: below minimum size (2)
                  .requiredMetadata(Map.of()) // Invalid: empty (NotEmpty)
                  .validatedUsername("a") // Invalid: too short (min 3)
                  .ageRange(15) // Invalid: below minimum (18)
                  .starRating(-1.0) // Invalid: negative (PositiveOrZero)
                  .build();
            });

    // Should have multiple validation errors
    int errorCount = exception.getErrors().size();
    assertTrue(errorCount >= 10, "Expected at least 10 errors, got " + errorCount);

    System.out.println("Total comprehensive validation errors: " + errorCount);
    exception.getErrors().forEach(System.out::println);
  }

  @Test
  public void testCollectionSizeValidations() {
    // Test various collection size validations
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
                  .requiredTags(List.of("a", "b", "c", "d", "e", "f")) // Invalid: above maximum (5)
                  .optionalCategories(
                      Set.of(
                          "cat1", "cat2", "cat3", "cat4", "cat5", "cat6", "cat7", "cat8", "cat9",
                          "cat10", "cat11")) // Invalid: above maximum (10)
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(2, exception.getErrors().size());

    // Check that each collection error is properly identified
    List<String> errors = exception.getErrors();
    assertTrue(errors.stream().anyMatch(e -> e.contains("requiredTags") && e.contains("size")));
    assertTrue(
        errors.stream().anyMatch(e -> e.contains("optionalCategories") && e.contains("size")));
  }

  @Test
  public void testErrorMessageContentValidation() {
    // Test that error messages contain expected information
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ComprehensiveValidationRecordBuilder()
                  .requiredName("Test")
                  .requiredScore(150) // Invalid: above maximum (100)
                  .positiveCount(10)
                  .negativeOffset(-5)
                  .nonNegativeAmount(0)
                  .nonPositiveBalance(-10)
                  .requiredPrice(new BigDecimal("99.99"))
                  .requiredTags(List.of("tag1"))
                  .requiredMetadata(Map.of("key", "value"))
                  .validatedUsername("username")
                  .ageRange(25)
                  .starRating(4.5)
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
    String errorMessage = exception.getErrors().get(0);

    // Verify error message contains field name and boundary information
    assertTrue(errorMessage.contains("requiredScore"));
    assertTrue(errorMessage.contains("100")); // max value
    assertTrue(errorMessage.contains("150")); // actual value
    assertFalse(errorMessage.isEmpty());
  }
}
