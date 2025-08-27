package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;

@Builder
public record Person(String name, int age, Address address) {}
