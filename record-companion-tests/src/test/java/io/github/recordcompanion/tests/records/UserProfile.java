package io.github.recordcompanion.tests.records;

import static io.github.recordcompanion.tests.records.UserProfileValidator.checkUsername;
import static io.github.recordcompanion.tests.records.UserProfileValidator.validator;

import io.github.recordcompanion.annotations.Builder;
import java.util.Map;

@Builder
public record UserProfile(String username, int score, Map<String, String> metadata) {
  public UserProfile {
    validator()
        .username(username, stringValidator -> stringValidator.notNull().lengthBetween(3, 20))
        .score(score, numericValidator -> numericValidator.isPositive().max(100))
        .metadata(metadata, mapValidator -> mapValidator.notNull().notEmpty())
        .validate();

    checkUsername(username).notNull();
  }
}
