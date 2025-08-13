package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.MultiValidatedUser;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class MultiValidationTest {

  @Test
  void testValidRecordCreation() {
    MultiValidatedUser user = new MultiValidatedUser("John", 30, "john@example.com", "555-1234");
    
    assertEquals("John", user.name());
    assertEquals(30, user.age());
    assertEquals("john@example.com", user.email());
    assertEquals("555-1234", user.phone());
  }

  @Test
  void testNullNameThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new MultiValidatedUser(null, 30, "john@example.com", "555-1234")
    );
    
    assertEquals(1, exception.getConstraintViolations().size());
    var violation = exception.getConstraintViolations().iterator().next();
    assertEquals("name cannot be null", violation.getMessage());
  }

  @Test
  void testNullEmailThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new MultiValidatedUser("John", 30, null, "555-1234")
    );
    
    assertEquals(1, exception.getConstraintViolations().size());
    var violation = exception.getConstraintViolations().iterator().next();
    assertEquals("email cannot be null", violation.getMessage());
  }

  @Test
  void testBothNameAndEmailNullThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new MultiValidatedUser(null, 30, null, "555-1234")
    );
    
    assertEquals(2, exception.getConstraintViolations().size());
  }

  @Test
  void testNonValidatedFieldsCanBeNull() {
    // age and phone are not annotated with @NotNull, so they should be allowed to be null
    assertDoesNotThrow(() -> new MultiValidatedUser("John", null, "john@example.com", null));
  }
}