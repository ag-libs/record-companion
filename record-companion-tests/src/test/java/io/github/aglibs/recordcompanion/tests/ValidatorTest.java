package io.github.aglibs.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.aglibs.recordcompanion.tests.records.SimpleUser;
import io.github.aglibs.recordcompanion.tests.records.SimpleUserBuilder;
import org.junit.jupiter.api.Test;

/** Test cases for ValidCheck integration. */
public class ValidatorTest {

  @Test
  public void testBuilderGenerationWithoutValidCheck() {
    // Test that the builder works correctly without ValidCheck
    SimpleUser user = new SimpleUserBuilder().name("John").age(25).build();

    assertNotNull(user);
    assertEquals("John", user.name());
    assertEquals(25, user.age());
  }
}
