package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;
import java.util.List;
import java.util.Set;

@Builder
public record CollectionRecord(String name, List<String> tags, Set<Integer> scores) {}
