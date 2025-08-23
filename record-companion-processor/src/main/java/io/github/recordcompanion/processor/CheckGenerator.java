package io.github.recordcompanion.processor;

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

    // Check for @NotNull
    NotNull notNullAnnotation = accessor.getAnnotation(NotNull.class);
    if (notNullAnnotation != null) {
      rules.add(new ValidationRule("notNull", componentName));
    }

    // Check for @NotEmpty
    NotEmpty notEmptyAnnotation = accessor.getAnnotation(NotEmpty.class);
    if (notEmptyAnnotation != null) {
      rules.add(new ValidationRule("notNullOrEmpty", componentName));
    }

    // Check for @NotBlank
    NotBlank notBlankAnnotation = accessor.getAnnotation(NotBlank.class);
    if (notBlankAnnotation != null) {
      // TODO: Add notBlank() method to ValidCheck core
      // Temporarily skip until ValidCheck core has notBlank() method
    }

    // Check for @Size
    Size sizeAnnotation = accessor.getAnnotation(Size.class);
    if (sizeAnnotation != null) {
      int min = sizeAnnotation.min();
      int max = sizeAnnotation.max();
      rules.add(new ValidationRule("hasLength", componentName, min, max));
    }

    // Check for @Pattern
    Pattern patternAnnotation = accessor.getAnnotation(Pattern.class);
    if (patternAnnotation != null) {
      String regex = patternAnnotation.regexp();
      rules.add(new ValidationRule("matches", componentName, regex));
    }

    // Check for @Min and @Max - handle individually and combined
    Min minAnnotation = accessor.getAnnotation(Min.class);
    Max maxAnnotation = accessor.getAnnotation(Max.class);

    if (minAnnotation != null && maxAnnotation != null) {
      // Both present - use range validation (this works)
      long min = minAnnotation.value();
      long max = maxAnnotation.value();
      rules.add(new ValidationRule("inRange", componentName, (int) min, (int) max));
    } else if (minAnnotation != null) {
      // Only @Min present - TODO: Add min() method to ValidCheck core
      // Temporarily skip until ValidCheck core has min() method
    } else if (maxAnnotation != null) {
      // Only @Max present - TODO: Add max() method to ValidCheck core
      // Temporarily skip until ValidCheck core has max() method
    }

    // Check for @Positive (number > 0)
    Positive positiveAnnotation = accessor.getAnnotation(Positive.class);
    if (positiveAnnotation != null) {
      // Map @Positive to inRange(value, 1, Integer.MAX_VALUE) for practical validation
      rules.add(new ValidationRule("inRange", componentName, 1, Integer.MAX_VALUE));
    }

    // Check for @Negative (number < 0)
    Negative negativeAnnotation = accessor.getAnnotation(Negative.class);
    if (negativeAnnotation != null) {
      // Map @Negative to inRange(value, Integer.MIN_VALUE, -1) for practical validation
      rules.add(new ValidationRule("inRange", componentName, Integer.MIN_VALUE, -1));
    }

    // Check for @PositiveOrZero (number >= 0)
    PositiveOrZero positiveOrZeroAnnotation = accessor.getAnnotation(PositiveOrZero.class);
    if (positiveOrZeroAnnotation != null) {
      // Map @PositiveOrZero to inRange(value, 0, Integer.MAX_VALUE) for practical validation
      rules.add(new ValidationRule("inRange", componentName, 0, Integer.MAX_VALUE));
    }

    // Check for @NegativeOrZero (number <= 0)
    NegativeOrZero negativeOrZeroAnnotation = accessor.getAnnotation(NegativeOrZero.class);
    if (negativeOrZeroAnnotation != null) {
      // Map @NegativeOrZero to inRange(value, Integer.MIN_VALUE, 0) for practical validation
      rules.add(new ValidationRule("inRange", componentName, Integer.MIN_VALUE, 0));
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

  private record ValidationRule(String method, String fieldName, Object... args) {}
}
