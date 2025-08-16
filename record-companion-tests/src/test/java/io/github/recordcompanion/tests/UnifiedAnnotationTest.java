package io.github.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.recordcompanion.tests.records.AdvancedAnnotatedRecord;
import io.github.recordcompanion.tests.records.AdvancedAnnotatedRecordBuilder;
import io.github.recordcompanion.tests.records.AnnotatedRecord;
import io.github.recordcompanion.tests.records.AnnotatedRecordBuilder;
import io.github.recordcompanion.tests.records.ClassLevelAnnotation;
import io.github.recordcompanion.tests.records.FieldAnnotation;
import io.github.recordcompanion.tests.records.NonAnnotatedRecordBuilder;
import io.github.recordcompanion.tests.records.TypeVariableAnnotation;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnifiedAnnotationTest {

  @Nested
  class BasicAnnotationCopying {

    @Test
    void testClassLevelAnnotationsCopied() {
      Class<?> builderClass = AnnotatedRecordBuilder.class;

      assertTrue(
          builderClass.isAnnotationPresent(ClassLevelAnnotation.class),
          "Builder class should have ClassLevelAnnotation copied from the record");

      ClassLevelAnnotation classAnnotation = builderClass.getAnnotation(ClassLevelAnnotation.class);
      assertEquals("record-annotation", classAnnotation.value());
    }

    @Test
    void testFieldAnnotationsCopied() throws NoSuchMethodException {
      Class<?> builderClass = AnnotatedRecordBuilder.class;

      // Check name setter
      Method nameMethod = builderClass.getMethod("name", String.class);
      assertTrue(nameMethod.isAnnotationPresent(FieldAnnotation.class));
      assertEquals("name-annotation", nameMethod.getAnnotation(FieldAnnotation.class).value());

      // Check age setter
      Method ageMethod = builderClass.getMethod("age", int.class);
      assertTrue(ageMethod.isAnnotationPresent(FieldAnnotation.class));
      assertEquals("age-annotation", ageMethod.getAnnotation(FieldAnnotation.class).value());
    }

    @Test
    void testBuilderFunctionality() {
      AnnotatedRecord record = AnnotatedRecordBuilder.builder().name("Test Name").age(25).build();

      assertEquals("Test Name", record.name());
      assertEquals(25, record.age());
    }
  }

  @Nested
  class AnnotationCopyingDisabled {

    @Test
    void testNoAnnotationsCopiedWhenDisabled() {
      Class<?> builderClass = NonAnnotatedRecordBuilder.class;

      long customAnnotationsCount =
          Arrays.stream(builderClass.getAnnotations())
              .filter(annotation -> !annotation.annotationType().getName().contains("Generated"))
              .count();

      assertEquals(0, customAnnotationsCount);
    }

    @Test
    void testSetterMethodsHaveNoAnnotationsWhenDisabled() throws NoSuchMethodException {
      Class<?> builderClass = NonAnnotatedRecordBuilder.class;

      Method nameMethod = builderClass.getMethod("name", String.class);
      assertEquals(0, nameMethod.getAnnotations().length);

      Method ageMethod = builderClass.getMethod("age", int.class);
      assertEquals(0, ageMethod.getAnnotations().length);
    }
  }

  @Nested
  class AdvancedAnnotationCopying {

    @Test
    void testTypeVariableAnnotationsCopied() {
      Class<?> builderClass = AdvancedAnnotatedRecordBuilder.class;
      TypeVariable<?>[] typeVariables = builderClass.getTypeParameters();

      assertEquals(2, typeVariables.length);

      // Check first type variable (T)
      TypeVariable<?> tVar = typeVariables[0];
      assertEquals("T", tVar.getName());
      assertTrue(tVar.isAnnotationPresent(TypeVariableAnnotation.class));
      assertEquals("T-annotation", tVar.getAnnotation(TypeVariableAnnotation.class).value());

      // Check second type variable (U)
      TypeVariable<?> uVar = typeVariables[1];
      assertEquals("U", uVar.getName());
      assertTrue(uVar.isAnnotationPresent(TypeVariableAnnotation.class));
      assertEquals("U-annotation", uVar.getAnnotation(TypeVariableAnnotation.class).value());
    }

    @Test
    void testAdvancedClassLevelAnnotationsCopied() {
      Class<?> builderClass = AdvancedAnnotatedRecordBuilder.class;

      assertTrue(builderClass.isAnnotationPresent(ClassLevelAnnotation.class));
      assertEquals(
          "advanced-record", builderClass.getAnnotation(ClassLevelAnnotation.class).value());
    }

    @Test
    void testAdvancedFieldAnnotationsCopied() throws NoSuchMethodException {
      Class<?> builderClass = AdvancedAnnotatedRecordBuilder.class;

      String[] fields = {"name", "value", "stringList", "dataMap"};
      String[] expectedValues = {
        "name-annotation", "value-annotation", "list-annotation", "map-annotation"
      };
      Class<?>[] paramTypes = {String.class, Object.class, List.class, Map.class};

      for (int i = 0; i < fields.length; i++) {
        Method method = builderClass.getMethod(fields[i], paramTypes[i]);
        assertTrue(method.isAnnotationPresent(FieldAnnotation.class));
        assertEquals(expectedValues[i], method.getAnnotation(FieldAnnotation.class).value());
      }
    }

    @Test
    void testAdvancedBuilderFunctionality() {
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
    void testWithMethodFunctionality() {
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
    void testBuilderFromExisting() {
      List<String> testList = Arrays.asList("test1", "test2");
      Map<String, String> testMap = Map.of("testKey", "testValue");

      AdvancedAnnotatedRecord<Integer, String> original =
          new AdvancedAnnotatedRecord<>("Original", 42, testList, testMap);

      AdvancedAnnotatedRecord<Integer, String> copy =
          AdvancedAnnotatedRecordBuilder.builder(original).value(100).build();

      assertEquals("Original", copy.name());
      assertEquals(100, copy.value());
      assertEquals(testList, copy.stringList());
      assertEquals(testMap, copy.dataMap());
    }
  }
}
