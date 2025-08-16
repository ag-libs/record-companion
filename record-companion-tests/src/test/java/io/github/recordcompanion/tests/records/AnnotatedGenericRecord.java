package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;

@ClassLevelAnnotation("generic-record-annotation")
@Builder(copyAnnotations = true)
public record AnnotatedGenericRecord<T, U>(
    @FieldAnnotation("name-annotation") String name,
    @FieldAnnotation("first-value-annotation") T firstValue,
    @FieldAnnotation("second-value-annotation") U secondValue) {}
