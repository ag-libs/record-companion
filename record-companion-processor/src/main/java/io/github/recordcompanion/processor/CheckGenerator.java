package io.github.recordcompanion.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/** Generates check classes for ValidCheck integration. */
public class CheckGenerator {

  private static final String CHECK_SUFFIX = "Check";
  private static final String VALIDCHECK_PACKAGE = "io.github.validcheck";
  private static final String CHECK_METHOD_PREFIX = "check";

  private static final ClassName VALIDCHECK_CHECK = ClassName.get(VALIDCHECK_PACKAGE, CHECK_SUFFIX);
  private static final ClassName VALIDCHECK_BATCH_CONTEXT =
      ClassName.get(VALIDCHECK_PACKAGE, "BatchValidationContext");
  private static final ClassName VALIDCHECK_VALUE_VALIDATOR =
      ClassName.get(VALIDCHECK_PACKAGE, "ValueValidator");
  private static final ClassName VALIDCHECK_STRING_VALIDATOR =
      ClassName.get(VALIDCHECK_PACKAGE, "StringValidator");
  private static final ClassName VALIDCHECK_NUMERIC_VALIDATOR =
      ClassName.get(VALIDCHECK_PACKAGE, "NumericValidator");
  private static final ClassName VALIDCHECK_COLLECTION_VALIDATOR =
      ClassName.get(VALIDCHECK_PACKAGE, "CollectionValidator");
  private static final ClassName VALIDCHECK_MAP_VALIDATOR =
      ClassName.get(VALIDCHECK_PACKAGE, "MapValidator");
  private static final ClassName CONSUMER_TYPE = ClassName.get("java.util.function", "Consumer");

  private final ProcessingEnvironment processingEnv;

