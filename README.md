# Record Companion

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=record-companion_record-companion&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=record-companion_record-companion)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=record-companion_record-companion&metric=coverage)](https://sonarcloud.io/summary/new_code?id=record-companion_record-companion)

A simple Java library that enhances Java records with builder pattern support and
Bean Validation integration through compile-time code generation.

## What Record Companion Provides

- **Modular architecture** - Use only the modules you need (builder, validation, or both)
- Generate fluent builder classes for Java records with a single annotation
- Optional annotation copying from records to generated builders
- **Enhanced Bean Validation integration** - Automatic mapping from Bean Validation annotations to ValidCheck API
- Built-in support for `@NotNull`, `@NotEmpty`, `@NotBlank`, `@Size`, `@Pattern`, individual `@Min`/`@Max`, `@Min/@Max` combinations, `@Positive`, `@Negative`, `@PositiveOrZero`, and `@NegativeOrZero`
- **Null-safe validation** - Automatically uses `nullOr*` methods for optional fields without `@NotNull`
- Works with any IDE without special plugins
- No runtime dependencies required for builders (ValidCheck integration requires ValidCheck library)

## Quick Start

### 1. Add Dependency

Choose the modules you need:
- **record-companion-builder**: For `@Builder` annotation (builder pattern generation)  
- **record-companion-validcheck**: For `@ValidCheck` annotation (validation integration)

**Maven:**

```xml
<!-- Record Companion Builder -->
<dependency>
  <groupId>io.github.record-companion</groupId>
  <artifactId>record-companion-builder</artifactId>
  <version>0.1.3</version>
  <scope>provided</scope>
</dependency>

<!-- Record Companion ValidCheck (optional) -->
<dependency>
  <groupId>io.github.record-companion</groupId>
  <artifactId>record-companion-validcheck</artifactId>
  <version>0.1.3</version>
  <scope>provided</scope>
</dependency>
```

**Gradle:**

```gradle
// Record Companion Builder
annotationProcessor 'io.github.record-companion:record-companion-builder:0.1.2'
compileOnly 'io.github.record-companion:record-companion-builder:0.1.2'

// Record Companion ValidCheck (optional)
annotationProcessor 'io.github.record-companion:record-companion-validcheck:0.1.2'
compileOnly 'io.github.record-companion:record-companion-validcheck:0.1.2'
```

### 2. Annotate Your Records

```java
import io.github.recordcompanion.builder.Builder;

@Builder
public record User(String name, int age, String email) {}

```

### 3. Use the Generated Builder

```java
// Create new instances
User user = UserBuilder.builder()
        .name("John Doe")
        .age(30)
        .email("john@example.com")
        .build();

// Create from existing record
User updatedUser = UserBuilder.builder(user)
    .age(31)
    .build();

// Functional updates
User modifiedUser = UserBuilder.with(user, updater ->
    updater.name("Jane Doe").email("jane@example.com")
);
```

## Core Features

### Builder Pattern Support

Record Companion generates two main components for each annotated record:

**Builder Class:** A concrete implementation with fluent methods

```java
public final class UserBuilder implements UserUpdater {

  public UserBuilder name(String name) { ...}

  public UserBuilder age(int age) { ...}

  public UserBuilder email(String email) { ...}

  public User build() { ...}
}
```

**Updater Interface:** For flexible composition and functional updates

```java
public interface UserUpdater {

  UserUpdater name(String name);

  UserUpdater age(int age);

  UserUpdater email(String email);
}
```

### Static Factory Methods

Every generated builder provides convenient static methods:

```java
// Create a new builder
UserBuilder.builder()

// Create builder pre-populated with existing record
UserBuilder.builder(existingUser)

// Functional update pattern
UserBuilder.with(existingUser, updater -> updater.age(31))
```

### Bean Validation Integration

Add `@ValidCheck` to generate validation code using [ValidCheck library](https://github.com/validcheck/validcheck):

```java
import io.github.recordcompanion.builder.Builder;
import io.github.recordcompanion.validcheck.ValidCheck;
import javax.validation.constraints.*;

@Builder
@ValidCheck
public record UserProfile(
    @NotNull @NotBlank @Size(min = 3, max = 20) String username,
    @Min(0) @Max(100) Integer age,
    @Pattern(regexp = "\\w+@\\w+\\.\\w+") String email) {

  public UserProfile {
    UserProfileCheck.validate(username, age, email);
  }
}
```

**Supported Annotations:**
- **Null checks:** `@NotNull`, `@NotEmpty`, `@NotBlank`
- **Size/Length:** `@Size` (strings and collections)  
- **Patterns:** `@Pattern`
- **Numeric ranges:** `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`
- **Sign constraints:** `@Positive`, `@Negative`, `@PositiveOrZero`, `@NegativeOrZero`

Fields without `@NotNull` automatically use null-safe validation methods.

**Generated Methods:**

```java
UserProfileCheck.check(...)    // BatchValidator for manual control
UserProfileCheck.require(...)  // Validator for chaining
UserProfileCheck.validate(...) // Throws ValidationException on failure
```

## Requirements

- **Java 17+** (for record support)
- **Maven 3.6+** or **Gradle 6.0+**
- **Any IDE** (no special plugins required)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

