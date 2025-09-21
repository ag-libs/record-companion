package io.github.aglibs.recordcompanion.validcheck.internal;

import io.github.aglibs.recordcompanion.validcheck.ValidCheck;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Annotation processor for ValidCheck validation generation.
 *
 * <p>Processes @ValidCheck annotations on record classes and generates Check classes with
 * ValidCheck integration for Bean Validation annotations.
 */
@SupportedAnnotationTypes("io.github.aglibs.recordcompanion.validcheck.ValidCheck")
public class ValidCheckProcessor extends AbstractProcessor {

  private CheckGenerator checkGenerator;

  public ValidCheckProcessor() {
    // Default constructor
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (checkGenerator == null) {
      checkGenerator = new CheckGenerator(processingEnv);
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(ValidCheck.class)) {
      if (element.getKind() != ElementKind.RECORD) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "@ValidCheck annotation can only be applied to record classes",
                element);
        continue;
      }

      TypeElement recordElement = (TypeElement) element;

      try {
        checkGenerator.generateCheck(recordElement);
      } catch (IOException e) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to generate check class for "
                    + recordElement.getSimpleName()
                    + ": "
                    + e.getMessage(),
                recordElement);
      }
    }

    return true;
  }
}
