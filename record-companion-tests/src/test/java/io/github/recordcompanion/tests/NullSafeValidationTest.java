package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.recordcompanion.tests.records.NullSafeTestRecord;
import io.github.recordcompanion.tests.records.NullSafeTestRecordBuilder;
import io.github.validcheck.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests specifically for null-safe validation variants (nullOr* methods). */
public class NullSafeValidationTest {

  @Test
  public void testAllOptionalFieldsNull() {
    // Test that null optional fields are accepted (nullOr* methods should pass)
    assertDoesNotThrow(
        () -> {
          NullSafeTestRecord record =
              new NullSafeTestRecordBuilder()
                  .requiredText("test")
                  .requiredNumber(50)
                  .requiredPrice(new BigDecimal("99.99"))
                  // All optional fields set to null
                  .optionalText(null)
                  .optionalCode(null)
                  .optionalTitle(null)
                  .optionalMinValue(null)
                  .optionalMaxValue(null)
                  .optionalMinDecimal(null)
                  .optionalMaxDecimal(null)
                  .optionalList(null)
                  .optionalSet(null)
                  .build();

          assertNotNull(record);
          assertEquals("test", record.requiredText());
        });
  }

  @Test
  public void testOptionalFieldsWithValidValues() {
    // Test that valid optional fields pass validation
    assertDoesNotThrow(
        () -> {
          new NullSafeTestRecordBuilder()
              .requiredText("test")
              .requiredNumber(50)
              .requiredPrice(new BigDecimal("99.99"))
              .optionalText("valid text here")
              .optionalCode("VALID")
              .optionalTitle("Valid Title")
              .optionalMinValue(15)
              .optionalMaxValue(500)
              .optionalMinDecimal(10.5)
              .optionalMaxDecimal(75.0)
              .optionalList(List.of("item1", "item2"))
              .optionalSet(Set.of("cat1", "cat2", "cat3"))
              .build();
        });
  }

