package io.github.recordcompanion.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.recordcompanion.annotations.Builder;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/** Generates builder pattern implementations for record classes. */
public class BuilderGenerator {

  private final ProcessingEnvironment processingEnv;

  public BuilderGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  private String joinTypeParameters(List<TypeVariableName> typeVariableNames) {
    return typeVariableNames.stream()
        .map(TypeVariableName::toString)
        .collect(java.util.stream.Collectors.joining(", "));
  }

  private String generateUpdaterParameterName(TypeElement recordElement) {
    return generateUpdaterParameterName(recordElement.getSimpleName().toString());
  }

  private TypeName createNestedUpdaterType(
      DeclaredType declaredType, ClassName nestedCompanionClass) {
    List<? extends TypeMirror> nestedTypeArguments = declaredType.getTypeArguments();
    if (nestedTypeArguments.isEmpty()) {
      return nestedCompanionClass.nestedClass("Updater");
    } else {
      TypeName[] nestedTypeNames =
          nestedTypeArguments.stream().map(TypeName::get).toArray(TypeName[]::new);
      return ParameterizedTypeName.get(
          nestedCompanionClass.nestedClass("Updater"), nestedTypeNames);
    }
  }

  private MethodSpec createNestedSetterMethod(
      RecordComponentElement component, TypeName updaterInterfaceType, boolean isInterface) {
    DeclaredType declaredType = (DeclaredType) component.asType();
    TypeElement nestedRecordElement = (TypeElement) declaredType.asElement();
    ClassName nestedCompanionClass = getCompanionClassName(nestedRecordElement);
    String updaterParamName = generateUpdaterParameterName(nestedRecordElement);
    String componentName = component.getSimpleName().toString();

    ClassName consumerType = ClassName.get("java.util.function", "Consumer");
    TypeName nestedUpdaterType = createNestedUpdaterType(declaredType, nestedCompanionClass);
    ParameterizedTypeName nestedUpdaterConsumerType =
        ParameterizedTypeName.get(consumerType, nestedUpdaterType);

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(componentName)
            .addParameter(nestedUpdaterConsumerType, updaterParamName)
            .returns(updaterInterfaceType)
            .addJavadoc("Updates the $N value using a fluent updater.\n", componentName)
            .addJavadoc(
                "@param $N consumer that receives an updater for the nested $N\n",
                updaterParamName,
                componentName)
            .addJavadoc("@return this updater for method chaining\n");

    if (isInterface) {
      methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
    } else {
      methodBuilder
          .addModifiers(Modifier.PUBLIC)
          .addCode(
              generateNestedSetterBody(
                  componentName,
                  nestedCompanionClass,
                  updaterParamName,
                  TypeName.get(component.asType())));
    }

    return methodBuilder.build();
  }

