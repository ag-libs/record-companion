package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;

@Builder(copyAnnotations = false)
public record NonAnnotatedRecord(String name, int age) {}
