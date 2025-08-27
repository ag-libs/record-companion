package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;
import java.util.List;
import java.util.Set;

@Builder
public record CollectionRecord(String name, List<String> tags, Set<Integer> scores) {}
