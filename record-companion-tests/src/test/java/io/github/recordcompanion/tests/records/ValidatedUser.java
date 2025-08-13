package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import jakarta.validation.constraints.NotNull;

@Builder
public record ValidatedUser(@NotNull String name, Integer age, String email) {
  public ValidatedUser {
    ValidatedUserCompanion.validate(name);
  }
}
