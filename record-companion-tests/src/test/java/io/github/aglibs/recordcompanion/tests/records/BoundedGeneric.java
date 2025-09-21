package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;
import java.util.List;

@Builder
public record BoundedGeneric<T extends Number, U extends List<? extends String>>(
    String name, T value, U items) {}
