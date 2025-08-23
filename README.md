# Record Companion

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=record-companion_record-companion&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=record-companion_record-companion)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=record-companion_record-companion&metric=coverage)](https://sonarcloud.io/summary/new_code?id=record-companion_record-companion)

A simple Java library that enhances Java records with builder pattern support and
Bean Validation integration through compile-time code generation.

## What Record Companion Provides

- Generate fluent builder classes for Java records with a single annotation
- Optional annotation copying from records to generated builders
- **Bean Validation integration** - Automatic mapping from Bean Validation annotations to ValidCheck API
- Built-in support for `@NotNull`, `@NotEmpty`, `@Size`, `@Pattern`, `@Min/@Max` combinations, `@Positive`, `@Negative`, `@PositiveOrZero`, and `@NegativeOrZero`
- Works with any IDE without special plugins
- No runtime dependencies required for builders (ValidCheck integration requires ValidCheck library)

## Quick Start

### 1. Add Dependency

**Maven:**

```xml
<!-- Record Companion Processor -->
<dependency>
  <groupId>io.github.record-companion</groupId>
  <artifactId>record-companion-processor</artifactId>
  <version>0.1.1</version>
  <scope>provided</scope>
</dependency>

<!-- ValidCheck library (required only for @ValidCheck integration) -->
<dependency>
  <groupId>io.github.validcheck</groupId>
  <artifactId>validcheck</artifactId>
  <version>0.1.1</version>
</dependency>

<!-- Bean Validation API (required only for @ValidCheck integration) -->
<dependency>
  <groupId>javax.validation</groupId>
  <artifactId>validation-api</artifactId>
  <version>0.1.1</version>
  <scope>provided</scope>
</dependency>
```

**Gradle:**

```gradle
// Record Companion Processor  
annotationProcessor 'io.github.record-companion:record-companion-processor:0.1.1'
compileOnly 'io.github.record-companion:record-companion-processor:0.1.1'

// ValidCheck library (required only for @ValidCheck integration)
implementation 'io.github.validcheck:validcheck:0.9.3'

// Bean Validation API (required only for @ValidCheck integration) 
compileOnly 'javax.validation:validation-api:2.0.1.Final'
```

### 2. Annotate Your Records

```java
import io.github.recordcompanion.annotations.Builder;

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

Use `@ValidCheck` with Bean Validation annotations for automatic validation code generation
with [ValidCheck library](https://github.com/validcheck/validcheck):

```java
@Builder
@ValidCheck
public record UserProfile(
    @NotNull @Size(min = 3, max = 20) String username,
    @Min(0) @Max(100) int score,
    @NotEmpty Map<String, String> metadata,
    @Pattern(regexp = "[A-Z]{3}[0-9]{3}") String code,
    @Positive int positiveAmount,
    @NegativeOrZero int balance) {

  public UserProfile {
    // Automatic validation using generated check class
    UserProfileCheck.validate(username, score, metadata, code, positiveAmount, balance);
  }
}
```

**Supported Bean Validation Annotations:**

- `@NotNull` → `.notNull(value, fieldName)`
- `@NotEmpty` → `.notNullOrEmpty(value, fieldName)`
- `@Size(min, max)` → `.hasLength(value, min, max, fieldName)`
- `@Pattern(regexp)` → `.matches(value, pattern, fieldName)`
- `@Min + @Max` (combined) → `.inRange(value, min, max, fieldName)`
- `@Positive` → `.inRange(value, 1, Integer.MAX_VALUE, fieldName)`
- `@Negative` → `.inRange(value, Integer.MIN_VALUE, -1, fieldName)`
- `@PositiveOrZero` → `.inRange(value, 0, Integer.MAX_VALUE, fieldName)`
- `@NegativeOrZero` → `.inRange(value, Integer.MIN_VALUE, 0, fieldName)`

**Generated Check Class Methods:**

```java
// BatchValidator for manual control
UserProfileCheck.check(username, score, metadata, code, positiveAmount, balance)

// Validator for immediate validation with chaining  
UserProfileCheck.require(username, score, metadata, code, positiveAmount, balance)

// Convenience method - validates and throws on failure
UserProfileCheck.validate(username, score, metadata, code, positiveAmount, balance)
```

**Example Generated Validation Chain:**

```java
return validator
    .notNull(username, "username")
    .hasLength(username, 3, 20, "username")
    .inRange(score, 0, 100, "score") 
    .notNullOrEmpty(metadata, "metadata")
    .matches(code, "[A-Z]{3}[0-9]{3}", "code")
    .inRange(positiveAmount, 1, Integer.MAX_VALUE, "positiveAmount")
    .inRange(balance, Integer.MIN_VALUE, 0, "balance");
```

## Requirements

- **Java 17+** (for record support)
- **Maven 3.6+** or **Gradle 6.0+**
- **Any IDE** (no special plugins required)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

