package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.User;
import io.github.recordcompanion.tests.records.UserCompanion;
import org.junit.jupiter.api.Test;

class BuilderTest {

  @Test
  void testBuilderGeneration() {
    // Test that we can create a User using the generated builder
    User user = UserCompanion.builder().name("John Doe").age(30).email("john@example.com").build();

    assertEquals("John Doe", user.name());
    assertEquals(30, user.age());
    assertEquals("john@example.com", user.email());
  }

  @Test
  void testBuilderWithExisting() {
    User original = new User("Jane", 25, "jane@example.com");

    // Test builder with existing record - should copy values and allow chaining
    User copy =
        UserCompanion.builder(original)
            .age(26) // modify age with chaining
            .email("jane.doe@example.com") // chain another modification
            .build();

    assertEquals("Jane", copy.name()); // name should be copied
    assertEquals(26, copy.age()); // age should be modified
    assertEquals("jane.doe@example.com", copy.email()); // email should be modified
  }

  @Test
  void testWithMethod() {
    User original = new User("John", 30, "john@example.com");

    // Test the with method using Updater interface (build method is hidden)
    User updated =
        UserCompanion.with(
            original,
            updater -> {
              updater.age(original.age() + 1);
              updater.email("john.doe@example.com");
            });

    assertEquals("John", updated.name()); // name should remain the same
    assertEquals(31, updated.age()); // age should be updated
    assertEquals("john.doe@example.com", updated.email()); // email should be updated
  }

  @Test
  void testWithMethodSingleChange() {
    User original = new User("Alice", 28, "alice@example.com");

    // Test the with method with single property change using Updater
    User updated = UserCompanion.with(original, updater -> updater.age(29));

    assertEquals("Alice", updated.name());
    assertEquals(29, updated.age());
    assertEquals("alice@example.com", updated.email());
  }

  @Test
  void testBuilderChaining() {
    // Test that Builder methods return Builder for proper chaining
    User user =
        UserCompanion.builder().name("Chained").age(25).email("chained@example.com").build();

    assertEquals("Chained", user.name());
    assertEquals(25, user.age());
    assertEquals("chained@example.com", user.email());
  }
}
