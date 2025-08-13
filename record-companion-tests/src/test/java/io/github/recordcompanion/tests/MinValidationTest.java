package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.ValidatedProduct;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class MinValidationTest {

  @Test
  void testValidProductCreation() {
    ValidatedProduct product = new ValidatedProduct("Laptop", 1000, 5L, "Gaming laptop");
    
    assertEquals("Laptop", product.name());
    assertEquals(1000, product.price());
    assertEquals(5L, product.quantity());
    assertEquals("Gaming laptop", product.description());
  }

  @Test
  void testNullNameThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new ValidatedProduct(null, 1000, 5L, "Gaming laptop")
    );
    
    assertTrue(exception.getConstraintViolations().stream()
        .anyMatch(v -> v.getMessage().contains("name cannot be null")));
  }

  @Test
  void testNullQuantityThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new ValidatedProduct("Laptop", 1000, null, "Gaming laptop")
    );
    
    assertTrue(exception.getConstraintViolations().stream()
        .anyMatch(v -> v.getMessage().contains("quantity cannot be null")));
  }

  @Test
  void testPriceTooLowThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new ValidatedProduct("Laptop", 0, 5L, "Gaming laptop")
    );
    
    assertTrue(exception.getConstraintViolations().stream()
        .anyMatch(v -> v.getMessage().contains("price must be at least 1")));
  }

  @Test
  void testQuantityTooLowThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new ValidatedProduct("Laptop", 1000, -1L, "Gaming laptop")
    );
    
    assertTrue(exception.getConstraintViolations().stream()
        .anyMatch(v -> v.getMessage().contains("quantity must be at least 0")));
  }

  @Test
  void testMultipleViolations() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new ValidatedProduct(null, 0, -5L, "Gaming laptop")
    );
    
    assertEquals(3, exception.getConstraintViolations().size());
  }

  @Test
  void testMinimumValidValues() {
    // Test minimum valid values
    assertDoesNotThrow(() -> new ValidatedProduct("Test", 1, 0L, null));
  }

  @Test
  void testNullDescriptionAllowed() {
    // description is not annotated, so null should be allowed
    assertDoesNotThrow(() -> new ValidatedProduct("Laptop", 1000, 5L, null));
  }
}