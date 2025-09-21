package io.github.aglibs.recordcompanion.tests.records;

import io.github.aglibs.recordcompanion.builder.Builder;
import io.github.aglibs.recordcompanion.validcheck.ValidCheck;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Extended validation profile demonstrating new ValidCheck API features: - @DecimalMin
 * and @DecimalMax validation - @Size validation on Collections - Enhanced collection support
 */
@Builder
@ValidCheck
public record ExtendedValidationProfile(
    @NotNull @Size(min = 2, max = 50) String name,
    @DecimalMin("0.0") @DecimalMax("999.99") BigDecimal price,
    @DecimalMin("10.5") Double rating, // Only minimum decimal validation
    @DecimalMax("100.0") Double percentage, // Only maximum decimal validation
    @Size(min = 1, max = 10) List<String> tags, // Collection size validation
    @Size(min = 2, max = 5) Set<String> categories, // Set size validation
    @Size(min = 0, max = 3) List<String> optionalItems // Optional collection (nullable)
    ) {

  public ExtendedValidationProfile {
    // Validate using generated check methods with new ValidCheck API features
    ExtendedValidationProfileCheck.validate(
        name, price, rating, percentage, tags, categories, optionalItems);
  }
}
