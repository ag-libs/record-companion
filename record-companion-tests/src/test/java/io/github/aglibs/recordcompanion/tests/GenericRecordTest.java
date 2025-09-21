package io.github.aglibs.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.aglibs.recordcompanion.tests.records.BoundedGeneric;
import io.github.aglibs.recordcompanion.tests.records.BoundedGenericBuilder;
import io.github.aglibs.recordcompanion.tests.records.Container;
import io.github.aglibs.recordcompanion.tests.records.ContainerBuilder;
import io.github.aglibs.recordcompanion.tests.records.NestedGeneric;
import io.github.aglibs.recordcompanion.tests.records.NestedGenericBuilder;
import io.github.aglibs.recordcompanion.tests.records.Pair;
import io.github.aglibs.recordcompanion.tests.records.PairBuilder;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenericRecordTest {

  @Test
  void testGenericContainerBuilding() {
    // Test building a Container<String>
    Container<String> stringContainer =
        ContainerBuilder.<String>builder().name("String Container").value("Hello World").build();

    assertEquals("String Container", stringContainer.name());
    assertEquals("Hello World", stringContainer.value());
  }

  @Test
  void testGenericContainerWith() {
    // Test using the with method on Container<Integer>
    Container<Integer> original = new Container<>("Numbers", 42);

    Container<Integer> updated =
        ContainerBuilder.with(original, u -> u.name("Updated Numbers").value(100));

    assertEquals("Updated Numbers", updated.name());
    assertEquals(100, updated.value());
  }

  @Test
  void testGenericPairBuilding() {
    // Test building a Pair<String, Integer>
    Pair<String, Integer> pair =
        PairBuilder.<String, Integer>builder().first("Answer").second(42).build();

    assertEquals("Answer", pair.first());
    assertEquals(42, pair.second());
  }

  @Test
  void testGenericPairWith() {
    // Test using the with method on Pair<Boolean, String>
    Pair<Boolean, String> original = new Pair<>(true, "Original");

    Pair<Boolean, String> updated =
        PairBuilder.with(original, u -> u.first(false).second("Updated"));

    assertEquals(false, updated.first());
    assertEquals("Updated", updated.second());
  }

  @Test
  void testNestedGenericBuilding() {
    // Test building NestedGeneric<Double> with nested Container<Double>
    NestedGeneric<Double> nested =
        NestedGenericBuilder.<Double>builder()
            .id("test-id")
            .container(
                ContainerBuilder.<Double>builder().name("Double Container").value(3.14).build())
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
        NestedGenericBuilder.with(
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
        NestedGenericBuilder.with(
            withNullContainer, u -> u.container(cu -> cu.name("New Container").value(123L)));

    assertEquals("test", updated.id());
    assertNotNull(updated.container());
    assertEquals("New Container", updated.container().name());
    assertEquals(123L, updated.container().value());
  }

  @Test
  void testBoundedGenericsBuilder() {
    List<String> items = Arrays.asList("item1", "item2");

    BoundedGeneric<Integer, List<String>> bounded =
        BoundedGenericBuilder.<Integer, List<String>>builder()
            .name("Test")
            .value(42)
            .items(items)
            .build();

    assertEquals("Test", bounded.name());
    assertEquals(42, bounded.value());
    assertEquals(items, bounded.items());
  }

  @Test
  void testBoundedGenericsWithMethod() {
    List<String> originalItems = Arrays.asList("old1", "old2");
    List<String> newItems = Arrays.asList("new1", "new2");

    BoundedGeneric<Double, List<String>> original =
        BoundedGenericBuilder.<Double, List<String>>builder()
            .name("Original")
            .value(3.14)
            .items(originalItems)
            .build();

    BoundedGeneric<Double, List<String>> updated =
        BoundedGenericBuilder.with(original, u -> u.name("Updated").value(2.71).items(newItems));

    assertEquals("Updated", updated.name());
    assertEquals(2.71, updated.value());
    assertEquals(newItems, updated.items());
  }

  @Test
  void testBoundedGenericBuilder() {
    // Test with wildcards and bounded types
    BoundedGeneric<Integer, List<String>> boundedGeneric =
        BoundedGenericBuilder.<Integer, List<String>>builder()
            .name("Test Bounded")
            .value(42)
            .items(Arrays.asList("item1", "item2"))
            .build();

    assertEquals("Test Bounded", boundedGeneric.name());
    assertEquals(42, boundedGeneric.value());
    assertEquals(Arrays.asList("item1", "item2"), boundedGeneric.items());

    // Test that the bounded generic works with Number subtypes
    BoundedGeneric<Long, List<String>> longRecord =
        BoundedGenericBuilder.<Long, List<String>>builder()
            .name("Long Test")
            .value(100L)
            .items(Arrays.asList("string1", "string2", "string3"))
            .build();

    assertEquals("Long Test", longRecord.name());
    assertEquals(100L, longRecord.value());
    assertEquals(Arrays.asList("string1", "string2", "string3"), longRecord.items());
  }
}
