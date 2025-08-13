package io.github.recordcompanion.processor.validation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import java.util.List;

/**
 * Generates validation methods and supporting classes for record validation.
 */
public class ValidationMethodGenerator {

  private final ValidationAnnotationDetector annotationDetector;

  public ValidationMethodGenerator(ValidationAnnotationDetector annotationDetector) {
    this.annotationDetector = annotationDetector;
  }

  /**
   * Generates a static validate method for validation annotations.
   */
  public MethodSpec generateValidateMethod(List<? extends RecordComponentElement> components, 
                                           List<TypeVariableName> typeVariableNames) {

    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("validate")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TypeName.VOID)
        .addJavadoc("Validates record field values for validation constraints.\n")
        .addJavadoc("@throws jakarta.validation.ConstraintViolationException if validation fails\n");

    // Add parameters only for components with validation annotations
    for (RecordComponentElement component : components) {
      if (annotationDetector.hasValidationAnnotation(component)) {
        methodBuilder.addParameter(
            TypeName.get(component.asType()), component.getSimpleName().toString());
      }
    }

    // Add type parameters if needed
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    // Generate method body with validation checks
    methodBuilder.addCode(generateValidationMethodBody(components));
    return methodBuilder.build();
  }

  /**
   * Generates the validation method body.
   */
  private CodeBlock generateValidationMethodBody(List<? extends RecordComponentElement> components) {
    CodeBlock.Builder body = CodeBlock.builder();

    body.addStatement(
        "$T<$T<Object>> violations = new $T<>()",
        java.util.Set.class,
        ClassName.get("jakarta.validation", "ConstraintViolation"),
        java.util.HashSet.class);

    for (RecordComponentElement component : components) {
      if (annotationDetector.hasValidationAnnotation(component)) {
        String fieldName = component.getSimpleName().toString();
        
        // @NotNull validation
        if (annotationDetector.hasNotNullAnnotation(component)) {
          generateNotNullValidation(body, fieldName);
        }
        
        // @Min validation
        if (annotationDetector.hasMinAnnotation(component)) {
          generateMinValidation(body, component, fieldName);
        }
      }
    }

    body.beginControlFlow("if (!violations.isEmpty())")
        .addStatement(
            "throw new $T(violations)",
            ClassName.get("jakarta.validation", "ConstraintViolationException"))
        .endControlFlow();

    return body.build();
  }

  /**
   * Generates @NotNull validation logic.
   */
  private void generateNotNullValidation(CodeBlock.Builder body, String fieldName) {
    body.beginControlFlow("if ($N == null)", fieldName)
        .addStatement(
            "violations.add(new SimpleConstraintViolation($S, $S, $N))",
            fieldName,
            fieldName + " cannot be null",
            fieldName)
        .endControlFlow();
  }

  /**
   * Generates @Min validation logic.
   */
  private void generateMinValidation(CodeBlock.Builder body, 
                                     RecordComponentElement component, 
                                     String fieldName) {
    long minValue = annotationDetector.getMinValue(component);
    TypeName componentType = TypeName.get(component.asType());
    
    // Handle different numeric types
    if (componentType.equals(TypeName.INT) || componentType.equals(TypeName.get(Integer.class))) {
      body.beginControlFlow("if ($N != null && $N < $L)", fieldName, fieldName, minValue)
          .addStatement(
              "violations.add(new SimpleConstraintViolation($S, $S, $N))",
              fieldName,
              fieldName + " must be at least " + minValue,
              fieldName)
          .endControlFlow();
    } else if (componentType.equals(TypeName.LONG) || componentType.equals(TypeName.get(Long.class))) {
      body.beginControlFlow("if ($N != null && $N < $LL)", fieldName, fieldName, minValue)
          .addStatement(
              "violations.add(new SimpleConstraintViolation($S, $S, $N))",
              fieldName,
              fieldName + " must be at least " + minValue,
              fieldName)
          .endControlFlow();
    }
  }

  /**
   * Generates a simple ConstraintViolation implementation.
   */
  public TypeSpec generateSimpleConstraintViolation() {
    ClassName constraintViolationType = ClassName.get("jakarta.validation", "ConstraintViolation");
    ParameterizedTypeName constraintViolationOfObject = 
        ParameterizedTypeName.get(constraintViolationType, TypeName.OBJECT);

    return TypeSpec.classBuilder("SimpleConstraintViolation")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addSuperinterface(constraintViolationOfObject)
        .addField(String.class, "propertyPath", Modifier.PRIVATE, Modifier.FINAL)
        .addField(String.class, "message", Modifier.PRIVATE, Modifier.FINAL)
        .addField(Object.class, "invalidValue", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(generateConstraintViolationConstructor())
        .addMethod(generateGetMessage())
        .addMethod(generateGetMessageTemplate())
        .addMethod(generateGetInvalidValue())
        .addMethod(generateGetPropertyPath())
        .addMethod(generateGetRootBean())
        .addMethod(generateGetRootBeanClass())
        .addMethod(generateGetLeafBean())
        .addMethod(generateGetExecutableParameters())
        .addMethod(generateGetExecutableReturnValue())
        .addMethod(generateGetConstraintDescriptor())
        .addMethod(generateUnwrap())
        .build();
  }

  private MethodSpec generateConstraintViolationConstructor() {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(String.class, "propertyPath")
        .addParameter(String.class, "message")
        .addParameter(Object.class, "invalidValue")
        .addStatement("this.propertyPath = propertyPath")
        .addStatement("this.message = message")
        .addStatement("this.invalidValue = invalidValue")
        .build();
  }

  private MethodSpec generateGetMessage() {
    return MethodSpec.methodBuilder("getMessage")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return message")
        .build();
  }

  private MethodSpec generateGetMessageTemplate() {
    return MethodSpec.methodBuilder("getMessageTemplate")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return message")
        .build();
  }

  private MethodSpec generateGetInvalidValue() {
    return MethodSpec.methodBuilder("getInvalidValue")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(Object.class)
        .addStatement("return invalidValue")
        .build();
  }

  private MethodSpec generateGetPropertyPath() {
    return MethodSpec.methodBuilder("getPropertyPath")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get("jakarta.validation", "Path"))
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateGetRootBean() {
    return MethodSpec.methodBuilder("getRootBean")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(Object.class)
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateGetRootBeanClass() {
    return MethodSpec.methodBuilder("getRootBeanClass")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get(Class.class), TypeName.OBJECT))
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateGetLeafBean() {
    return MethodSpec.methodBuilder("getLeafBean")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(Object.class)
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateGetExecutableParameters() {
    return MethodSpec.methodBuilder("getExecutableParameters")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeName.get(Object[].class))
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateGetExecutableReturnValue() {
    return MethodSpec.methodBuilder("getExecutableReturnValue")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(Object.class)
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateGetConstraintDescriptor() {
    return MethodSpec.methodBuilder("getConstraintDescriptor")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get("jakarta.validation.metadata", "ConstraintDescriptor"))
        .addStatement("return null")
        .build();
  }

  private MethodSpec generateUnwrap() {
    return MethodSpec.methodBuilder("unwrap")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariable(TypeVariableName.get("U"))
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("U")),
            "type")
        .returns(TypeVariableName.get("U"))
        .addStatement(
            "throw new $T($S)", UnsupportedOperationException.class, "Not supported")
        .build();
  }
}