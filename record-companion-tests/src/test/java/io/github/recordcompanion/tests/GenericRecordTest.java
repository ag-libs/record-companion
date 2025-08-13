package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.Container;
import io.github.recordcompanion.tests.records.ContainerCompanion;
import io.github.recordcompanion.tests.records.NestedGeneric;
import io.github.recordcompanion.tests.records.NestedGenericCompanion;
import io.github.recordcompanion.tests.records.Pair;
import io.github.recordcompanion.tests.records.PairCompanion;
import org.junit.jupiter.api.Test;

class GenericRecordTest {

  @Test
  void testGenericContainerBuilding() {
    // Test building a Container<String>
    Container<String> stringContainer =
        ContainerCompanion.<String>builder().name("String Container").value("Hello World").build();

    assertEquals("String Container", stringContainer.name());
    assertEquals("Hello World", stringContainer.value());
  }

  @Test
  void testGenericContainerWith() {
    // Test using the with method on Container<Integer>
    Container<Integer> original = new Container<>("Numbers", 42);

    Container<Integer> updated =
        ContainerCompanion.with(original, u -> u.name("Updated Numbers").value(100));

    assertEquals("Updated Numbers", updated.name());
    assertEquals(100, updated.value());
  }

  @Test
  void testGenericPairBuilding() {
    // Test building a Pair<String, Integer>
    Pair<String, Integer> pair =
        PairCompanion.<String, Integer>builder().first("Answer").second(42).build();

    assertEquals("Answer", pair.first());
    assertEquals(42, pair.second());
  }

  @Test
  void testGenericPairWith() {
    // Test using the with method on Pair<Boolean, String>
    Pair<Boolean, String> original = new Pair<>(true, "Original");

    Pair<Boolean, String> updated =
        PairCompanion.with(original, u -> u.first(false).second("Updated"));

    assertEquals(false, updated.first());
    assertEquals("Updated", updated.second());
  }

  @Test
  void testNestedGenericBuilding() {
    // Test building NestedGeneric<Double> with nested Container<Double>
    NestedGeneric<Double> nested =
        NestedGenericCompanion.<Double>builder()
            .id("test-id")
            .container(
                ContainerCompanion.<Double>builder().name("Double Container").value(3.14).build())
            .build();

    assertEquals("test-id", nested.id());
    assertEquals("Double Container", nested.container().name());
    assertEquals(3.14, nested.container().value());
  }

  @Test
  void testNestedGenericWithFluentUpdate() {
    // Test nested generic fluent updates
    NestedGeneric<String> original =
        new NestedGeneric<>("original-id", new Container<>("Original Name", "original value"));

    NestedGeneric<String> updated =
        NestedGenericCompanion.with(
            original,
            u ->
                u.id("updated-id").container(cu -> cu.name("Updated Name").value("updated value")));

    assertEquals("updated-id", updated.id());
    assertEquals("Updated Name", updated.container().name());
    assertEquals("updated value", updated.container().value());
  }

  @Test
  void testNestedGenericWithNullContainer() {
    // Test fluent update when nested container is initially null
    NestedGeneric<Long> withNullContainer = new NestedGeneric<>("test", null);

    NestedGeneric<Long> updated =
        NestedGenericCompanion.with(
            withNullContainer, u -> u.container(cu -> cu.name("New Container").value(123L)));

    assertEquals("test", updated.id());
    assertNotNull(updated.container());
    assertEquals("New Container", updated.container().name());
    assertEquals(123L, updated.container().value());
  }
}
