package io.github.aglibs.recordcompanion.builder.internal;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.aglibs.recordcompanion.builder.Builder;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/** Generates builder pattern implementations for record classes. */
public class BuilderGenerator {

  private static final String BUILDER_SUFFIX = "Builder";
  private static final String UPDATER_SUFFIX = "Updater";
  private static final ClassName CONSUMER_TYPE = ClassName.get("java.util.function", "Consumer");

  private final ProcessingEnvironment processingEnv;

  public BuilderGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv cannot be null");
  }

  private String joinTypeParameters(List<TypeVariableName> typeVariableNames) {
    return typeVariableNames.stream()
        .map(TypeVariableName::toString)
        .collect(Collectors.joining(", "));
  }

  /** Creates a parameterized TypeName from a base class and type variables. */
  private TypeName createParameterizedTypeOrSimple(
      ClassName baseClass, List<TypeVariableName> typeVariableNames) {
    return typeVariableNames.isEmpty()
        ? baseClass
        : ParameterizedTypeName.get(baseClass, typeVariableNames.toArray(new TypeName[0]));
  }

  private String generateUpdaterParameterName(TypeElement recordElement) {
    return generateUpdaterParameterName(recordElement.getSimpleName().toString());
  }

  private TypeName createNestedUpdaterType(
      DeclaredType declaredType, ClassName nestedUpdaterClass) {
    List<? extends TypeMirror> nestedTypeArguments = declaredType.getTypeArguments();
    if (nestedTypeArguments.isEmpty()) {
      return nestedUpdaterClass;
    }
    TypeName[] nestedTypeNames =
        nestedTypeArguments.stream().map(TypeName::get).toArray(TypeName[]::new);
    return ParameterizedTypeName.get(nestedUpdaterClass, nestedTypeNames);
  }

  private MethodSpec createNestedSetterMethod(
      RecordComponentElement component, TypeName updaterInterfaceType, boolean isInterface) {
    DeclaredType declaredType = (DeclaredType) component.asType();
    TypeElement nestedRecordElement = (TypeElement) declaredType.asElement();
    ClassName nestedUpdaterClass = getUpdaterClassName(nestedRecordElement);
    String updaterParamName = generateUpdaterParameterName(nestedRecordElement);
    String componentName = component.getSimpleName().toString();

    TypeName nestedUpdaterType = createNestedUpdaterType(declaredType, nestedUpdaterClass);
    ParameterizedTypeName nestedUpdaterConsumerType =
        ParameterizedTypeName.get(CONSUMER_TYPE, nestedUpdaterType);

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
                  getBuilderClassName(nestedRecordElement),
                  updaterParamName,
                  TypeName.get(component.asType())));
    }

    return methodBuilder.build();
  }

  /**
   * Generates separate builder class and updater interface for the given record.
   *
   * @param recordElement the record element to generate builder and updater for
   * @throws IOException if there's an error writing the generated files
   * @throws NullPointerException if recordElement is null
   */
  public void generateBuilderAndUpdaterTypes(TypeElement recordElement) throws IOException {
    Objects.requireNonNull(recordElement, "recordElement cannot be null");
    String recordName = recordElement.getSimpleName().toString();
    String packageName =
        processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();

    // Extract copyAnnotations parameter from @Builder annotation
    Builder builderAnnotation = recordElement.getAnnotation(Builder.class);
    boolean copyAnnotations = builderAnnotation != null && builderAnnotation.copyAnnotations();

    // Extract type parameters from the record
    List<? extends TypeParameterElement> typeParameters = recordElement.getTypeParameters();
    List<TypeVariableName> typeVariableNames =
        typeParameters.stream()
            .map(
                tp ->
                    copyAnnotations
                        ? createTypeVariableNameWithAnnotations(tp)
                        : TypeVariableName.get(tp))
            .toList();

    ClassName recordClass = ClassName.get(packageName, recordName);

    // Create parameterized types if the record has type parameters
    TypeName recordTypeName =
        typeVariableNames.isEmpty()
            ? recordClass
            : ParameterizedTypeName.get(recordClass, typeVariableNames.toArray(new TypeName[0]));
    List<? extends RecordComponentElement> components = recordElement.getRecordComponents();

    // Generate the XxxUpdater interface
    generateStandaloneUpdaterInterface(recordName, packageName, components, typeVariableNames);

    // Generate the XxxBuilder class (implements XxxUpdater)
    generateStandaloneBuilderClass(
        recordElement,
        recordName,
        packageName,
        recordTypeName,
        components,
        typeVariableNames,
        copyAnnotations);
  }

  /** Generates a standalone XxxUpdater interface file. */
  private void generateStandaloneUpdaterInterface(
      String recordName,
      String packageName,
      List<? extends RecordComponentElement> components,
      List<TypeVariableName> typeVariableNames)
      throws IOException {

    String updaterName = recordName + UPDATER_SUFFIX;

    TypeSpec.Builder updaterBuilder =
        TypeSpec.interfaceBuilder(updaterName)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Interface for updating $N record values.\n", recordName);

    // Add type parameters to the interface
    for (TypeVariableName typeVariableName : typeVariableNames) {
      updaterBuilder.addTypeVariable(typeVariableName);
    }

    // Create the updater interface type for return types
    TypeName updaterInterfaceType =
        createParameterizedTypeOrSimple(ClassName.get(packageName, updaterName), typeVariableNames);

    // Add setter methods to updater interface
    addSetterMethodsToUpdaterInterface(updaterBuilder, components, updaterInterfaceType);

    TypeSpec updaterInterface = updaterBuilder.build();

    // Write the updater interface to a file
    JavaFile javaFile =
        JavaFile.builder(packageName, updaterInterface).skipJavaLangImports(true).build();
    javaFile.writeTo(processingEnv.getFiler());
  }

  /** Generates a standalone XxxBuilder class file. */
  private void generateStandaloneBuilderClass(
      TypeElement recordElement,
      String recordName,
      String packageName,
      TypeName recordTypeName,
      List<? extends RecordComponentElement> components,
      List<TypeVariableName> typeVariableNames,
      boolean copyAnnotations)
      throws IOException {

    String builderName = recordName + BUILDER_SUFFIX;
    String updaterName = recordName + UPDATER_SUFFIX;

    ClassName builderClass = ClassName.get(packageName, builderName);
    ClassName updaterInterface = ClassName.get(packageName, updaterName);

    // Create the updater interface type for implements clause
    TypeName updaterInterfaceType =
        createParameterizedTypeOrSimple(updaterInterface, typeVariableNames);

    // Create the builder class type for return types
    TypeName builderClassType = createParameterizedTypeOrSimple(builderClass, typeVariableNames);

    TypeSpec.Builder builderBuilder =
        TypeSpec.classBuilder(builderName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(updaterInterfaceType)
            .addJavadoc("Builder class for {@link $T} record.\n", recordTypeName);

    // Copy annotations from record to builder class if copyAnnotations is true
    if (copyAnnotations) {
      addCopiedAnnotationsToBuilder(builderBuilder, recordElement);
    }

    // Add type parameters to the builder class
    for (TypeVariableName typeVariableName : typeVariableNames) {
      builderBuilder.addTypeVariable(typeVariableName);
    }

    // Add fields to builder class
    addFieldsToBuilderClass(builderBuilder, components);

    // Add setter methods to builder class (returns builder type)
    addSetterMethodsToBuilderClass(builderBuilder, components, builderClassType, copyAnnotations);

    // Add build method to builder class
    addBuildMethodToBuilderClass(builderBuilder, recordTypeName, components);

    // Add static factory methods
    builderBuilder.addMethod(generateStaticBuilderMethod(builderClass, typeVariableNames));
    builderBuilder.addMethod(
        generateStaticBuilderWithExistingMethod(
            recordTypeName, builderClass, components, typeVariableNames));
    builderBuilder.addMethod(
        generateStaticWithMethod(recordTypeName, updaterInterface, typeVariableNames));

    TypeSpec builder = builderBuilder.build();

    // Write the builder class to a file
    JavaFile javaFile = JavaFile.builder(packageName, builder).skipJavaLangImports(true).build();
    javaFile.writeTo(processingEnv.getFiler());
  }

  private MethodSpec generateStaticBuilderMethod(
      ClassName builderClass, List<TypeVariableName> typeVariableNames) {

    // Create the Builder return type with proper type parameters
    TypeName builderReturnType = createParameterizedTypeOrSimple(builderClass, typeVariableNames);

    String constructorCall = typeVariableNames.isEmpty() ? "()" : "<>()";
    String builderConstructor = "return new " + builderClass.simpleName() + constructorCall;

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

  private MethodSpec generateStaticBuilderWithExistingMethod(
      TypeName recordTypeName,
      ClassName builderClass,
      List<? extends RecordComponentElement> components,
      List<TypeVariableName> typeVariableNames) {

    // Create the Builder return type with proper type parameters
    TypeName builderReturnType = createParameterizedTypeOrSimple(builderClass, typeVariableNames);

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(recordTypeName, "existing")
            .returns(builderReturnType)
            .addJavadoc(
                "Creates a new builder instance initialized with values from an existing record.\n")
            .addJavadoc("@param existing the existing record to copy values from\n")
            .addJavadoc("@return a new builder instance with copied values\n")
            .addCode(
                generateStaticBuilderWithExistingBody(builderClass, components, typeVariableNames));

    // Add type parameters to the method
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    return methodBuilder.build();
  }

  private CodeBlock generateStaticBuilderWithExistingBody(
      ClassName builderClass,
      List<? extends RecordComponentElement> components,
      List<TypeVariableName> typeVariableNames) {
    CodeBlock.Builder body = CodeBlock.builder();

    // Generate Builder type with proper type parameters
    if (typeVariableNames.isEmpty()) {
      body.addStatement("$T builder = new $T()", builderClass, builderClass);
    } else {
      String typeParams = joinTypeParameters(typeVariableNames);
      body.addStatement("$T<$L> builder = new $T<>()", builderClass, typeParams, builderClass);
    }

    // Copy all values from existing record
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      body.addStatement("builder.$N = existing.$N()", componentName, componentName);
    }

    body.addStatement("return builder");
    return body.build();
  }

  private MethodSpec generateStaticWithMethod(
      TypeName recordTypeName,
      ClassName updaterInterface,
      List<TypeVariableName> typeVariableNames) {
    // Create the Updater type with proper type parameters for the Consumer
    TypeName updaterType = createParameterizedTypeOrSimple(updaterInterface, typeVariableNames);
    ParameterizedTypeName updaterConsumerType =
        ParameterizedTypeName.get(CONSUMER_TYPE, updaterType);

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
            .addCode(generateStaticWithMethodBody());

    // Add type parameters to the method
    for (TypeVariableName typeVariableName : typeVariableNames) {
      methodBuilder.addTypeVariable(typeVariableName);
    }

    return methodBuilder.build();
  }

  private CodeBlock generateStaticWithMethodBody() {
    CodeBlock.Builder body = CodeBlock.builder();

    // Generate Builder type with proper type parameters
    body.addStatement("var builder = builder(existing)");
    body.addStatement("updater.accept(builder)");
    body.addStatement("return builder.build()");

    return body.build();
  }

  /** Adds setter methods to the updater interface. */
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

  /** Adds fields to the builder class. */
  private void addFieldsToBuilderClass(
      TypeSpec.Builder builderBuilder, List<? extends RecordComponentElement> components) {
    for (RecordComponentElement component : components) {
      builderBuilder.addField(
          TypeName.get(component.asType()), component.getSimpleName().toString(), Modifier.PRIVATE);
    }
  }

  /** Adds setter methods to the builder class. */
  private void addSetterMethodsToBuilderClass(
      TypeSpec.Builder builderBuilder,
      List<? extends RecordComponentElement> components,
      TypeName builderClassType,
      boolean copyAnnotations) {
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      TypeMirror componentType = component.asType();

      // Always generate the original setter method
      TypeName parameterType =
          copyAnnotations
              ? createTypeNameWithAnnotations(componentType)
              : TypeName.get(componentType);

      MethodSpec.Builder setterMethodBuilder =
          MethodSpec.methodBuilder(componentName)
              .addModifiers(Modifier.PUBLIC)
              .addParameter(parameterType, componentName)
              .returns(builderClassType)
              .addStatement("this.$N = $N", componentName, componentName)
              .addStatement("return this");

      // Copy annotations from record component to setter method if copyAnnotations is true
      if (copyAnnotations) {
        addCopiedAnnotationsToMethod(setterMethodBuilder, component);
      }

      MethodSpec setterMethod = setterMethodBuilder.build();

      builderBuilder.addMethod(setterMethod);

      // Generate overloaded method for @Builder-annotated record types
      if (isBuilderAnnotatedRecord(component)) {
        builderBuilder.addMethod(createNestedSetterMethod(component, builderClassType, false));
      }
    }
  }

  /** Adds build method to the builder class. */
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

  /** Generates the method body for nested record setter methods. */
  private CodeBlock generateNestedSetterBody(
      String componentName,
      ClassName nestedBuilderClass,
      String parameterName,
      TypeName componentTypeName) {
    CodeBlock.Builder body = CodeBlock.builder();

    // Update existing nested record or create new one if null
    body.beginControlFlow("if (this.$N != null)", componentName);
    body.addStatement(
        "this.$N = $T.with(this.$N, $N)",
        componentName,
        nestedBuilderClass,
        componentName,
        parameterName);
    body.nextControlFlow("else");
    // Create a new builder, apply the consumer, then build
    // Extract type arguments from the component type for proper generic support
    if (componentTypeName instanceof ParameterizedTypeName parameterizedType) {
      TypeName[] typeArguments = parameterizedType.typeArguments.toArray(new TypeName[0]);
      String typeArgsStr =
          Stream.of(typeArguments).map(TypeName::toString).collect(Collectors.joining(", "));
      body.addStatement("var builder = $T.<$L>builder()", nestedBuilderClass, typeArgsStr);
    } else {
      body.addStatement("var builder = $T.builder()", nestedBuilderClass);
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

  /** Gets the builder class name for a given record type. */
  private ClassName getBuilderClassName(TypeElement recordElement) {
    String packageName =
        processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();
    String recordName = recordElement.getSimpleName().toString();
    String builderName = recordName + BUILDER_SUFFIX;
    return ClassName.get(packageName, builderName);
  }

  /** Gets the updater interface name for a given record type. */
  private ClassName getUpdaterClassName(TypeElement recordElement) {
    String packageName =
        processingEnv.getElementUtils().getPackageOf(recordElement).getQualifiedName().toString();
    String recordName = recordElement.getSimpleName().toString();
    String updaterName = recordName + UPDATER_SUFFIX;
    return ClassName.get(packageName, updaterName);
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
    return Character.toLowerCase(className.charAt(0)) + className.substring(1) + UPDATER_SUFFIX;
  }

  /**
   * Adds copied annotations from the record element to the builder class. Excludes @Builder
   * annotation to avoid conflicts.
   */
  private void addCopiedAnnotationsToBuilder(
      TypeSpec.Builder builderBuilder, TypeElement recordElement) {
    for (AnnotationMirror annotation : recordElement.getAnnotationMirrors()) {
      // Skip @Builder annotation to avoid conflicts
      if (!annotation.getAnnotationType().toString().equals(Builder.class.getName())) {
        AnnotationSpec annotationSpec = AnnotationSpec.get(annotation);
        builderBuilder.addAnnotation(annotationSpec);
      }
    }
  }

  /** Adds copied annotations from the record component to the method. */
  private void addCopiedAnnotationsToMethod(
      MethodSpec.Builder methodBuilder, RecordComponentElement component) {
    for (AnnotationMirror annotation : component.getAnnotationMirrors()) {
      AnnotationSpec annotationSpec = AnnotationSpec.get(annotation);
      methodBuilder.addAnnotation(annotationSpec);
    }
  }

  /**
   * Creates a TypeVariableName with annotations preserved from the TypeParameterElement. This
   * handles type variable annotations like {@code @TypeVarAnnotation T}.
   */
  private TypeVariableName createTypeVariableNameWithAnnotations(
      TypeParameterElement typeParameter) {
    TypeVariableName baseTypeVar = TypeVariableName.get(typeParameter);

    // Copy annotations from the type parameter to the type variable name
    List<AnnotationSpec> annotations =
        typeParameter.getAnnotationMirrors().stream().map(AnnotationSpec::get).toList();

    if (annotations.isEmpty()) {
      return baseTypeVar;
    }

    // Create a new TypeVariableName with annotations
    return baseTypeVar.annotated(annotations);
  }

  /**
   * Creates a TypeName with type use annotations preserved from the TypeMirror. This handles type
   * use annotations within parameterized types like {@code List<@Ann String>}.
   */
  private TypeName createTypeNameWithAnnotations(TypeMirror typeMirror) {
    // JavaPoet's TypeName.get() should already preserve type use annotations
    // but we can enhance this if needed for specific cases
    return TypeName.get(typeMirror);
  }
}
