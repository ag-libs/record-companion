package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import java.util.Map;

@Builder
public record UserProfile(String username, int score, Map<String, String> metadata) {}
