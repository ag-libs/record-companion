package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.builder.Builder;
import io.github.recordcompanion.validcheck.ValidCheck;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

/**
 * Comprehensive validation record testing all supported Bean Validation annotations and
 * combinations for maximum test coverage.
 */
@Builder
@ValidCheck
public record ComprehensiveValidationRecord(
    // Required string validations
    @NotNull @NotBlank @Size(min = 1, max = 50) String requiredName,

    // Optional string validations (nullable)
    @Size(min = 5, max = 100) String optionalDescription,
    @Pattern(regexp = "[A-Z]{2,4}") String optionalCode,
    @NotBlank String optionalTitle,

    // Integer number validations
    @NotNull @Min(1) @Max(100) Integer requiredScore,
    @Min(10) Integer optionalMinOnly,
    @Max(1000) Integer optionalMaxOnly,
    @Positive Integer positiveCount,
    @Negative Integer negativeOffset,
    @PositiveOrZero Integer nonNegativeAmount,
    @NegativeOrZero Integer nonPositiveBalance,

    // Decimal number validations
    @NotNull @DecimalMin("0.01") @DecimalMax("999.99") BigDecimal requiredPrice,
    @DecimalMin("5.0") Double optionalMinRating,
    @DecimalMax("100.0") Double optionalMaxPercentage,

    // Collection validations
    @NotNull @NotEmpty @Size(min = 1, max = 5) List<String> requiredTags,
    @Size(min = 2, max = 10) Set<String> optionalCategories,
    @NotEmpty Map<String, String> requiredMetadata,

    // Complex combinations
    @NotNull @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "[a-zA-Z]+")
        String validatedUsername,
    @Min(18) @Max(120) @PositiveOrZero Integer ageRange,
    @DecimalMin("0.0") @DecimalMax("5.0") @PositiveOrZero Double starRating) {

  public ComprehensiveValidationRecord {
    // Use generated validation
    ComprehensiveValidationRecordCheck.validate(
        requiredName,
        optionalDescription,
        optionalCode,
        optionalTitle,
        requiredScore,
        optionalMinOnly,
        optionalMaxOnly,
        positiveCount,
        negativeOffset,
        nonNegativeAmount,
        nonPositiveBalance,
        requiredPrice,
        optionalMinRating,
        optionalMaxPercentage,
        requiredTags,
        optionalCategories,
        requiredMetadata,
        validatedUsername,
        ageRange,
        starRating);
  }
}