  /** Generates a companion class with builder methods for the given record. */
  public void generateCompanionClass(TypeElement recordElement) throws IOException {
    String recordName = recordElement.getSimpleName().toString();
    String companionName = recordName + "Companion";
    String packageName =
        processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();

    // Extract type parameters from the record
    List<? extends TypeParameterElement> typeParameters = recordElement.getTypeParameters();
    List<TypeVariableName> typeVariableNames =
        typeParameters.stream().map(TypeVariableName::get).toList();

    ClassName recordClass = ClassName.get(packageName, recordName);
    ClassName companionClass = ClassName.get(packageName, companionName);

    // Create parameterized types if the record has type parameters
    TypeName recordTypeName =
        typeVariableNames.isEmpty()
            ? recordClass
            : ParameterizedTypeName.get(recordClass, typeVariableNames.toArray(new TypeName[0]));
    List<? extends RecordComponentElement> components = recordElement.getRecordComponents();

    // Generate the Updater interface
    TypeSpec updaterInterface = generateUpdaterInterface(components, typeVariableNames);

    // Generate the Builder inner class
    TypeSpec builderClass = generateBuilderClass(recordTypeName, components, typeVariableNames);

    // Generate the Companion class
    TypeSpec.Builder companionBuilder =
        TypeSpec.classBuilder(companionName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(
                AnnotationSpec.builder(Generated.class)
                    .addMember(
                        "value",
                        "$S",
                        "io.github.recordcompanion.processor.RecordCompanionProcessor")
                    .build())
            .addJavadoc(
                "Generated companion class for {@link $T} record with builder pattern support.\n",
                recordClass)
            .addJavadoc("@generated by RecordCompanion annotation processor\n")
            .addType(updaterInterface)
            .addType(builderClass)
            .addMethod(generateBuilderMethod(companionClass, typeVariableNames))
            .addMethod(
                generateBuilderWithExistingMethod(
                    recordTypeName, companionClass, components, typeVariableNames))
            .addMethod(generateWithMethod(recordTypeName, companionClass, typeVariableNames));

    // Add validation method if any components have validation annotations
    if (hasValidationAnnotation(components)) {
      companionBuilder
          .addType(generateSimpleConstraintViolation())
          .addMethod(generateValidateMethod(components, typeVariableNames));
    }

    TypeSpec companion = companionBuilder.build();

    // Write the companion class to a file
    JavaFile javaFile = JavaFile.builder(packageName, companion).skipJavaLangImports(true).build();

    javaFile.writeTo(processingEnv.getFiler());
  }

  private TypeSpec generateUpdaterInterface(
      List<? extends RecordComponentElement> components, List<TypeVariableName> typeVariableNames) {
    TypeSpec.Builder updaterBuilder = createUpdaterInterfaceBuilder(typeVariableNames);
    TypeName updaterInterfaceType = createUpdaterInterfaceType(typeVariableNames);

    addSetterMethodsToUpdaterInterface(updaterBuilder, components, updaterInterfaceType);

    return updaterBuilder.build();
  }

  private TypeSpec.Builder createUpdaterInterfaceBuilder(List<TypeVariableName> typeVariableNames) {
    TypeSpec.Builder updaterBuilder =
        TypeSpec.interfaceBuilder("Updater")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addAnnotation(
                AnnotationSpec.builder(Generated.class)
                    .addMember(
                        "value",
                        "$S",
                        "io.github.recordcompanion.processor.RecordCompanionProcessor")
                    .build())
            .addJavadoc("Interface for updating record values without exposing build method.\n");

    // Add type parameters to the interface
    for (TypeVariableName typeVariableName : typeVariableNames) {
      updaterBuilder.addTypeVariable(typeVariableName);
    }

    return updaterBuilder;
  }

  private TypeName createUpdaterInterfaceType(List<TypeVariableName> typeVariableNames) {
    return typeVariableNames.isEmpty()
        ? ClassName.bestGuess("Updater")
        : ParameterizedTypeName.get(
            ClassName.bestGuess("Updater"), typeVariableNames.toArray(new TypeName[0]));
  }

  private void addSetterMethodsToUpdaterInterface(
      TypeSpec.Builder updaterBuilder,
      List<? extends RecordComponentElement> components,
      TypeName updaterInterfaceType) {
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      TypeMirror componentType = component.asType();

      // Always generate the original setter method
      MethodSpec setterMethod =
          MethodSpec.methodBuilder(componentName)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .addParameter(TypeName.get(componentType), componentName)
              .returns(updaterInterfaceType)
              .addJavadoc("Sets the $N value.\n", componentName)
              .addJavadoc("@param $N the new $N value\n", componentName, componentName)
              .addJavadoc("@return this updater for method chaining\n")
              .build();

      updaterBuilder.addMethod(setterMethod);

      // Generate overloaded method for @Builder-annotated record types
      if (isBuilderAnnotatedRecord(component)) {
        updaterBuilder.addMethod(createNestedSetterMethod(component, updaterInterfaceType, true));
      }
    }
  }

  private TypeSpec generateBuilderClass(
      TypeName recordTypeName,
      List<? extends RecordComponentElement> components,
      List<TypeVariableName> typeVariableNames) {

    TypeName updaterInterfaceType = createUpdaterInterfaceType(typeVariableNames);
    TypeName builderClassType = createBuilderClassType(typeVariableNames);

    TypeSpec.Builder builderBuilder =
        createBuilderClassBuilder(updaterInterfaceType, typeVariableNames);
    addFieldsToBuilderClass(builderBuilder, components);
    addSetterMethodsToBuilderClass(builderBuilder, components, builderClassType);
    addBuildMethodToBuilderClass(builderBuilder, recordTypeName, components);

    return builderBuilder.build();
  }

