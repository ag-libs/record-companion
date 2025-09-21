package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@Builder
public record Address(String street, String city, String zipCode) {}
