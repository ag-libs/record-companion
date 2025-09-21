package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@Builder
public record NestedGeneric<T>(String id, Container<T> container) {}
