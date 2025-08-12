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
        
        // Test builder with existing record (when implemented)
        UserCompanion.Builder builder = UserCompanion.builder(original);
        assertNotNull(builder);
    }
}
