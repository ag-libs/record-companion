/** RecordCompanion ValidCheck module for generating ValidCheck validation for Java records. */
module recordcompanion.validcheck {
  requires transitive java.compiler;
  requires java.validation;
  requires com.squareup.javapoet;

  // Export only the public annotation API
  exports io.github.recordcompanion.validcheck;

  // Internal implementation is not exported - only accessible via service provider

  provides javax.annotation.processing.Processor with
      io.github.recordcompanion.validcheck.internal.ValidCheckProcessor;
}
