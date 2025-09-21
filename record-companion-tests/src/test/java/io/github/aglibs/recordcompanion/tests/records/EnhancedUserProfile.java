package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;
import io.github.aglibs.recordcompanion.validcheck.ValidCheck;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Enhanced user profile record demonstrating new ValidCheck API features including: -
 * Individual @Min and @Max validation - @NotBlank validation - Null-safe validation for optional
 * fields
 */
@Builder
@ValidCheck
public record EnhancedUserProfile(
    @NotNull @NotBlank @Size(min = 3, max = 20) String username,
    @Min(0) @Max(120) Integer age,
    @Min(1) Integer minFollowers, // Only minimum validation
    @Max(5000) Integer maxConnections, // Only maximum validation
    @Pattern(regexp = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        String email, // Optional email (nullable)
    @Size(min = 10, max = 200) String bio, // Optional bio (nullable)
    @NotBlank String displayName // Required display name
    ) {

  public EnhancedUserProfile {
    // Validate using generated check methods with enhanced ValidCheck API
    EnhancedUserProfileCheck.validate(
        username, age, minFollowers, maxConnections, email, bio, displayName);
  }
}
