package io.github.recordcompanion.tests.records;

import static io.github.validcheck.ValidCheck.check;

import io.github.recordcompanion.annotations.Builder;
import io.github.recordcompanion.annotations.ValidCheck;
import java.util.List;

@Builder
@ValidCheck
public record SimpleUser(String name, int age, List<String> aliases) {

  public SimpleUser {
    check()
        .notNull(age, "age")
        .notNullOrEmpty(name, "name")
        .assertTrue(aliases == null, "aliases")
        .validate();
  }
}
