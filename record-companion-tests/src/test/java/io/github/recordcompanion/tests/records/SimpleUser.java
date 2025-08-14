package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import io.github.validcheck.CollectionValidator;
import io.github.validcheck.StringValidator;
import io.github.validcheck.ValueValidator;
import java.util.List;

@Builder
public record SimpleUser(String name, int age, List<String> aliases) {

  public SimpleUser {
    SimpleUserValidator.validator()
        .checkAge(age, ValueValidator::notNull)
        .checkName(name, StringValidator::notEmpty)
        .checkAliases(aliases, CollectionValidator::isNull)
        .validate();
  }
}