  private TypeName createBuilderClassType(List<TypeVariableName> typeVariableNames) {
    return typeVariableNames.isEmpty()
        ? ClassName.bestGuess("Builder")
        : ParameterizedTypeName.get(
            ClassName.bestGuess("Builder"), typeVariableNames.toArray(new TypeName[0]));
  }

  private TypeSpec.Builder createBuilderClassBuilder(
      TypeName updaterInterfaceType, List<TypeVariableName> typeVariableNames) {
    TypeSpec.Builder builderBuilder =
        TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addAnnotation(
                AnnotationSpec.builder(Generated.class)
                    .addMember(
                        "value",
                        "$S",
                        "io.github.recordcompanion.processor.RecordCompanionProcessor")
                    .build())
            .addSuperinterface(updaterInterfaceType);

    // Add type parameters to the builder class
    for (TypeVariableName typeVariableName : typeVariableNames) {
      builderBuilder.addTypeVariable(typeVariableName);
    }

    return builderBuilder;
  }

  private void addFieldsToBuilderClass(
      TypeSpec.Builder builderBuilder, List<? extends RecordComponentElement> components) {
    for (RecordComponentElement component : components) {
      builderBuilder.addField(
          TypeName.get(component.asType()), component.getSimpleName().toString(), Modifier.PRIVATE);
    }
  }

