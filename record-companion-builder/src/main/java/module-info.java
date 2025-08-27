/**
 * RecordCompanion Builder module for generating builder pattern implementations for Java records.
 */
module recordcompanion.builder {
  requires transitive java.compiler;
  requires com.squareup.javapoet;

  // Export only the public annotation API
  exports io.github.recordcompanion.builder;

  // Internal implementation is not exported - only accessible via service provider

  provides javax.annotation.processing.Processor with
      io.github.recordcompanion.builder.internal.BuilderProcessor;
}
