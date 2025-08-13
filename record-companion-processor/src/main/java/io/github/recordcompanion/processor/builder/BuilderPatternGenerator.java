package io.github.recordcompanion.processor.builder;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.recordcompanion.annotations.Builder;
import javax.annotation.processing.Generated;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates builder pattern classes (Updater interface and Builder implementation).
 */
public class BuilderPatternGenerator {

  /**
   * Generates the Updater interface.
   */
  public TypeSpec generateUpdaterInterface(List<? extends RecordComponentElement> components, 
                                           List<TypeVariableName> typeVariableNames) {
    TypeSpec.Builder updaterBuilder = createUpdaterInterfaceBuilder(typeVariableNames);
    TypeName updaterInterfaceType = createUpdaterInterfaceType(typeVariableNames);

    addSetterMethodsToUpdaterInterface(updaterBuilder, components, updaterInterfaceType);

    return updaterBuilder.build();
  }

  /**
   * Generates the Builder inner class.
   */
  public TypeSpec generateBuilderClass(TypeName recordTypeName,
                                       List<? extends RecordComponentElement> components,
                                       List<TypeVariableName> typeVariableNames) {

    TypeName updaterInterfaceType = createUpdaterInterfaceType(typeVariableNames);
    TypeName builderClassType = createBuilderClassType(typeVariableNames);

    TypeSpec.Builder builderBuilder = createBuilderClassBuilder(updaterInterfaceType, typeVariableNames);
    addFieldsToBuilderClass(builderBuilder, components);
    addSetterMethodsToBuilderClass(builderBuilder, components, builderClassType);
    addBuildMethodToBuilderClass(builderBuilder, recordTypeName, components);

    return builderBuilder.build();
  }

  private TypeSpec.Builder createUpdaterInterfaceBuilder(List<TypeVariableName> typeVariableNames) {
    TypeSpec.Builder updaterBuilder = TypeSpec.interfaceBuilder("Updater")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addAnnotation(
            AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", "io.github.recordcompanion.processor.RecordCompanionProcessor")
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

  private void addSetterMethodsToUpdaterInterface(TypeSpec.Builder updaterBuilder,
                                                  List<? extends RecordComponentElement> components,
                                                  TypeName updaterInterfaceType) {
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      TypeMirror componentType = component.asType();

      // Always generate the original setter method
      MethodSpec setterMethod = MethodSpec.methodBuilder(componentName)
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

  private TypeName createBuilderClassType(List<TypeVariableName> typeVariableNames) {
    return typeVariableNames.isEmpty()
        ? ClassName.bestGuess("Builder")
        : ParameterizedTypeName.get(
            ClassName.bestGuess("Builder"), typeVariableNames.toArray(new TypeName[0]));
  }

  private TypeSpec.Builder createBuilderClassBuilder(TypeName updaterInterfaceType, 
                                                     List<TypeVariableName> typeVariableNames) {
    TypeSpec.Builder builderBuilder = TypeSpec.classBuilder("Builder")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addAnnotation(
            AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", "io.github.recordcompanion.processor.RecordCompanionProcessor")
                .build())
        .addSuperinterface(updaterInterfaceType);

    // Add type parameters to the builder class
    for (TypeVariableName typeVariableName : typeVariableNames) {
      builderBuilder.addTypeVariable(typeVariableName);
    }

    return builderBuilder;
  }

  private void addFieldsToBuilderClass(TypeSpec.Builder builderBuilder, 
                                       List<? extends RecordComponentElement> components) {
    for (RecordComponentElement component : components) {
      builderBuilder.addField(
          TypeName.get(component.asType()), component.getSimpleName().toString(), Modifier.PRIVATE);
    }
  }

  private void addSetterMethodsToBuilderClass(TypeSpec.Builder builderBuilder,
                                              List<? extends RecordComponentElement> components,
                                              TypeName builderClassType) {
    for (RecordComponentElement component : components) {
      String componentName = component.getSimpleName().toString();
      TypeMirror componentType = component.asType();

      // Always generate the original setter method
      MethodSpec setterMethod = MethodSpec.methodBuilder(componentName)
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

  private void addBuildMethodToBuilderClass(TypeSpec.Builder builderBuilder,
                                            TypeName recordTypeName,
                                            List<? extends RecordComponentElement> components) {
    CodeBlock.Builder buildMethodBody = CodeBlock.builder();
    buildMethodBody.add("return new $T(", recordTypeName);

    for (int i = 0; i < components.size(); i++) {
      if (i > 0) buildMethodBody.add(", ");
      buildMethodBody.add("$N", components.get(i).getSimpleName().toString());
    }
    buildMethodBody.add(")");

    MethodSpec buildMethod = MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(recordTypeName)
        .addStatement(buildMethodBody.build())
        .build();

    builderBuilder.addMethod(buildMethod);
  }

  // Helper methods for nested record handling
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

  private MethodSpec createNestedSetterMethod(RecordComponentElement component, 
                                              TypeName updaterInterfaceType, 
                                              boolean isInterface) {
    // This would need ProcessingEnvironment and other dependencies
    // For now, returning a placeholder - this should be properly implemented
    // with dependency injection or by passing required parameters
    throw new UnsupportedOperationException("Nested setter method generation needs refactoring");
  }
}