  @Test
  public void testOptionalFieldsWithInvalidValues() {
    // Test that invalid optional field values are still caught by nullOr* methods
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new NullSafeTestRecordBuilder()
                  .requiredText("test")
                  .requiredNumber(50)
                  .requiredPrice(new BigDecimal("99.99"))
                  .optionalText("abc") // Invalid: too short (< 5)
                  .optionalCode("123") // Invalid: pattern mismatch (should be [A-Z]+)
                  .optionalTitle("   ") // Invalid: blank string
                  .optionalMinValue(5) // Invalid: below minimum (< 10)
                  .optionalMaxValue(2000) // Invalid: above maximum (> 1000)
                  .optionalMinDecimal(2.0) // Invalid: below minimum (< 5.0)
                  .optionalMaxDecimal(150.0) // Invalid: above maximum (> 100.0)
                  .optionalList(List.of()) // Invalid: below minimum size (< 1)
                  .optionalSet(Set.of("single")) // Invalid: below minimum size (< 2)
                  .build();
            });

    // Should have multiple validation errors from nullOr* methods
    assertTrue(
        exception.getErrors().size() >= 8,
        "Expected at least 8 nullOr* validation errors, got " + exception.getErrors().size());

    System.out.println("Null-safe validation errors: " + exception.getErrors().size());
    exception
        .getErrors()
        .forEach(
            error -> {
              // All errors should contain "null or" in the message indicating nullOr* method was
              // used
              assertTrue(
                  error.contains("must be null or"),
                  "Error should indicate null-safe validation: " + error);
            });
  }

  @Test
  public void testRequiredFieldsStillRequired() {
    // Test that required fields (@NotNull) still fail when null
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new NullSafeTestRecordBuilder()
                  .requiredText(null) // Invalid: NotNull
                  .requiredNumber(null) // Invalid: NotNull
                  .requiredPrice(null) // Invalid: NotNull
                  // Optional fields can be null
                  .optionalText(null)
                  .optionalCode(null)
                  .optionalTitle(null)
                  .optionalMinValue(null)
                  .optionalMaxValue(null)
                  .optionalMinDecimal(null)
                  .optionalMaxDecimal(null)
                  .optionalList(null)
                  .optionalSet(null)
                  .build();
            });

    assertEquals(6, exception.getErrors().size());
  }

  @Test
  public void testMixedNullAndValidOptionalFields() {
    // Test mixing null and valid values for optional fields
    assertDoesNotThrow(
        () -> {
          new NullSafeTestRecordBuilder()
              .requiredText("test")
              .requiredNumber(50)
              .requiredPrice(new BigDecimal("99.99"))
              .optionalText("valid text here") // Valid value
              .optionalCode(null) // Null value
              .optionalTitle("Valid Title") // Valid value
              .optionalMinValue(null) // Null value
              .optionalMaxValue(500) // Valid value
              .optionalMinDecimal(null) // Null value
              .optionalMaxDecimal(75.0) // Valid value
              .optionalList(List.of("item1")) // Valid value
              .optionalSet(null) // Null value
              .build();
        });
  }

  @Test
  public void testNullSafeStringValidations() {
    // Specifically test null-safe string validations
    assertDoesNotThrow(
        () -> {
          new NullSafeTestRecordBuilder()
              .requiredText("test")
              .requiredNumber(50)
              .requiredPrice(new BigDecimal("99.99"))
              .optionalText(null) // nullOrHasLength should pass
              .optionalCode(null) // nullOrMatches should pass
              .optionalTitle(null) // nullOrNotBlank should pass
              .build();
        });

    // Test invalid values are still caught
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new NullSafeTestRecordBuilder()
                  .requiredText("test")
                  .requiredNumber(50)
                  .requiredPrice(new BigDecimal("99.99"))
                  .optionalText("x") // Invalid: too short for nullOrHasLength
                  .optionalCode("abc") // Invalid: pattern mismatch for nullOrMatches
                  .optionalTitle("") // Invalid: blank for nullOrNotBlank
                  .build();
            });

    assertEquals(3, exception.getErrors().size());
  }

  @Test
  public void testNullSafeNumericValidations() {
    // Specifically test null-safe numeric validations
    assertDoesNotThrow(
        () -> {
          new NullSafeTestRecordBuilder()
              .requiredText("test")
              .requiredNumber(50)
              .requiredPrice(new BigDecimal("99.99"))
              .optionalMinValue(null) // nullOrMin should pass
              .optionalMaxValue(null) // nullOrMax should pass
              .optionalMinDecimal(null) // nullOrMin should pass
              .optionalMaxDecimal(null) // nullOrMax should pass
              .build();
        });

    // Test invalid values are still caught
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new NullSafeTestRecordBuilder()
                  .requiredText("test")
                  .requiredNumber(50)
                  .requiredPrice(new BigDecimal("99.99"))
                  .optionalMinValue(5) // Invalid: below min for nullOrMin
                  .optionalMaxValue(2000) // Invalid: above max for nullOrMax
                  .optionalMinDecimal(2.0) // Invalid: below min for nullOrMin
                  .optionalMaxDecimal(150.0) // Invalid: above max for nullOrMax
                  .build();
            });

    assertEquals(4, exception.getErrors().size());
  }

  @Test
  public void testNullSafeCollectionValidations() {
    // Specifically test null-safe collection validations
    assertDoesNotThrow(
        () -> {
          new NullSafeTestRecordBuilder()
              .requiredText("test")
              .requiredNumber(50)
              .requiredPrice(new BigDecimal("99.99"))
              .optionalList(null) // nullOrHasSize should pass
              .optionalSet(null) // nullOrHasSize should pass
              .build();
        });

    // Test invalid sizes are still caught
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new NullSafeTestRecordBuilder()
                  .requiredText("test")
                  .requiredNumber(50)
                  .requiredPrice(new BigDecimal("99.99"))
                  .optionalList(List.of()) // Invalid: empty for nullOrHasSize (min 1)
                  .optionalSet(Set.of("single")) // Invalid: too small for nullOrHasSize (min 2)
                  .build();
            });

    assertEquals(2, exception.getErrors().size());
  }
}
