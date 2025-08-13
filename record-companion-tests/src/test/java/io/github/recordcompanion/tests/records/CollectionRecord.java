package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import java.util.List;
import java.util.Set;

@Builder
public record CollectionRecord(String name, List<String> tags, Set<Integer> scores) {}
