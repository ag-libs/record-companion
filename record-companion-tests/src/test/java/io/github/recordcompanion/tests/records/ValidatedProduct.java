package io.github.recordcompanion.tests.records;

import io.github.recordcompanion.annotations.Builder;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Builder
public record ValidatedProduct(
    @NotNull String name,
    @Min(1) Integer price,
    @NotNull @Min(0) Long quantity,
    String description
) {
  public ValidatedProduct {
    ValidatedProductCompanion.validate(name, price, quantity);
  }
}