package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;

@Builder
public record Address(String street, String city, String zipCode) {}
