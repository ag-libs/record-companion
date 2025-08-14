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
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/** Generates validator classes for ValidCheck integration. */
public class ValidatorGenerator {

  private static final String GENERATOR_VALUE =
      "io.github.recordcompanion.processor.RecordBuilderProcessor";
  private static final String VALIDATOR_SUFFIX = "Validator";

  private static final ClassName VALIDCHECK_CHECK = ClassName.get("io.github.validcheck", "Check");
  private static final ClassName VALIDCHECK_BATCH_CONTEXT =
      ClassName.get("io.github.validcheck", "BatchValidationContext");
  private static final ClassName VALIDCHECK_VALUE_VALIDATOR =
      ClassName.get("io.github.validcheck", "ValueValidator");
  private static final ClassName VALIDCHECK_STRING_VALIDATOR =
      ClassName.get("io.github.validcheck", "StringValidator");
  private static final ClassName VALIDCHECK_NUMERIC_VALIDATOR =
      ClassName.get("io.github.validcheck", "NumericValidator");
  private static final ClassName VALIDCHECK_COLLECTION_VALIDATOR =
      ClassName.get("io.github.validcheck", "CollectionValidator");
  private static final ClassName VALIDCHECK_MAP_VALIDATOR =
      ClassName.get("io.github.validcheck", "MapValidator");
  private static final ClassName CONSUMER_TYPE = ClassName.get("java.util.function", "Consumer");

  private final ProcessingEnvironment processingEnv;

  public ValidatorGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv cannot be null");
  }

  public void generateValidator(TypeElement recordElement) throws IOException {
    String className = recordElement.getSimpleName() + VALIDATOR_SUFFIX;
    String packageName = processingEnv.getElementUtils().getPackageOf(recordElement).toString();

    List<? extends RecordComponentElement> components = recordElement.getRecordComponents();
    List<TypeVariableName> typeVariables =
        recordElement.getTypeParameters().stream()
            .map(TypeVariableName::get)
            .collect(Collectors.toList());

    TypeSpec.Builder validatorClass =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addTypeVariables(typeVariables)
            .addAnnotation(
                AnnotationSpec.builder(Generated.class)
                    .addMember("value", "$S", GENERATOR_VALUE)
                    .build());

    // Add validation field
    FieldSpec validationField =
        FieldSpec.builder(VALIDCHECK_BATCH_CONTEXT, "validation")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();
    validatorClass.addField(validationField);

    // Add private constructor
    MethodSpec constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addStatement("this.validation = $T.batch()", VALIDCHECK_CHECK)
            .build();
    validatorClass.addMethod(constructor);

    // Add static factory method
    TypeName returnType =
        typeVariables.isEmpty()
            ? ClassName.get(packageName, className)
            : ParameterizedTypeName.get(
                ClassName.get(packageName, className), typeVariables.toArray(new TypeName[0]));

    MethodSpec validatorMethod =
        MethodSpec.methodBuilder("validator")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariables(typeVariables)
            .returns(returnType)
            .addStatement("return new $T()", ClassName.get(packageName, className))
            .build();
    validatorClass.addMethod(validatorMethod);

    // Add validation methods for each component
    for (RecordComponentElement component : components) {
      MethodSpec validationMethod =
          createValidationMethod(component, className, packageName, typeVariables);
      validatorClass.addMethod(validationMethod);
    }

    // Add context() method
    MethodSpec contextMethod =
        MethodSpec.methodBuilder("context")
            .addModifiers(Modifier.PUBLIC)
            .returns(VALIDCHECK_BATCH_CONTEXT)
            .addStatement("return validation")
            .build();
    validatorClass.addMethod(contextMethod);

    // Add validate() method
    MethodSpec validateMethod =
        MethodSpec.methodBuilder("validate")
            .addModifiers(Modifier.PUBLIC)
            .addStatement("validation.validate()")
            .build();
    validatorClass.addMethod(validateMethod);

    JavaFile javaFile = JavaFile.builder(packageName, validatorClass.build()).indent("  ").build();

    javaFile.writeTo(processingEnv.getFiler());
  }

  private MethodSpec createValidationMethod(
      RecordComponentElement component,
      String validatorClassName,
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
            ? ClassName.get(packageName, validatorClassName)
            : ParameterizedTypeName.get(
                ClassName.get(packageName, validatorClassName),
                typeVariables.toArray(new TypeName[0]));

    return MethodSpec.methodBuilder(componentName)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(componentTypeName, componentName)
        .addParameter(consumerType, "validator")
        .returns(returnType)
        .addStatement("validator.accept(validation.check($L, $S))", componentName, componentName)
        .addStatement("return this")
        .build();
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

  private TypeName getCollectionElementType(TypeMirror typeMirror) {
    String typeName = typeMirror.toString();

    // Extract generic type from collection types like List<String>
    int startIndex = typeName.indexOf('<');
    int endIndex = typeName.lastIndexOf('>');

    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
      String elementTypeName = typeName.substring(startIndex + 1, endIndex);
      // Handle simple cases - for complex generics, this would need more sophisticated parsing
      return switch (elementTypeName) {
        case "java.lang.String" -> ClassName.get(String.class);
        case "java.lang.Integer" -> ClassName.get(Integer.class);
        case "java.lang.Long" -> ClassName.get(Long.class);
        case "java.lang.Double" -> ClassName.get(Double.class);
        case "java.lang.Boolean" -> ClassName.get(Boolean.class);
        default -> ClassName.get(Object.class); // Fallback for complex types
      };
    }

    // If no generic type found, use Object as wildcard
    return ClassName.get(Object.class);
  }
}
