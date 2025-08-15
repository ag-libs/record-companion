package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.AnnotatedRecord;
import io.github.recordcompanion.tests.records.AnnotatedRecordBuilder;
import io.github.recordcompanion.tests.records.ClassLevelAnnotation;
import io.github.recordcompanion.tests.records.FieldAnnotation;
import io.github.recordcompanion.tests.records.NonAnnotatedRecord;
import io.github.recordcompanion.tests.records.NonAnnotatedRecordBuilder;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class AnnotationCopyingTest {

  @Test
  void testAnnotationsCopiedToBuilderWhenEnabled() {
    // Test that annotations are copied to the builder class when copyAnnotations = true
    Class<?> builderClass = AnnotatedRecordBuilder.class;

    // Check if the class-level annotation is copied
    assertTrue(
        builderClass.isAnnotationPresent(ClassLevelAnnotation.class),
        "Builder class should have the ClassLevelAnnotation copied from the record");

    ClassLevelAnnotation classAnnotation = builderClass.getAnnotation(ClassLevelAnnotation.class);
    assertEquals(
        "record-annotation",
        classAnnotation.value(),
        "ClassLevelAnnotation value should be preserved");
  }

  @Test
  void testAnnotationsCopiedToSetterMethodsWhenEnabled() throws NoSuchMethodException {
    // Test that annotations are copied to setter methods when copyAnnotations = true
    Class<?> builderClass = AnnotatedRecordBuilder.class;

    // Check name setter method
    Method nameMethod = builderClass.getMethod("name", String.class);
    assertTrue(
        nameMethod.isAnnotationPresent(FieldAnnotation.class),
        "name setter should have FieldAnnotation copied from record component");

    FieldAnnotation nameAnnotation = nameMethod.getAnnotation(FieldAnnotation.class);
    assertEquals(
        "name-annotation", nameAnnotation.value(), "FieldAnnotation value should be preserved");

    // Check age setter method
    Method ageMethod = builderClass.getMethod("age", int.class);
    assertTrue(
        ageMethod.isAnnotationPresent(FieldAnnotation.class),
        "age setter should have FieldAnnotation copied from record component");

    FieldAnnotation ageAnnotation = ageMethod.getAnnotation(FieldAnnotation.class);
    assertEquals(
        "age-annotation", ageAnnotation.value(), "FieldAnnotation value should be preserved");
  }

  @Test
  void testAnnotationsNotCopiedWhenDisabled() {
    // Test that annotations are not copied when copyAnnotations = false (default)
    Class<?> builderClass = NonAnnotatedRecordBuilder.class;

    // Should not have any custom annotations (only @Generated which is added by the processor)
    long customAnnotationsCount =
        java.util.Arrays.stream(builderClass.getAnnotations())
            .filter(annotation -> !annotation.annotationType().getName().contains("Generated"))
            .count();

    assertEquals(
        0,
        customAnnotationsCount,
        "Builder class should not have custom annotations when copyAnnotations = false");
  }

  @Test
  void testSetterMethodsHaveNoAnnotationsWhenDisabled() throws NoSuchMethodException {
    // Test that setter methods don't have annotations when copyAnnotations = false
    Class<?> builderClass = NonAnnotatedRecordBuilder.class;

    Method nameMethod = builderClass.getMethod("name", String.class);
    assertEquals(
        0,
        nameMethod.getAnnotations().length,
        "name setter should have no annotations when copyAnnotations = false");

    Method ageMethod = builderClass.getMethod("age", int.class);
    assertEquals(
        0,
        ageMethod.getAnnotations().length,
        "age setter should have no annotations when copyAnnotations = false");
  }

  @Test
  void testBuilderFunctionalityStillWorksWithAnnotations() {
    // Test that the builder still works correctly even with copied annotations
    AnnotatedRecord record = AnnotatedRecordBuilder.builder().name("Test Name").age(25).build();

    assertEquals("Test Name", record.name());
    assertEquals(25, record.age());
  }

  @Test
  void testBuilderFunctionalityWorksWithoutAnnotations() {
    // Test that the builder works correctly when annotations are not copied
    NonAnnotatedRecord record =
        NonAnnotatedRecordBuilder.builder().name("Test Name").age(25).build();

    assertEquals("Test Name", record.name());
    assertEquals(25, record.age());
  }
}
