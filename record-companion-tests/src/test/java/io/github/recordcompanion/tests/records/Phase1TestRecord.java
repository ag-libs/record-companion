package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import io.github.recordcompanion.annotations.ValidCheck;
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

@Builder
@ValidCheck
public record Phase1TestRecord(
    @NotNull String notNullField,
    @NotEmpty String notEmptyField,
    @NotBlank String notBlankField,
    @Size(min = 5, max = 10) String sizeField,
    @Pattern(regexp = "[A-Z]{3}[0-9]{3}") String patternField,
    @Min(10) int minOnlyField,
    @Max(100) int maxOnlyField,
    @Min(0) @Max(50) int minMaxCombinedField,
    @Positive int positiveField,
    @Negative int negativeField,
    @PositiveOrZero int positiveOrZeroField,
    @NegativeOrZero int negativeOrZeroField) {}
