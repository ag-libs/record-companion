package io.github.recordcompanion.validcheck.internal;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
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

/** Generates check classes for ValidCheck integration based on Bean Validation annotations. */
public class CheckGenerator {

  private static final String CHECK_SUFFIX = "Check";
  private static final ClassName VALIDCHECK_CLASS =
      ClassName.get("io.github.validcheck", "ValidCheck");
  private static final ClassName BATCH_VALIDATOR =
      ClassName.get("io.github.validcheck", "BatchValidator");
  private static final ClassName VALIDATOR = ClassName.get("io.github.validcheck", "Validator");

  private final ProcessingEnvironment processingEnv;

  public CheckGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv cannot be null");
  }

  public void generateCheck(TypeElement recordElement) throws IOException {
    String className = recordElement.getSimpleName() + CHECK_SUFFIX;
    String packageName = processingEnv.getElementUtils().getPackageOf(recordElement).toString();

    List<? extends RecordComponentElement> components = recordElement.getRecordComponents();
    List<ValidatedComponent> validatedComponents = extractValidatedComponents(components);

    // Only generate if there are components with validation annotations
    if (validatedComponents.isEmpty()) {
      return;
    }

    TypeSpec.Builder checkClass =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addJavadoc("Generated check class for $L record.\n\n", recordElement.getSimpleName())
            .addJavadoc(
                "<p>This class provides validation methods that map Bean Validation annotations to ValidCheck API\n")
            .addJavadoc("calls for components with validation annotations.\n");

    // Add private constructor
    MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addComment("Utility class")
            .build();
    checkClass.addMethod(constructor);

    // Generate method parameters and validation chain
    List<ParameterSpec> parameters = generateParameters(validatedComponents);
    CodeBlock validationChain = generateValidationChain(validatedComponents);

    // Add check method
    MethodSpec checkMethod =
        createCheckMethod(parameters, validationChain, recordElement.getSimpleName().toString());
    checkClass.addMethod(checkMethod);

    // Add require method
    MethodSpec requireMethod =
        createRequireMethod(parameters, validationChain, recordElement.getSimpleName().toString());
    checkClass.addMethod(requireMethod);

    // Add validate method
    MethodSpec validateMethod =
        createValidateMethod(parameters, recordElement.getSimpleName().toString());
    checkClass.addMethod(validateMethod);

    // Add buildValidation method
    MethodSpec buildValidationMethod = createBuildValidationMethod(parameters, validationChain);
    checkClass.addMethod(buildValidationMethod);

    JavaFile javaFile = JavaFile.builder(packageName, checkClass.build()).indent("  ").build();
    javaFile.writeTo(processingEnv.getFiler());
  }

  private List<ValidatedComponent> extractValidatedComponents(
      List<? extends RecordComponentElement> components) {
    List<ValidatedComponent> validatedComponents = new ArrayList<>();

    for (RecordComponentElement component : components) {
      List<ValidationRule> rules = extractValidationRules(component);
      if (!rules.isEmpty()) {
        validatedComponents.add(new ValidatedComponent(component, rules));
      }
    }

    return validatedComponents;
  }

  private List<ValidationRule> extractValidationRules(RecordComponentElement component) {
    List<ValidationRule> rules = new ArrayList<>();
    String componentName = component.getSimpleName().toString();

    // Get the accessor method element, which is where annotations are typically placed
    Element accessor = component.getAccessor();

    // Check if the field is nullable (no @NotNull annotation)
    boolean isNullable = accessor.getAnnotation(NotNull.class) == null;

    // Check for @NotNull
    NotNull notNullAnnotation = accessor.getAnnotation(NotNull.class);
    if (notNullAnnotation != null) {
      rules.add(new ValidationRule("notNull", componentName, List.of()));
    }

    // Check for @NotEmpty
    NotEmpty notEmptyAnnotation = accessor.getAnnotation(NotEmpty.class);
    if (notEmptyAnnotation != null) {
      rules.add(new ValidationRule("notNullOrEmpty", componentName, List.of()));
    }

    // Check for @NotBlank
    NotBlank notBlankAnnotation = accessor.getAnnotation(NotBlank.class);
    if (notBlankAnnotation != null) {
      if (isNullable) {
        rules.add(new ValidationRule("nullOrNotBlank", componentName, List.of()));
      } else {
        rules.add(new ValidationRule("notBlank", componentName, List.of()));
      }
    }

    // Check for @Size
    Size sizeAnnotation = accessor.getAnnotation(Size.class);
    if (sizeAnnotation != null) {
      int min = sizeAnnotation.min();
      int max = sizeAnnotation.max();

      // Determine if this is a Collection type or String/CharSequence
      String typeName = component.asType().toString();
      boolean isCollection =
          typeName.contains("Collection") || typeName.contains("List") || typeName.contains("Set");
      boolean isMap = typeName.contains("Map");

      if (isCollection) {
        // Use hasSize for collections (List, Set, Collection)
        if (isNullable) {
          rules.add(new ValidationRule("nullOrHasSize", componentName, List.of(min, max)));
        } else {
          rules.add(new ValidationRule("hasSize", componentName, List.of(min, max)));
        }
      } else if (isMap) {
        // For Maps, we need to validate the size differently - skip for now
        // TODO: Add Map size validation support when ValidCheck supports it
        // For now, fall back to not adding validation for Maps
      } else {
        // Use hasLength for strings/char sequences
        if (isNullable) {
          rules.add(new ValidationRule("nullOrHasLength", componentName, List.of(min, max)));
        } else {
          rules.add(new ValidationRule("hasLength", componentName, List.of(min, max)));
        }
      }
    }

    // Check for @Pattern
    Pattern patternAnnotation = accessor.getAnnotation(Pattern.class);
    if (patternAnnotation != null) {
      String regex = patternAnnotation.regexp();
      if (isNullable) {
        rules.add(new ValidationRule("nullOrMatches", componentName, List.of(regex)));
      } else {
        rules.add(new ValidationRule("matches", componentName, List.of(regex)));
      }
    }

    // Check for @Min and @Max - handle individually and combined
    Min minAnnotation = accessor.getAnnotation(Min.class);
    Max maxAnnotation = accessor.getAnnotation(Max.class);

    if (minAnnotation != null && maxAnnotation != null) {
      // Both present - use range validation (this works)
      long min = minAnnotation.value();
      long max = maxAnnotation.value();
      rules.add(new ValidationRule("inRange", componentName, List.of((int) min, (int) max)));
    } else if (minAnnotation != null) {
      // Only @Min present - use min() method from ValidCheck API
      long min = minAnnotation.value();
      if (isNullable) {
        rules.add(new ValidationRule("nullOrMin", componentName, List.of((int) min)));
      } else {
        rules.add(new ValidationRule("min", componentName, List.of((int) min)));
      }
    } else if (maxAnnotation != null) {
      // Only @Max present - use max() method from ValidCheck API
      long max = maxAnnotation.value();
      if (isNullable) {
        rules.add(new ValidationRule("nullOrMax", componentName, List.of((int) max)));
      } else {
        rules.add(new ValidationRule("max", componentName, List.of((int) max)));
      }
    }

    // Check for @DecimalMin and @DecimalMax - handle individually and combined
    DecimalMin decimalMinAnnotation = accessor.getAnnotation(DecimalMin.class);
    DecimalMax decimalMaxAnnotation = accessor.getAnnotation(DecimalMax.class);

    if (decimalMinAnnotation != null && decimalMaxAnnotation != null) {
      // Both present - use range validation
      double min = Double.parseDouble(decimalMinAnnotation.value());
      double max = Double.parseDouble(decimalMaxAnnotation.value());
      rules.add(new ValidationRule("inRange", componentName, List.of(min, max)));
    } else if (decimalMinAnnotation != null) {
      // Only @DecimalMin present - use min() method from ValidCheck API
      double min = Double.parseDouble(decimalMinAnnotation.value());
      if (isNullable) {
        rules.add(new ValidationRule("nullOrMin", componentName, List.of(min)));
      } else {
        rules.add(new ValidationRule("min", componentName, List.of(min)));
      }
    } else if (decimalMaxAnnotation != null) {
      // Only @DecimalMax present - use max() method from ValidCheck API
      double max = Double.parseDouble(decimalMaxAnnotation.value());
      if (isNullable) {
        rules.add(new ValidationRule("nullOrMax", componentName, List.of(max)));
      } else {
        rules.add(new ValidationRule("max", componentName, List.of(max)));
      }
    }

    // Check for @Positive (number > 0)
    Positive positiveAnnotation = accessor.getAnnotation(Positive.class);
    if (positiveAnnotation != null) {
      // Map @Positive to inRange(value, 1, Integer.MAX_VALUE) for practical validation
      rules.add(new ValidationRule("inRange", componentName, List.of(1, Integer.MAX_VALUE)));
    }

    // Check for @Negative (number < 0)
    Negative negativeAnnotation = accessor.getAnnotation(Negative.class);
    if (negativeAnnotation != null) {
      // Map @Negative to inRange(value, Integer.MIN_VALUE, -1) for practical validation
      rules.add(new ValidationRule("inRange", componentName, List.of(Integer.MIN_VALUE, -1)));
    }

    // Check for @PositiveOrZero (number >= 0)
    PositiveOrZero positiveOrZeroAnnotation = accessor.getAnnotation(PositiveOrZero.class);
    if (positiveOrZeroAnnotation != null) {
      // Map @PositiveOrZero to inRange(value, 0, Integer.MAX_VALUE) for practical validation
      rules.add(new ValidationRule("inRange", componentName, List.of(0, Integer.MAX_VALUE)));
    }

    // Check for @NegativeOrZero (number <= 0)
    NegativeOrZero negativeOrZeroAnnotation = accessor.getAnnotation(NegativeOrZero.class);
    if (negativeOrZeroAnnotation != null) {
      // Map @NegativeOrZero to inRange(value, Integer.MIN_VALUE, 0) for practical validation
      rules.add(new ValidationRule("inRange", componentName, List.of(Integer.MIN_VALUE, 0)));
    }

    return rules;
  }

  private List<ParameterSpec> generateParameters(List<ValidatedComponent> validatedComponents) {
    List<ParameterSpec> parameters = new ArrayList<>();
    for (ValidatedComponent component : validatedComponents) {
      TypeName paramType = TypeName.get(component.element().asType());
      String paramName = component.element().getSimpleName().toString();
      parameters.add(ParameterSpec.builder(paramType, paramName).build());
    }
    return parameters;
  }

  private CodeBlock generateValidationChain(List<ValidatedComponent> validatedComponents) {
    CodeBlock.Builder chain = CodeBlock.builder();
    chain.add("return validator");

    for (ValidatedComponent component : validatedComponents) {
      for (ValidationRule rule : component.rules()) {
        chain.add("\n        .$L($L", rule.method(), rule.fieldName());
        for (Object arg : rule.args()) {
          if (arg instanceof String) {
            chain.add(", $S", arg); // Use $S for string literals (adds quotes)
          } else {
            chain.add(", $L", arg); // Use $L for other literals (numbers, etc.)
          }
        }
        chain.add(", $S)", rule.fieldName());
      }
    }

    chain.add(";");
    return chain.build();
  }

  private MethodSpec createCheckMethod(
      List<ParameterSpec> parameters, CodeBlock validationChain, String recordName) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("check")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(BATCH_VALIDATOR)
            .addJavadoc("Creates a batch validator for $L validation.\n\n", recordName);

    for (ParameterSpec param : parameters) {
      method.addParameter(param);
      method.addJavadoc(
          "@param $L the $L to validate (from validation annotations)\n", param.name, param.name);
    }
    method.addJavadoc("@return BatchValidator for manual validation control\n");

    method.addStatement(
        "return ($T) buildValidation($T.check(), $L)",
        BATCH_VALIDATOR,
        VALIDCHECK_CLASS,
        parameters.stream().map(p -> p.name).reduce((a, b) -> a + ", " + b).orElse(""));

    return method.build();
  }

  private MethodSpec createRequireMethod(
      List<ParameterSpec> parameters, CodeBlock validationChain, String recordName) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("require")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(VALIDATOR)
            .addJavadoc(
                "Creates a validator for $L validation with immediate validation.\n\n", recordName);

    for (ParameterSpec param : parameters) {
      method.addParameter(param);
      method.addJavadoc(
          "@param $L the $L to validate (from validation annotations)\n", param.name, param.name);
    }
    method.addJavadoc("@return Validator for chaining additional validations\n");

    method.addStatement(
        "return buildValidation($T.require(), $L)",
        VALIDCHECK_CLASS,
        parameters.stream().map(p -> p.name).reduce((a, b) -> a + ", " + b).orElse(""));

    return method.build();
  }

  private MethodSpec createValidateMethod(List<ParameterSpec> parameters, String recordName) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("validate")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(void.class)
            .addJavadoc(
                "Convenience method that validates $L and throws on failure.\n\n", recordName);

    for (ParameterSpec param : parameters) {
      method.addParameter(param);
      method.addJavadoc(
          "@param $L the $L to validate (from validation annotations)\n", param.name, param.name);
    }

    method.addStatement(
        "check($L).validate()",
        parameters.stream().map(p -> p.name).reduce((a, b) -> a + ", " + b).orElse(""));

    return method.build();
  }

  private MethodSpec createBuildValidationMethod(
      List<ParameterSpec> parameters, CodeBlock validationChain) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("buildValidation")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(VALIDATOR)
            .addParameter(VALIDATOR, "validator")
            .addJavadoc("Builds validation chain from Bean Validation annotations.\n\n")
            .addJavadoc("@param validator the base validator instance\n");

    for (ParameterSpec param : parameters) {
      method.addParameter(param);
      method.addJavadoc("@param $L the $L to validate\n", param.name, param.name);
    }
    method.addJavadoc("@return validator with validation chain applied\n");

    method.addCode(validationChain);

    return method.build();
  }

  private record ValidatedComponent(RecordComponentElement element, List<ValidationRule> rules) {}

  private record ValidationRule(String method, String fieldName, List<Object> args) {}
}
