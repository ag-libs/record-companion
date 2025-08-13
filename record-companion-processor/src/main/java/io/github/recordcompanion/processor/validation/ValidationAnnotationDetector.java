package io.github.recordcompanion.processor.validation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.RecordComponentElement;
import java.util.List;
import java.util.Optional;

/**
 * Detects validation annotations on record components.
 * Handles the fact that annotations appear on accessor methods during annotation processing.
 */
public class ValidationAnnotationDetector {

  /**
   * Checks if any record components have validation annotations.
   */
  public boolean hasValidationAnnotations(List<? extends RecordComponentElement> components) {
    return components.stream().anyMatch(this::hasValidationAnnotation);
  }

  /**
   * Checks if a record component has any validation annotations.
   */
  public boolean hasValidationAnnotation(RecordComponentElement component) {
    return hasNotNullAnnotation(component) || hasMinAnnotation(component);
  }

  /**
   * Checks if a record component has @NotNull annotation.
   */
  public boolean hasNotNullAnnotation(RecordComponentElement component) {
    return hasAnnotation(component, "jakarta.validation.constraints.NotNull");
  }

  /**
   * Checks if a record component has @Min annotation.
   */
  public boolean hasMinAnnotation(RecordComponentElement component) {
    return hasAnnotation(component, "jakarta.validation.constraints.Min");
  }

  /**
   * Gets the minimum value from @Min annotation.
   */
  public long getMinValue(RecordComponentElement component) {
    return getAnnotationValue(component, "jakarta.validation.constraints.Min", "value")
        .map(value -> (Long) value)
        .orElse(0L);
  }

  /**
   * Checks if a record component has a specific annotation.
   */
  private boolean hasAnnotation(RecordComponentElement component, String annotationName) {
    // Check annotations on the accessor method (getter) since component annotations
    // appear on the accessor method during annotation processing
    var accessor = component.getAccessor();
    if (accessor != null) {
      boolean hasAnnotationOnAccessor = accessor.getAnnotationMirrors().stream()
          .anyMatch(annotation -> annotationName.equals(annotation.getAnnotationType().toString()));

      if (hasAnnotationOnAccessor) {
        return true;
      }
    }

    // Also check for annotation on component itself (fallback)
    return component.getAnnotationMirrors().stream()
        .anyMatch(annotation -> annotationName.equals(annotation.getAnnotationType().toString()));
  }

  /**
   * Gets the value of a specific annotation attribute.
   */
  private Optional<Object> getAnnotationValue(RecordComponentElement component, 
                                              String annotationName, String attributeName) {
    // Check annotations on the accessor method first
    var accessor = component.getAccessor();
    if (accessor != null) {
      for (AnnotationMirror annotation : accessor.getAnnotationMirrors()) {
        if (annotationName.equals(annotation.getAnnotationType().toString())) {
          for (var entry : annotation.getElementValues().entrySet()) {
            if (attributeName.equals(entry.getKey().getSimpleName().toString())) {
              return Optional.of(entry.getValue().getValue());
            }
          }
        }
      }
    }

    // Check component annotations as fallback
    for (AnnotationMirror annotation : component.getAnnotationMirrors()) {
      if (annotationName.equals(annotation.getAnnotationType().toString())) {
        for (var entry : annotation.getElementValues().entrySet()) {
          if (attributeName.equals(entry.getKey().getSimpleName().toString())) {
            return Optional.of(entry.getValue().getValue());
          }
        }
      }
    }
    
    return Optional.empty();
  }
}