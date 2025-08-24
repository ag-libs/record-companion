package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.recordcompanion.tests.records.EnhancedUserProfile;
import io.github.recordcompanion.tests.records.EnhancedUserProfileBuilder;
import io.github.validcheck.ValidationException;
import org.junit.jupiter.api.Test;

/** Test cases for enhanced ValidCheck integration demonstrating new API features. */
public class EnhancedValidatorTest {

  @Test
  public void testValidEnhancedUserProfile() {
    // Test valid profile with all fields
    assertDoesNotThrow(
        () -> {
          EnhancedUserProfile profile =
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(25)
                  .minFollowers(10)
                  .maxConnections(1000)
                  .email("test@example.com")
                  .bio("A test user profile for validation testing")
                  .displayName("Test User")
                  .build();

          assertNotNull(profile);
          assertEquals("testuser", profile.username());
          assertEquals(25, profile.age());
        });
  }

  @Test
  public void testValidEnhancedUserProfileWithNullOptionalFields() {
    // Test valid profile with null optional fields (email, bio)
    assertDoesNotThrow(
        () -> {
          EnhancedUserProfile profile =
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(30)
                  .minFollowers(5)
                  .maxConnections(500)
                  .email(null) // Optional field - should use nullOrMatches validation
                  .bio(null) // Optional field - should use nullOrHasLength validation
                  .displayName("Test User")
                  .build();

          assertNotNull(profile);
          assertEquals("testuser", profile.username());
          assertEquals(30, profile.age());
        });
  }

  @Test
  public void testMinimumFollowersValidation() {
    // Test individual @Min validation for minFollowers
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(25)
                  .minFollowers(0) // Below minimum of 1
                  .maxConnections(1000)
                  .displayName("Test User")
                  .build();
            });

    // Count validation errors
    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testMaximumConnectionsValidation() {
    // Test individual @Max validation for maxConnections
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(25)
                  .minFollowers(10)
                  .maxConnections(6000) // Above maximum of 5000
                  .displayName("Test User")
                  .build();
            });

    // Count validation errors
    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testNotBlankValidation() {
    // Test @NotBlank validation for displayName
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(25)
                  .minFollowers(10)
                  .maxConnections(1000)
                  .displayName("   ") // Blank string should fail @NotBlank
                  .build();
            });

    // Count validation errors
    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testMultipleValidationErrors() {
    // Test multiple validation failures at once
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new EnhancedUserProfileBuilder()
                  .username("ab") // Too short (min 3)
                  .age(-5) // Below minimum age (min 0)
                  .minFollowers(0) // Below minimum (min 1)
                  .maxConnections(6000) // Above maximum (max 5000)
                  .email("invalid-email") // Invalid email format
                  .bio("Short") // Too short (min 10)
                  .displayName("   ") // Blank
                  .build();
            });

    // Count validation errors - should have multiple errors
    int errorCount = exception.getErrors().size();
    assertNotNull(exception.getErrors());
    System.out.println("Total validation errors: " + errorCount);

    // Print all error messages for debugging
    exception.getErrors().forEach(System.out::println);
  }

  @Test
  public void testInvalidEmailPattern() {
    // Test pattern validation for email (when not null)
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(25)
                  .minFollowers(10)
                  .maxConnections(1000)
                  .email("invalid-email") // Invalid email format
                  .displayName("Test User")
                  .build();
            });

    // Count validation errors
    assertEquals(1, exception.getErrors().size());
  }

  @Test
  public void testBioLengthValidation() {
    // Test size validation for bio (when not null)
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> {
              new EnhancedUserProfileBuilder()
                  .username("testuser")
                  .age(25)
                  .minFollowers(10)
                  .maxConnections(1000)
                  .bio("Short") // Below minimum length of 10
                  .displayName("Test User")
                  .build();
            });

    // Count validation errors
    assertEquals(1, exception.getErrors().size());
  }
}
