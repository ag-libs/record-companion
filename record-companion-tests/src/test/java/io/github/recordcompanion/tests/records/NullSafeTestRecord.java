package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import io.github.recordcompanion.annotations.ValidCheck;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Test record with mixed required and optional fields to test nullOr* variants. */
@Builder
@ValidCheck
public record NullSafeTestRecord(
    // Required fields (should use regular validation methods)
    @NotNull @Size(min = 1, max = 10) String requiredText,
    @NotNull @Min(1) @Max(100) Integer requiredNumber,
    @NotNull @DecimalMin("0.01") @DecimalMax("999.99") BigDecimal requiredPrice,

    // Optional fields (should use nullOr* validation methods)
    @Size(min = 5, max = 50) String optionalText,
    @Pattern(regexp = "[A-Z]+") String optionalCode,
    @NotBlank String optionalTitle,
    @Min(10) Integer optionalMinValue,
    @Max(1000) Integer optionalMaxValue,
    @DecimalMin("5.0") Double optionalMinDecimal,
    @DecimalMax("100.0") Double optionalMaxDecimal,
    @Size(min = 1, max = 5) List<String> optionalList,
    @Size(min = 2, max = 10) Set<String> optionalSet) {

  public NullSafeTestRecord {
    NullSafeTestRecordCheck.validate(
        requiredText,
        requiredNumber,
        requiredPrice,
        optionalText,
        optionalCode,
        optionalTitle,
        optionalMinValue,
        optionalMaxValue,
        optionalMinDecimal,
        optionalMaxDecimal,
        optionalList,
        optionalSet);
  }
}
