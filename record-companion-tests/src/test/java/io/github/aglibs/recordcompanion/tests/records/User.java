package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@Builder
public record User(String name, int age, String email) {}