  public CheckGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv cannot be null");
  }

  public void generateCheck(TypeElement recordElement) throws IOException {
    String className = recordElement.getSimpleName() + CHECK_SUFFIX;
    String packageName = processingEnv.getElementUtils().getPackageOf(recordElement).toString();

    List<? extends RecordComponentElement> components = recordElement.getRecordComponents();
    List<TypeVariableName> typeVariables =
        recordElement.getTypeParameters().stream().map(TypeVariableName::get).toList();

    TypeSpec.Builder checkClass =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addTypeVariables(typeVariables)
            .addAnnotation(
                AnnotationSpec.builder(Generated.class)
                    .addMember("value", "$S", Constants.GENERATOR_VALUE)
                    .build());

    // Add validation field
    FieldSpec validationField =
        FieldSpec.builder(VALIDCHECK_BATCH_CONTEXT, "validation")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();
    checkClass.addField(validationField);

    // Add private constructor
    MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addStatement("this.validation = $T.batch()", VALIDCHECK_CHECK)
            .build();
    checkClass.addMethod(constructor);

    // Add static factory method
    TypeName returnType =
        typeVariables.isEmpty()
            ? ClassName.get(packageName, className)
            : ParameterizedTypeName.get(
                ClassName.get(packageName, className), typeVariables.toArray(new TypeName[0]));

    MethodSpec checkMethod =
        MethodSpec.methodBuilder(CHECK_METHOD_PREFIX)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(typeVariables)
            .returns(returnType)
            .addStatement(
                "return new $T" + (typeVariables.isEmpty() ? "()" : "<>()"),
                ClassName.get(packageName, className))
            .build();
    checkClass.addMethod(checkMethod);

    // Add validation methods for each component
    for (RecordComponentElement component : components) {
      MethodSpec validationMethod =
          createValidationMethod(component, className, packageName, typeVariables);
      checkClass.addMethod(validationMethod);

      // Add static field-specific validation method (skip for generic types)
      MethodSpec staticValidationMethod = createStaticValidationMethod(component);
      if (staticValidationMethod != null) {
        checkClass.addMethod(staticValidationMethod);
      }
    }

    // Add context() method
    MethodSpec contextMethod =
        MethodSpec.methodBuilder("context")
            .addModifiers(Modifier.PUBLIC)
            .returns(VALIDCHECK_BATCH_CONTEXT)
            .addStatement("return validation")
            .build();
    checkClass.addMethod(contextMethod);

    // Add validate() method
    MethodSpec validateMethod =
        MethodSpec.methodBuilder("validate")
            .addModifiers(Modifier.PUBLIC)
            .addStatement("validation.validate()")
            .build();
    checkClass.addMethod(validateMethod);

    JavaFile javaFile = JavaFile.builder(packageName, checkClass.build()).indent("  ").build();

    javaFile.writeTo(processingEnv.getFiler());
  }

  private MethodSpec createValidationMethod(
      RecordComponentElement component,
      String checkClassName,
      String packageName,
      List<TypeVariableName> typeVariables) {
    String componentName = component.getSimpleName().toString();
    TypeMirror componentType = component.asType();
    TypeName componentTypeName = TypeName.get(componentType);

    // Use ValueValidator<T> for all types
    TypeName validatorType = getValidatorType(componentType);
    TypeName consumerType = ParameterizedTypeName.get(CONSUMER_TYPE, validatorType);

    TypeName returnType =
        typeVariables.isEmpty()
            ? ClassName.get(packageName, checkClassName)
            : ParameterizedTypeName.get(
                ClassName.get(packageName, checkClassName), typeVariables.toArray(new TypeName[0]));

    return MethodSpec.methodBuilder(CHECK_METHOD_PREFIX + capitalize(componentName))
        .addModifiers(Modifier.PUBLIC)
        .addParameter(componentTypeName, componentName)
        .addParameter(consumerType, "validator")
        .returns(returnType)
        .addStatement("validator.accept(validation.check($L, $S))", componentName, componentName)
        .addStatement("return this")
        .build();
  }

  private MethodSpec createStaticValidationMethod(RecordComponentElement component) {
    String componentName = component.getSimpleName().toString();
    TypeMirror componentType = component.asType();
    TypeName componentTypeName = TypeName.get(componentType);

    // Skip static methods for generic types to avoid "non-static type variable T cannot be
    // referenced from a static context"
    if (containsTypeVariable(componentTypeName)) {
      return null; // Will be filtered out
    }

    TypeName validatorType = getValidatorType(componentType);

    return MethodSpec.methodBuilder(CHECK_METHOD_PREFIX + capitalize(componentName))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(componentTypeName, componentName)
        .returns(validatorType)
        .addStatement("return $T.check($L)", VALIDCHECK_CHECK, componentName)
        .build();
  }

  private boolean containsTypeVariable(TypeName typeName) {
    if (typeName instanceof TypeVariableName) {
      return true;
    }
    if (typeName instanceof ParameterizedTypeName parameterizedType) {
      return parameterizedType.typeArguments.stream().anyMatch(this::containsTypeVariable);
    }
    return false;
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  private TypeName getValidatorType(TypeMirror componentType) {
    String typeName = componentType.toString();

    // Check for String type
    if ("java.lang.String".equals(typeName)) {
      return VALIDCHECK_STRING_VALIDATOR;
    }

    // Check for numeric types
    if (isNumericType(componentType)) {
      TypeName boxedType = getBoxedType(componentType);
      return ParameterizedTypeName.get(VALIDCHECK_NUMERIC_VALIDATOR, boxedType);
    }

    // Check for collection types
    if (isCollectionType(componentType)) {
      // Use the full collection type, not just the element type
      TypeName collectionType = TypeName.get(componentType);
      return ParameterizedTypeName.get(VALIDCHECK_COLLECTION_VALIDATOR, collectionType);
    }

    // Check for map types
    if (isMapType(componentType)) {
      // Use the full map type
      TypeName mapType = TypeName.get(componentType);
      return ParameterizedTypeName.get(VALIDCHECK_MAP_VALIDATOR, mapType);
    }

    // Default to ValueValidator<T> for other types
    TypeName boxedType = getBoxedType(componentType);
    return ParameterizedTypeName.get(VALIDCHECK_VALUE_VALIDATOR, boxedType);
  }

  private TypeName getBoxedType(TypeMirror typeMirror) {
    // Box primitive types for generics
    if (typeMirror.getKind().isPrimitive()) {
      return switch (typeMirror.getKind()) {
        case BOOLEAN -> ClassName.get(Boolean.class);
        case BYTE -> ClassName.get(Byte.class);
        case SHORT -> ClassName.get(Short.class);
        case INT -> ClassName.get(Integer.class);
        case LONG -> ClassName.get(Long.class);
        case CHAR -> ClassName.get(Character.class);
        case FLOAT -> ClassName.get(Float.class);
        case DOUBLE -> ClassName.get(Double.class);
        default -> TypeName.get(typeMirror);
      };
    }
    return TypeName.get(typeMirror);
  }

  private boolean isNumericType(TypeMirror typeMirror) {
    if (typeMirror.getKind().isPrimitive()) {
      return switch (typeMirror.getKind()) {
        case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true;
        default -> false;
      };
    }

    String typeName = typeMirror.toString();
    return "java.lang.Byte".equals(typeName)
        || "java.lang.Short".equals(typeName)
        || "java.lang.Integer".equals(typeName)
        || "java.lang.Long".equals(typeName)
        || "java.lang.Float".equals(typeName)
        || "java.lang.Double".equals(typeName)
        || "java.math.BigInteger".equals(typeName)
        || "java.math.BigDecimal".equals(typeName);
  }

  private boolean isCollectionType(TypeMirror typeMirror) {
    String typeName = typeMirror.toString();

    // Check for common collection types and their generic variants
    return typeName.startsWith("java.util.List<")
        || typeName.startsWith("java.util.Set<")
        || typeName.startsWith("java.util.Collection<")
        || typeName.startsWith("java.util.ArrayList<")
        || typeName.startsWith("java.util.LinkedList<")
        || typeName.startsWith("java.util.HashSet<")
        || typeName.startsWith("java.util.TreeSet<")
        || typeName.equals("java.util.List")
        || typeName.equals("java.util.Set")
        || typeName.equals("java.util.Collection");
  }

  private boolean isMapType(TypeMirror typeMirror) {
    String typeName = typeMirror.toString();

    // Check for common map types and their generic variants
    return typeName.startsWith("java.util.Map<")
        || typeName.startsWith("java.util.HashMap<")
        || typeName.startsWith("java.util.LinkedHashMap<")
        || typeName.startsWith("java.util.TreeMap<")
        || typeName.startsWith("java.util.ConcurrentHashMap<")
        || typeName.equals("java.util.Map")
        || typeName.equals("java.util.HashMap")
        || typeName.equals("java.util.LinkedHashMap")
        || typeName.equals("java.util.TreeMap")
        || typeName.equals("java.util.ConcurrentHashMap");
  }
}
