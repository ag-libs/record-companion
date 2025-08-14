package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.recordcompanion.tests.records.UserProfile;
import io.github.recordcompanion.tests.records.UserProfileBuilder;
import io.github.validcheck.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests evaluating the user experience of record validation with automatic constructor validation.
 */
public class UserExperienceTest {

  @Test
  public void testAutomaticValidationOnConstruction() {
    // Test that invalid data throws validation exception during construction
    assertThrows(
        ValidationException.class,
        () -> {
          new UserProfile("a", -5, Map.of()); // username too short, score negative, metadata empty
        });

    assertThrows(
        ValidationException.class,
        () -> {
          new UserProfile("", 50, Map.of("key", "value")); // username empty
        });

    assertThrows(
        ValidationException.class,
        () -> {
          new UserProfile("john_doe", 150, Map.of("key", "value")); // score too high
        });

    assertThrows(
        ValidationException.class,
        () -> {
          new UserProfile("john_doe", 50, Map.of()); // metadata empty
        });
  }

  @Test
  public void testValidConstructionSucceeds() {
    // Test that valid data allows successful construction
    assertDoesNotThrow(
        () -> {
          new UserProfile("john_doe", 95, Map.of("role", "admin", "dept", "engineering"));
        });

    assertDoesNotThrow(
        () -> {
          new UserProfile(
              "alice",
              1,
              Map.of("status", "active")); // edge case: score 1 is valid (minimum positive)
        });
  }

  @Test
  public void testBuilderWithAutomaticValidation() {
    // Test that builder also triggers validation through constructor
    assertThrows(
        ValidationException.class,
        () -> {
          UserProfileBuilder.builder()
              .username("x") // too short
              .score(50)
              .metadata(Map.of("key", "value"))
              .build();
        });

    assertDoesNotThrow(
        () -> {
          UserProfileBuilder.builder()
              .username("john_doe")
              .score(95)
              .metadata(Map.of("role", "admin"))
              .build();
        });
  }
}
