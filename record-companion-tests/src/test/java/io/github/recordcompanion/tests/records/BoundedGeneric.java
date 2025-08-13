package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import java.util.List;

@Builder
public record BoundedGeneric<T extends Number, U extends List<? extends String>>(
    String name, T value, U items) {}
