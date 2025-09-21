package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@Builder
public record Person(String name, int age, Address address) {}
