import io.github.recordcompanion.processor.RecordCompanionProcessor;

/**
 * Module for RecordCompanion annotation processor.
 *
 * <p>This module provides annotation processing for @Builder and @ValidCheck annotations to
 * generate builder classes and ValidCheck integration for Java records.
 */
module io.github.recordcompanion.processor {
  requires java.compiler;
  requires com.squareup.javapoet;
  requires static java.validation;

  // Export annotations for use by other modules
  exports io.github.recordcompanion.annotations;

  // Provide annotation processor service
  provides javax.annotation.processing.Processor with
      RecordCompanionProcessor;
}
