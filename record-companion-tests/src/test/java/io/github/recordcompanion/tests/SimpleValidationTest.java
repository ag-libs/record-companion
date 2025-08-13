package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.ValidatedUser;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class SimpleValidationTest {

  @Test
  void testValidRecordCreation() {
    ValidatedUser user = new ValidatedUser("John", 30, "john@example.com");
    
    assertEquals("John", user.name());
    assertEquals(30, user.age());
    assertEquals("john@example.com", user.email());
  }

  @Test
  void testNullNameThrowsException() {
    ConstraintViolationException exception = assertThrows(
        ConstraintViolationException.class,
        () -> new ValidatedUser(null, 30, "john@example.com")
    );
    
    assertFalse(exception.getConstraintViolations().isEmpty());
    assertEquals(1, exception.getConstraintViolations().size());
    
    var violation = exception.getConstraintViolations().iterator().next();
    assertEquals("name cannot be null", violation.getMessage());
  }

  @Test
  void testNullAgeIsAllowed() {
    // age is not annotated with @NotNull, so null should be allowed
    assertDoesNotThrow(() -> new ValidatedUser("John", null, "john@example.com"));
  }

  @Test
  void testNullEmailIsAllowed() {
    // email is not annotated with @NotNull, so null should be allowed
    assertDoesNotThrow(() -> new ValidatedUser("John", 30, null));
  }
}