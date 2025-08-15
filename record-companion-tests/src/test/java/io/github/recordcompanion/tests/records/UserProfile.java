package io.github.recordcompanion.tests.records;

import static io.github.recordcompanion.tests.records.UserProfileCheck.check;
import static io.github.recordcompanion.tests.records.UserProfileCheck.checkUsername;

import io.github.recordcompanion.annotations.Builder;
import io.github.recordcompanion.annotations.ValidCheck;
import java.util.Map;

@Builder
@ValidCheck
public record UserProfile(String username, int score, Map<String, String> metadata) {
  public UserProfile {
    check()
        .checkUsername(username, stringValidator -> stringValidator.notNull().lengthBetween(3, 20))
        .checkScore(score, numericValidator -> numericValidator.isPositive().max(100))
        .checkMetadata(metadata, mapValidator -> mapValidator.notNull().notEmpty())
        .validate();

    checkUsername(username).notNull();
  }
}
