package io.github.recordcompanion.processor;

import io.github.recordcompanion.annotations.Builder;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Annotation processor for RecordCompanion library.
 *
 * <p>Processes @Builder annotations on record classes and generates separate Builder classes and
 * Updater interfaces with builder pattern implementations.
 */
@SupportedAnnotationTypes("io.github.recordcompanion.annotations.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class RecordBuilderProcessor extends AbstractProcessor {

  private BuilderGenerator builderGenerator;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (builderGenerator == null) {
      builderGenerator = new BuilderGenerator(processingEnv);
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
      if (element.getKind() != ElementKind.RECORD) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "@Builder annotation can only be applied to record classes",
                element);
        continue;
      }

      TypeElement recordElement = (TypeElement) element;

      try {
        builderGenerator.generateBuilderAndUpdaterTypes(recordElement);
      } catch (IOException e) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to generate builder and updater types for "
                    + recordElement.getSimpleName()
                    + ": "
                    + e.getMessage(),
                recordElement);
      }
    }

    return true;
  }
}
