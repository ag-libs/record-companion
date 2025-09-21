package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@ClassLevelAnnotation("record-annotation")
@Builder(copyAnnotations = true)
public record AnnotatedRecord(
    @FieldAnnotation("name-annotation") String name, @FieldAnnotation("age-annotation") int age) {}
