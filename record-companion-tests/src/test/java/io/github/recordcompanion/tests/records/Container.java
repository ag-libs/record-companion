package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;

@Builder
public record Container<T>(String name, T value) {}
