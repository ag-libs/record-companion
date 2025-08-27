package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;
import java.util.List;
import java.util.Map;

@ClassLevelAnnotation("advanced-record")
@Builder(copyAnnotations = true)
public record AdvancedAnnotatedRecord<
    @TypeVariableAnnotation("T-annotation") T extends @NonNullAnnotation Object,
    @TypeVariableAnnotation("U-annotation") U>(
    @FieldAnnotation("name-annotation") String name,

    // Simple type use annotation
    @FieldAnnotation("value-annotation") @TypeUseAnnotation("type-use-T") T value,

    // Parameterized type with type use annotation
    @FieldAnnotation("list-annotation")
        List<@TypeUseAnnotation("type-use-string") String> stringList,

    // Nested parameterized type with multiple type use annotations
    @FieldAnnotation("map-annotation")
        Map<
                @TypeUseAnnotation("key-annotation") String,
                @TypeUseAnnotation("value-annotation") @NonNullAnnotation U>
            dataMap) {}
