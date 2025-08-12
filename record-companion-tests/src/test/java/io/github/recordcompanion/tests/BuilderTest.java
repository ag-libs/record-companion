package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.User;
import io.github.recordcompanion.tests.records.UserCompanion;
import org.junit.jupiter.api.Test;

class BuilderTest {

    @Test
    void testBuilderGeneration() {
        // Test that we can create a User using the generated builder
        User user = UserCompanion.builder()
            .name("John Doe")
            .age(30)
            .email("john@example.com")
            .build();

        assertEquals("John Doe", user.name());
        assertEquals(30, user.age());
        assertEquals("john@example.com", user.email());
    }

    @Test
    void testBuilderWithExisting() {
        User original = new User("Jane", 25, "jane@example.com");
        
        // Test builder with existing record - should copy values
        User copy = UserCompanion.builder(original)
            .age(26)  // modify age
            .build();
        
        assertEquals("Jane", copy.name());  // name should be copied
        assertEquals(26, copy.age());       // age should be modified
        assertEquals("jane@example.com", copy.email());  // email should be copied
    }

    @Test
    void testWithMethod() {
        User original = new User("John", 30, "john@example.com");
        
        // Test the with method for fluent modification
        User updated = UserCompanion.with(original, builder -> {
            builder.age(31);
            builder.email("john.doe@example.com");
        });
        
        assertEquals("John", updated.name());  // name should remain the same
        assertEquals(31, updated.age());       // age should be updated
        assertEquals("john.doe@example.com", updated.email());  // email should be updated
    }

    @Test
    void testWithMethodSingleChange() {
        User original = new User("Alice", 28, "alice@example.com");
        
        // Test the with method with single property change
        User updated = UserCompanion.with(original, builder -> builder.age(29));
        
        assertEquals("Alice", updated.name());
        assertEquals(29, updated.age());
        assertEquals("alice@example.com", updated.email());
    }
}
