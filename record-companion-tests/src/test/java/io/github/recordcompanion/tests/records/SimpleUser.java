package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import io.github.recordcompanion.annotations.ValidCheck;
import io.github.validcheck.CollectionValidator;
import io.github.validcheck.StringValidator;
import io.github.validcheck.ValueValidator;
import java.util.List;

@Builder
@ValidCheck
public record SimpleUser(String name, int age, List<String> aliases) {

  public SimpleUser {
    SimpleUserCheck.check()
        .checkAge(age, ValueValidator::notNull)
        .checkName(name, StringValidator::notEmpty)
        .checkAliases(aliases, CollectionValidator::isNull)
        .validate();
  }
}
