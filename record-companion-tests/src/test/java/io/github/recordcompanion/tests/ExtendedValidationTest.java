package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.recordcompanion.tests.records.ExtendedValidationProfile;
import io.github.recordcompanion.tests.records.ExtendedValidationProfileBuilder;
import io.github.validcheck.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Test cases for extended ValidCheck integration with new annotations. */
public class ExtendedValidationTest {

  @Test
  public void testValidExtendedProfile() {
    // Test valid profile with all new validation features
    assertDoesNotThrow(
        () -> {
          ExtendedValidationProfile profile =
              new ExtendedValidationProfileBuilder()
                  .name("Valid Product")
                  .price(new BigDecimal("99.99"))
                  .rating(15.5)
                  .percentage(85.0)
                  .tags(List.of("tag1", "tag2", "tag3"))
                  .categories(Set.of("cat1", "cat2"))
                  .optionalItems(List.of("item1"))
                  .build();

          assertNotNull(profile);
          assertEquals("Valid Product", profile.name());
          assertEquals(new BigDecimal("99.99"), profile.price());
        });
  }

  @Test
  public void testDecimalMinValidation() {
    // Test @DecimalMin validation
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ExtendedValidationProfileBuilder()
                  .name("Test Product")
                  .price(new BigDecimal("-5.00")) // Below minimum of 0.0
                  .rating(15.5)
                  .percentage(85.0)
                  .tags(List.of("tag1"))
                  .categories(Set.of("cat1", "cat2"))
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testDecimalMaxValidation() {
    // Test @DecimalMax validation
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ExtendedValidationProfileBuilder()
                  .name("Test Product")
                  .price(new BigDecimal("99.99"))
                  .rating(15.5)
                  .percentage(150.0) // Above maximum of 100.0
                  .tags(List.of("tag1"))
                  .categories(Set.of("cat1", "cat2"))
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testCollectionSizeValidation() {
    // Test @Size validation on collections
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ExtendedValidationProfileBuilder()
                  .name("Test Product")
                  .price(new BigDecimal("99.99"))
                  .rating(15.5)
                  .percentage(85.0)
                  .tags(List.of()) // Empty list - below minimum of 1
                  .categories(Set.of("cat1", "cat2"))
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testSetSizeValidation() {
    // Test @Size validation on Set collections
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ExtendedValidationProfileBuilder()
                  .name("Test Product")
                  .price(new BigDecimal("99.99"))
                  .rating(15.5)
                  .percentage(85.0)
                  .tags(List.of("tag1"))
                  .categories(
                      Set.of("cat1", "cat2", "cat3", "cat4", "cat5", "cat6")) // Above maximum of 5
                  .build();
            });

    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testOptionalCollectionWithNull() {
    // Test optional collection (nullable) with null value
    assertDoesNotThrow(
        () -> {
          ExtendedValidationProfile profile =
              new ExtendedValidationProfileBuilder()
                  .name("Test Product")
                  .price(new BigDecimal("99.99"))
                  .rating(15.5)
                  .percentage(85.0)
                  .tags(List.of("tag1"))
                  .categories(Set.of("cat1", "cat2"))
                  .optionalItems(null) // Null should be allowed
                  .build();

          assertNotNull(profile);
        });
  }

  @Test
  public void testMultipleNewValidationErrors() {
    // Test multiple new validation failures at once
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new ExtendedValidationProfileBuilder()
                  .name("X") // Too short (min 2)
                  .price(new BigDecimal("1500.00")) // Above maximum (999.99)
                  .rating(5.0) // Below minimum (10.5)
                  .percentage(150.0) // Above maximum (100.0)
                  .tags(List.of()) // Empty - below minimum (1)
                  .categories(Set.of("single")) // Below minimum (2)
                  .build();
            });

    // Count validation errors - should have multiple errors
    int errorCount = exception.getErrors().size();
    assertNotNull(exception.getErrors());
    System.out.println("Total extended validation errors: " + errorCount);

    // Print all error messages for debugging
    exception.getErrors().forEach(System.out::println);
  }
}
