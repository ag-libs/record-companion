package io.github.recordcompanion.processor;

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
            .addParameter(updaterConsumerType, "u")
            .returns(recordTypeName)
            .addJavadoc(
                "Creates a new record instance by applying modifications to an existing record.\n")
            .addJavadoc("@param existing the existing record to base the new record on\n")
            .addJavadoc(
                "@param u a consumer that receives an updater initialized with the existing record's values\n")
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

    body.addStatement("u.accept(builder)");
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
   * Generates a short parameter name from a class name by taking first letters of camelCase words +
   * 'u'. Examples: Address -> au, PersonUpdater -> pu, MyType -> mtu
   */
  private String generateUpdaterParameterName(String className) {
    StringBuilder result = new StringBuilder();

    // Add first character (always lowercase)
    if (!className.isEmpty()) {
      result.append(Character.toLowerCase(className.charAt(0)));
    }

    // Add first letter of each subsequent camelCase word
    for (int i = 1; i < className.length(); i++) {
      if (Character.isUpperCase(className.charAt(i))) {
        result.append(Character.toLowerCase(className.charAt(i)));
      }
    }

    // Add 'u' suffix
    result.append('u');

    return result.toString();
  }
}
