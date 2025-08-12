package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;

@Builder
public record User(String name, int age, String email) {
}