package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;

@Builder
public record Pair<T, U>(T first, U second) {}
