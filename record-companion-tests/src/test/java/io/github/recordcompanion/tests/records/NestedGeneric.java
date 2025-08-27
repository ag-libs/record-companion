package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;

@Builder
public record NestedGeneric<T>(String id, Container<T> container) {}
