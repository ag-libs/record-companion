package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.AdvancedAnnotatedRecord;
import io.github.recordcompanion.tests.records.AdvancedAnnotatedRecordBuilder;
import io.github.recordcompanion.tests.records.ClassLevelAnnotation;
import io.github.recordcompanion.tests.records.FieldAnnotation;
import io.github.recordcompanion.tests.records.TypeVariableAnnotation;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdvancedTypeAnnotationTest {

  @Test
  void testClassLevelAnnotationsCopied() {
    // Test that class-level annotations are copied to advanced builder
    Class<?> builderClass = AdvancedAnnotatedRecordBuilder.class;

    assertTrue(
        builderClass.isAnnotationPresent(ClassLevelAnnotation.class),
        "Advanced builder class should have ClassLevelAnnotation copied from the record");

    ClassLevelAnnotation classAnnotation = builderClass.getAnnotation(ClassLevelAnnotation.class);
    assertEquals(
        "advanced-record",
        classAnnotation.value(),
        "ClassLevelAnnotation value should be preserved");
  }

  @Test
  void testTypeVariableAnnotationsCopied() {
    // Test that type variable annotations are copied to the builder class
    Class<?> builderClass = AdvancedAnnotatedRecordBuilder.class;
    TypeVariable<?>[] typeVariables = builderClass.getTypeParameters();

    assertEquals(2, typeVariables.length, "Builder should have 2 type parameters");

    // Check first type variable (T)
    TypeVariable<?> tVar = typeVariables[0];
    assertEquals("T", tVar.getName());
    assertTrue(
        tVar.isAnnotationPresent(TypeVariableAnnotation.class),
        "Type variable T should have TypeVariableAnnotation");
    TypeVariableAnnotation tAnnotation = tVar.getAnnotation(TypeVariableAnnotation.class);
    assertEquals("T-annotation", tAnnotation.value());

    // Check second type variable (U)
    TypeVariable<?> uVar = typeVariables[1];
    assertEquals("U", uVar.getName());
    assertTrue(
        uVar.isAnnotationPresent(TypeVariableAnnotation.class),
        "Type variable U should have TypeVariableAnnotation");
    TypeVariableAnnotation uAnnotation = uVar.getAnnotation(TypeVariableAnnotation.class);
    assertEquals("U-annotation", uAnnotation.value());
  }

  @Test
  void testRecordComponentAnnotationsCopied() throws NoSuchMethodException {
    // Test that record component annotations are copied to setter methods
    Class<?> builderClass = AdvancedAnnotatedRecordBuilder.class;

    // Check name setter
    Method nameMethod = builderClass.getMethod("name", String.class);
    assertTrue(
        nameMethod.isAnnotationPresent(FieldAnnotation.class),
        "name setter should have FieldAnnotation");
    assertEquals("name-annotation", nameMethod.getAnnotation(FieldAnnotation.class).value());

    // Check value setter
    Method valueMethod = builderClass.getMethod("value", Object.class);
    assertTrue(
        valueMethod.isAnnotationPresent(FieldAnnotation.class),
        "value setter should have FieldAnnotation");
    assertEquals("value-annotation", valueMethod.getAnnotation(FieldAnnotation.class).value());

    // Check stringList setter
    Method stringListMethod = builderClass.getMethod("stringList", List.class);
    assertTrue(
        stringListMethod.isAnnotationPresent(FieldAnnotation.class),
        "stringList setter should have FieldAnnotation");
    assertEquals("list-annotation", stringListMethod.getAnnotation(FieldAnnotation.class).value());

    // Check dataMap setter
    Method dataMapMethod = builderClass.getMethod("dataMap", Map.class);
    assertTrue(
        dataMapMethod.isAnnotationPresent(FieldAnnotation.class),
        "dataMap setter should have FieldAnnotation");
    assertEquals("map-annotation", dataMapMethod.getAnnotation(FieldAnnotation.class).value());
  }

  @Test
  void testBuilderFunctionalityWithAdvancedAnnotations() {
    // Test that the advanced annotated builder works correctly
    List<String> testList = Arrays.asList("item1", "item2");
    Map<String, Integer> testMap = Map.of("key1", 1, "key2", 2);

    AdvancedAnnotatedRecord<String, Integer> record =
        AdvancedAnnotatedRecordBuilder.<String, Integer>builder()
            .name("Advanced Test")
            .value("test-value")
            .stringList(testList)
            .dataMap(testMap)
            .build();

    assertEquals("Advanced Test", record.name());
    assertEquals("test-value", record.value());
    assertEquals(testList, record.stringList());
    assertEquals(testMap, record.dataMap());
  }

  @Test
  void testWithMethodWithAdvancedAnnotations() {
    // Test the with method works with advanced type annotations
    List<String> originalList = Arrays.asList("original1", "original2");
    Map<String, Boolean> originalMap = Map.of("origKey", true);

    AdvancedAnnotatedRecord<Double, Boolean> original =
        new AdvancedAnnotatedRecord<>("Original", 3.14, originalList, originalMap);

    List<String> newList = Arrays.asList("new1", "new2", "new3");
    Map<String, Boolean> newMap = Map.of("newKey", false);

    AdvancedAnnotatedRecord<Double, Boolean> updated =
        AdvancedAnnotatedRecordBuilder.with(
            original,
            updater -> updater.name("Updated").value(2.71).stringList(newList).dataMap(newMap));

    assertEquals("Updated", updated.name());
    assertEquals(2.71, updated.value());
    assertEquals(newList, updated.stringList());
    assertEquals(newMap, updated.dataMap());
  }

  @Test
  void testBuilderFromExistingWithAdvancedAnnotations() {
    // Test builder from existing with advanced type annotations
    List<String> testList = Arrays.asList("test1", "test2");
    Map<String, String> testMap = Map.of("testKey", "testValue");

    AdvancedAnnotatedRecord<Integer, String> original =
        new AdvancedAnnotatedRecord<>("Original", 42, testList, testMap);

    AdvancedAnnotatedRecord<Integer, String> copy =
        AdvancedAnnotatedRecordBuilder.builder(original).value(100).build();

    assertEquals("Original", copy.name()); // name should be copied
    assertEquals(100, copy.value()); // value should be modified
    assertEquals(testList, copy.stringList()); // list should be copied
    assertEquals(testMap, copy.dataMap()); // map should be copied
  }
}
