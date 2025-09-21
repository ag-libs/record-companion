package io.github.aglibs.recordcompanion.tests.records;

import static io.github.aglibs.validcheck.ValidCheck.check;

import io.github.aglibs.recordcompanion.builder.Builder;
import io.github.aglibs.recordcompanion.validcheck.ValidCheck;
import java.util.List;

@Builder
@ValidCheck
public record SimpleUser(String name, int age, List<String> aliases) {

  public SimpleUser {
    check()
        .notNull(age, "age")
        .notEmpty(name, "name")
        .assertTrue(aliases == null, "aliases")
        .validate();
  }
}