  private void addSetterMethodsToBuilderClass(
      TypeSpec.Builder builderBuilder,
      List<? extends RecordComponentElement> components,
      TypeName builderClassType) {
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      TypeMirror componentType = component.asType();

      // Always generate the original setter method
      MethodSpec setterMethod =
          MethodSpec.methodBuilder(componentName)
              .addModifiers(Modifier.PUBLIC)
              .addParameter(TypeName.get(componentType), componentName)
              .returns(builderClassType)
              .addStatement("this.$N = $N", componentName, componentName)
              .addStatement("return this")
              .build();

      builderBuilder.addMethod(setterMethod);

      // Generate overloaded method for @Builder-annotated record types
      if (isBuilderAnnotatedRecord(component)) {
        builderBuilder.addMethod(createNestedSetterMethod(component, builderClassType, false));
      }
    }
  }

  private void addBuildMethodToBuilderClass(
      TypeSpec.Builder builderBuilder,
      TypeName recordTypeName,
      List<? extends RecordComponentElement> components) {
    CodeBlock.Builder buildMethodBody = CodeBlock.builder();
    buildMethodBody.add("return new $T(", recordTypeName);

    for (int i = 0; i < components.size(); i++) {
      if (i > 0) buildMethodBody.add(", ");
      buildMethodBody.add("$N", components.get(i).getSimpleName().toString());
    }
    buildMethodBody.add(")");

    MethodSpec buildMethod =
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(recordTypeName)
            .addStatement(buildMethodBody.build())
            .build();

    builderBuilder.addMethod(buildMethod);
  }

  private MethodSpec generateBuilderMethod(
      ClassName companionClass, List<TypeVariableName> typeVariableNames) {

    // Create the Builder return type with proper type parameters
    TypeName builderReturnType =
        typeVariableNames.isEmpty()
            ? companionClass.nestedClass("Builder")
            : ParameterizedTypeName.get(
                companionClass.nestedClass("Builder"), typeVariableNames.toArray(new TypeName[0]));

    String builderConstructor =
        typeVariableNames.isEmpty() ? "return new Builder()" : "return new Builder<>()";

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderReturnType)
            .addStatement(builderConstructor)
            .addJavadoc("Creates a new builder instance.\n")
            .addJavadoc("@return a new builder instance\n");

    // Add type parameters to the method
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    return methodBuilder.build();
  }

  private MethodSpec generateBuilderWithExistingMethod(
      TypeName recordTypeName,
      ClassName companionClass,
      List<? extends RecordComponentElement> components,
      List<TypeVariableName> typeVariableNames) {
    // Create the Builder return type with proper type parameters
    TypeName builderReturnType =
        typeVariableNames.isEmpty()
            ? companionClass.nestedClass("Builder")
            : ParameterizedTypeName.get(
                companionClass.nestedClass("Builder"), typeVariableNames.toArray(new TypeName[0]));

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(recordTypeName, "existing")
            .returns(builderReturnType)
            .addJavadoc(
                "Creates a new builder instance initialized with values from an existing record.\n")
            .addJavadoc("@param existing the existing record to copy values from\n")
            .addJavadoc("@return a new builder instance with copied values\n")
            .addCode(generateBuilderWithExistingBody(components, typeVariableNames));

    // Add type parameters to the method
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    return methodBuilder.build();
  }

  private CodeBlock generateBuilderWithExistingBody(
      List<? extends RecordComponentElement> components, List<TypeVariableName> typeVariableNames) {
    CodeBlock.Builder body = CodeBlock.builder();

    // Generate Builder type with proper type parameters
    if (typeVariableNames.isEmpty()) {
      body.addStatement("Builder builder = new Builder()");
    } else {
      String typeParams = joinTypeParameters(typeVariableNames);
      body.addStatement("Builder<$L> builder = new Builder<>()", typeParams);
    }

    // Copy all values from existing record
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      body.addStatement("builder.$N = existing.$N()", componentName, componentName);
    }

    body.addStatement("return builder");
    return body.build();
  }

  private MethodSpec generateWithMethod(
      TypeName recordTypeName, ClassName companionClass, List<TypeVariableName> typeVariableNames) {
    ClassName consumerType = ClassName.get("java.util.function", "Consumer");

    // Create the Updater type with proper type parameters for the Consumer
    TypeName updaterType =
        typeVariableNames.isEmpty()
            ? companionClass.nestedClass("Updater")
            : ParameterizedTypeName.get(
                companionClass.nestedClass("Updater"), typeVariableNames.toArray(new TypeName[0]));

    ParameterizedTypeName updaterConsumerType =
        ParameterizedTypeName.get(consumerType, updaterType);

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("with")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(recordTypeName, "existing")
            .addParameter(updaterConsumerType, "updater")
            .returns(recordTypeName)
            .addJavadoc(
                "Creates a new record instance by applying modifications to an existing record.\n")
            .addJavadoc("@param existing the existing record to base the new record on\n")
            .addJavadoc(
                "@param updater a consumer that receives an updater initialized with the existing record's values\n")
            .addJavadoc("@return a new record instance with the applied modifications\n")
            .addCode(generateWithMethodBody(typeVariableNames));

    // Add type parameters to the method
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    return methodBuilder.build();
  }

  private CodeBlock generateWithMethodBody(List<TypeVariableName> typeVariableNames) {
    CodeBlock.Builder body = CodeBlock.builder();

    // Generate Builder type with proper type parameters
    if (typeVariableNames.isEmpty()) {
      body.addStatement("Builder builder = builder(existing)");
    } else {
      String typeParams = joinTypeParameters(typeVariableNames);
      body.addStatement("Builder<$L> builder = builder(existing)", typeParams);
    }

    body.addStatement("updater.accept(builder)");
    body.addStatement("return builder.build()");

    return body.build();
  }

  /** Generates the method body for nested record setter methods. */
  private CodeBlock generateNestedSetterBody(
      String componentName,
      ClassName nestedCompanionClass,
      String parameterName,
      TypeName componentTypeName) {
    CodeBlock.Builder body = CodeBlock.builder();

    // Update existing nested record or create new one if null
    body.beginControlFlow("if (this.$N != null)", componentName);
    body.addStatement(
        "this.$N = $T.with(this.$N, $N)",
        componentName,
        nestedCompanionClass,
        componentName,
        parameterName);
    body.nextControlFlow("else");
    // Create a new builder, apply the consumer, then build
    // Extract type arguments from the component type for proper generic support
    if (componentTypeName instanceof ParameterizedTypeName parameterizedType) {
      TypeName[] typeArguments = parameterizedType.typeArguments.toArray(new TypeName[0]);
      String typeArgsStr =
          Stream.of(typeArguments).map(TypeName::toString).collect(Collectors.joining(", "));
      body.addStatement("var builder = $T.<$L>builder()", nestedCompanionClass, typeArgsStr);
    } else {
      body.addStatement("var builder = $T.builder()", nestedCompanionClass);
    }
    body.addStatement("$N.accept(builder)", parameterName);
    body.addStatement("this.$N = builder.build()", componentName);
    body.endControlFlow();
    body.addStatement("return this");

    return body.build();
  }

  /** Checks if a record component type is a record with @Builder annotation. */
  private boolean isBuilderAnnotatedRecord(RecordComponentElement component) {
    TypeMirror componentType = component.asType();

    if (!(componentType instanceof DeclaredType declaredType)) {
      return false;
    }

    TypeElement typeElement = (TypeElement) declaredType.asElement();

    // Check if it's a record and has @Builder annotation
    return typeElement.getKind() == ElementKind.RECORD
        && typeElement.getAnnotation(Builder.class) != null;
  }

  /** Gets the companion class name for a given record type. */
  private ClassName getCompanionClassName(TypeElement recordElement) {
    String packageName =
        processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();
    String recordName = recordElement.getSimpleName().toString();
    String companionName = recordName + "Companion";
    return ClassName.get(packageName, companionName);
  }

  /**
   * Generates a descriptive parameter name from a class name by converting to camelCase and adding
   * 'Updater' suffix. Examples: Address -> addressUpdater, PersonDetails -> personDetailsUpdater
   */
  private String generateUpdaterParameterName(String className) {
    if (className.isEmpty()) {
      return "updater";
    }

    // Convert first character to lowercase and append "Updater"
    return Character.toLowerCase(className.charAt(0)) + className.substring(1) + "Updater";
  }

  /** Checks if any record components have validation annotations. */
  private boolean hasValidationAnnotation(List<? extends RecordComponentElement> components) {
    return components.stream().anyMatch(this::hasValidationAnnotation);
  }

  /** Checks if a record component has validation annotations. */
  private boolean hasValidationAnnotation(RecordComponentElement component) {
    return hasNotNullAnnotation(component) || hasMinAnnotation(component);
  }

  /** Checks if a record component has @NotNull annotation. */
  private boolean hasNotNullAnnotation(RecordComponentElement component) {
    return hasAnnotation(component, "jakarta.validation.constraints.NotNull");
  }

  /** Checks if a record component has @Min annotation. */
  private boolean hasMinAnnotation(RecordComponentElement component) {
    return hasAnnotation(component, "jakarta.validation.constraints.Min");
  }

  /** Checks if a record component has a specific annotation. */
  private boolean hasAnnotation(RecordComponentElement component, String annotationName) {
    // Check annotations on the accessor method (getter) since component annotations
    // appear on the accessor method during annotation processing
    javax.lang.model.element.ExecutableElement accessor = component.getAccessor();
    if (accessor != null) {
      boolean hasAnnotationOnAccessor =
          accessor.getAnnotationMirrors().stream()
              .anyMatch(
                  annotation -> {
                    String name = annotation.getAnnotationType().toString();
                    return annotationName.equals(name);
                  });

      if (hasAnnotationOnAccessor) {
        return true;
      }
    }

    // Also check for annotation on component itself (fallback)
    return component.getAnnotationMirrors().stream()
        .anyMatch(
            annotation -> {
              String name = annotation.getAnnotationType().toString();
              return annotationName.equals(name);
            });
  }

  /** Gets the minimum value from @Min annotation. */
  private long getMinValue(RecordComponentElement component) {
    // Check annotations on the accessor method first
    javax.lang.model.element.ExecutableElement accessor = component.getAccessor();
    if (accessor != null) {
      for (javax.lang.model.element.AnnotationMirror annotation : accessor.getAnnotationMirrors()) {
        if ("jakarta.validation.constraints.Min".equals(annotation.getAnnotationType().toString())) {
          for (var entry : annotation.getElementValues().entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString())) {
              return (Long) entry.getValue().getValue();
            }
          }
        }
      }
    }

    // Check component annotations as fallback
    for (javax.lang.model.element.AnnotationMirror annotation : component.getAnnotationMirrors()) {
      if ("jakarta.validation.constraints.Min".equals(annotation.getAnnotationType().toString())) {
        for (var entry : annotation.getElementValues().entrySet()) {
          if ("value".equals(entry.getKey().getSimpleName().toString())) {
            return (Long) entry.getValue().getValue();
          }
        }
      }
    }
    
    return 0; // Default minimum value
  }

  /** Generates a static validate method for validation annotations. */
  private MethodSpec generateValidateMethod(
      List<? extends RecordComponentElement> components, List<TypeVariableName> typeVariableNames) {

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("validate")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.VOID)
            .addJavadoc("Validates record field values for validation constraints.\n")
            .addJavadoc(
                "@throws jakarta.validation.ConstraintViolationException if validation fails\n");

    // Add parameters only for components with validation annotations
    for (RecordComponentElement component : components) {
      if (hasValidationAnnotation(component)) {
        methodBuilder.addParameter(
            TypeName.get(component.asType()), component.getSimpleName().toString());
      }
    }

    // Add type parameters if needed
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    // Generate method body with @NotNull checks
    CodeBlock.Builder body = CodeBlock.builder();

    body.addStatement(
        "$T<$T<Object>> violations = new $T<>()",
        java.util.Set.class,
        ClassName.get("jakarta.validation", "ConstraintViolation"),
        java.util.HashSet.class);

    for (RecordComponentElement component : components) {
      if (hasValidationAnnotation(component)) {
        String fieldName = component.getSimpleName().toString();
        
        // @NotNull validation
        if (hasNotNullAnnotation(component)) {
          body.beginControlFlow("if ($N == null)", fieldName)
              .addStatement(
                  "violations.add(new SimpleConstraintViolation($S, $S, $N))",
                  fieldName,
                  fieldName + " cannot be null",
                  fieldName)
              .endControlFlow();
        }
        
        // @Min validation
        if (hasMinAnnotation(component)) {
          // Get the minimum value from the annotation
          long minValue = getMinValue(component);
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
      }
    }

    body.beginControlFlow("if (!violations.isEmpty())")
        .addStatement(
            "throw new $T(violations)",
            ClassName.get("jakarta.validation", "ConstraintViolationException"))
        .endControlFlow();

    methodBuilder.addCode(body.build());
    return methodBuilder.build();
  }

  /** Generates a simple ConstraintViolation implementation. */
  private TypeSpec generateSimpleConstraintViolation() {
    ClassName constraintViolationType = ClassName.get("jakarta.validation", "ConstraintViolation");
    ParameterizedTypeName constraintViolationOfObject =
        ParameterizedTypeName.get(constraintViolationType, TypeName.OBJECT);

    return TypeSpec.classBuilder("SimpleConstraintViolation")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addSuperinterface(constraintViolationOfObject)
        .addField(String.class, "propertyPath", Modifier.PRIVATE, Modifier.FINAL)
        .addField(String.class, "message", Modifier.PRIVATE, Modifier.FINAL)
        .addField(Object.class, "invalidValue", Modifier.PRIVATE, Modifier.FINAL)
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "propertyPath")
                .addParameter(String.class, "message")
                .addParameter(Object.class, "invalidValue")
                .addStatement("this.propertyPath = propertyPath")
                .addStatement("this.message = message")
                .addStatement("this.invalidValue = invalidValue")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getMessage")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return message")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getMessageTemplate")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return message")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getInvalidValue")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addStatement("return invalidValue")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getPropertyPath")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("jakarta.validation", "Path"))
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getRootBean")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getRootBeanClass")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), TypeName.OBJECT))
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getLeafBean")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getExecutableParameters")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(Object[].class))
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getExecutableReturnValue")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("getConstraintDescriptor")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("jakarta.validation.metadata", "ConstraintDescriptor"))
                .addStatement("return null")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("unwrap")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("U"))
                .addParameter(
                    ParameterizedTypeName.get(
                        ClassName.get(Class.class), TypeVariableName.get("U")),
                    "type")
                .returns(TypeVariableName.get("U"))
                .addStatement(
                    "throw new $T($S)", UnsupportedOperationException.class, "Not supported")
                .build())
        .build();
  }
}
