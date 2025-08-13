package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import jakarta.validation.constraints.NotNull;

@Builder
public record MultiValidatedUser(
    @NotNull String name,
    Integer age,
    @NotNull String email,
    String phone
) {
  public MultiValidatedUser {
    MultiValidatedUserCompanion.validate(name, email);
  }
}