package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.AnnotatedGenericRecord;
import io.github.recordcompanion.tests.records.AnnotatedGenericRecordBuilder;
import io.github.recordcompanion.tests.records.ClassLevelAnnotation;
import io.github.recordcompanion.tests.records.FieldAnnotation;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class GenericAnnotationCopyingTest {

  @Test
  void testGenericRecordClassLevelAnnotationsCopied() {
    // Test that class-level annotations are copied to generic builder class
    Class<?> builderClass = AnnotatedGenericRecordBuilder.class;

    assertTrue(
        builderClass.isAnnotationPresent(ClassLevelAnnotation.class),
        "Generic builder class should have ClassLevelAnnotation copied from the record");

    ClassLevelAnnotation classAnnotation = builderClass.getAnnotation(ClassLevelAnnotation.class);
    assertEquals(
        "generic-record-annotation",
        classAnnotation.value(),
        "ClassLevelAnnotation value should be preserved in generic builder");
  }

  @Test
  void testGenericRecordFieldAnnotationsCopied() throws NoSuchMethodException {
    // Test that field annotations are copied to setter methods in generic builder
    Class<?> builderClass = AnnotatedGenericRecordBuilder.class;

    // Check name setter method (non-generic type)
    Method nameMethod = builderClass.getMethod("name", String.class);
    assertTrue(
        nameMethod.isAnnotationPresent(FieldAnnotation.class),
        "name setter should have FieldAnnotation copied from record component");

    FieldAnnotation nameAnnotation = nameMethod.getAnnotation(FieldAnnotation.class);
    assertEquals(
        "name-annotation", nameAnnotation.value(), "FieldAnnotation value should be preserved");

    // Check firstValue setter method (generic type T)
    Method firstValueMethod = builderClass.getMethod("firstValue", Object.class);
    assertTrue(
        firstValueMethod.isAnnotationPresent(FieldAnnotation.class),
        "firstValue setter should have FieldAnnotation copied from generic record component T");

    FieldAnnotation firstValueAnnotation = firstValueMethod.getAnnotation(FieldAnnotation.class);
    assertEquals(
        "first-value-annotation",
        firstValueAnnotation.value(),
        "FieldAnnotation value should be preserved on generic type T");

    // Check secondValue setter method (generic type U)
    Method secondValueMethod = builderClass.getMethod("secondValue", Object.class);
    assertTrue(
        secondValueMethod.isAnnotationPresent(FieldAnnotation.class),
        "secondValue setter should have FieldAnnotation copied from generic record component U");

    FieldAnnotation secondValueAnnotation = secondValueMethod.getAnnotation(FieldAnnotation.class);
    assertEquals(
        "second-value-annotation",
        secondValueAnnotation.value(),
        "FieldAnnotation value should be preserved on generic type U");
  }

  @Test
  void testGenericBuilderFunctionalityWithAnnotations() {
    // Test that the generic builder works correctly with copied annotations
    AnnotatedGenericRecord<String, Integer> record =
        AnnotatedGenericRecordBuilder.<String, Integer>builder()
            .name("Test Generic")
            .firstValue("Hello")
            .secondValue(42)
            .build();

    assertEquals("Test Generic", record.name());
    assertEquals("Hello", record.firstValue());
    assertEquals(42, record.secondValue());
  }

  @Test
  void testGenericBuilderWithMethod() {
    // Test the with method works correctly with generic types and annotations
    AnnotatedGenericRecord<Double, String> original =
        new AnnotatedGenericRecord<>("Original", 3.14, "world");

    AnnotatedGenericRecord<Double, String> updated =
        AnnotatedGenericRecordBuilder.with(
            original,
            updater -> updater.name("Updated").firstValue(2.71).secondValue("updated world"));

    assertEquals("Updated", updated.name());
    assertEquals(2.71, updated.firstValue());
    assertEquals("updated world", updated.secondValue());
  }

  @Test
  void testGenericBuilderFromExisting() {
    // Test builder from existing with generic types
    AnnotatedGenericRecord<Boolean, Long> original =
        new AnnotatedGenericRecord<>("Original", true, 123L);

    AnnotatedGenericRecord<Boolean, Long> copy =
        AnnotatedGenericRecordBuilder.builder(original).firstValue(false).secondValue(456L).build();

    assertEquals("Original", copy.name()); // name should be copied
    assertEquals(false, copy.firstValue()); // modified
    assertEquals(456L, copy.secondValue()); // modified
  }

  @Test
  void testComplexGenericTypes() {
    // Test with more complex generic types like java.util.List<String>
    AnnotatedGenericRecord<java.util.List<String>, java.util.Map<String, Integer>> record =
        AnnotatedGenericRecordBuilder
            .<java.util.List<String>, java.util.Map<String, Integer>>builder()
            .name("Complex Generics")
            .firstValue(java.util.Arrays.asList("item1", "item2"))
            .secondValue(java.util.Map.of("key1", 1, "key2", 2))
            .build();

    assertEquals("Complex Generics", record.name());
    assertEquals(java.util.Arrays.asList("item1", "item2"), record.firstValue());
    assertEquals(java.util.Map.of("key1", 1, "key2", 2), record.secondValue());
  }
}
