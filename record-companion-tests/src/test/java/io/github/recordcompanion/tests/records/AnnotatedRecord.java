package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;

@ClassLevelAnnotation("record-annotation")
@Builder(copyAnnotations = true)
public record AnnotatedRecord(
    @FieldAnnotation("name-annotation") String name, @FieldAnnotation("age-annotation") int age) {}
