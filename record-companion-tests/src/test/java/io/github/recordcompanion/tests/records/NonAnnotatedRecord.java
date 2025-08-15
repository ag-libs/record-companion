package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;

@Builder(copyAnnotations = false)
public record NonAnnotatedRecord(String name, int age) {}
