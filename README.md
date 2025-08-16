# Record Companion

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=record-companion_record-companion&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=record-companion_record-companion)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=record-companion_record-companion&metric=coverage)](https://sonarcloud.io/summary/new_code?id=record-companion_record-companion)

A simple Java library that enhances Java records with builder pattern support and optional
validation capabilities through compile-time code generation.

## What Record Companion Provides

- Generate fluent builder classes for Java records with a single annotation
- Optional annotation copying from records to generated builders
- Optional validation integration through `@ValidCheck` annotation
- Works with any IDE without special plugins
- No runtime dependencies required

## Quick Start

### 1. Add Dependency

**Maven:**

```xml

<dependency>
  <groupId>io.github.record-companion</groupId>
  <artifactId>record-companion-processor</artifactId>
  <version>1.0.0</version>
  <scope>provided</scope>
</dependency>
```

**Gradle:**

```gradle
annotationProcessor 'io.github.record-companion:record-companion-processor:1.0.0'
compileOnly 'io.github.record-companion:record-companion-processor:1.0.0'
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

### Optional Validation Integration

Use `@ValidCheck` for custom validation support
with [ValidCheck library](https://github.com/validcheck/validcheck):

```java

@Builder
@ValidCheck
public record Product(String name, BigDecimal price, int quantity) {

  public Product {
    // Your validation logic using generated helper
    ProductCheck.check()
        .checkName(name, StringValidator::notNullOrEmpty)
        .checkPrice(price, value -> value
            .notNull()
            .satisfies(p -> p.compareTo(BigDecimal.ZERO) > 0, "must be positive"))
        .checkQuantity(quantity, value -> value
            .isNonNegative()
            .max(1000))
        .validate();
  }
}
```

## Requirements

- **Java 17+** (for record support)
- **Maven 3.6+** or **Gradle 6.0+**
- **Any IDE** (no special plugins required)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

