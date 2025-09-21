package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@Builder
public record Pair<T, U>(T first, U second) {}
