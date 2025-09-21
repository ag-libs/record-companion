package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;
import io.github.aglibs.recordcompanion.validcheck.ValidCheck;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Builder
@ValidCheck
public record UserProfile(
    @NotNull @Size(min = 3, max = 20) String username,
    @Min(0) @Max(100) int score,
    @NotEmpty Map<String, String> metadata,
    @Pattern(regexp = ".*") String address) {
  public UserProfile {
    UserProfileCheck.require(username, score, metadata, address);
  }
}
