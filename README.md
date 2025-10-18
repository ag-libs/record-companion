# Record Companion

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-libs_record-companion&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-libs-companion)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ag-libs_record-companion&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ag-libs-companion)

A simple library that generates builder pattern implementations for Java records at compile time. Just add one annotation to your record, and get a fluent builder automatically.

No runtime dependencies. No reflection. No magic. Just clean, generated code that works with any IDE.

## Getting Started

Add the dependency to your project:

**Maven:**

```xml
<dependency>
  <groupId>io.github.ag-libs.record-companion</groupId>
  <artifactId>record-companion-builder</artifactId>
  <version>0.1.5</version>
  <scope>provided</scope>
</dependency>
```

**Gradle:**

```gradle
annotationProcessor 'io.github.ag-libs.record-companion:record-companion-builder:0.1.5'
compileOnly 'io.github.ag-libs.record-companion:record-companion-builder:0.1.5'
```

Annotate your record with `@Builder`:

```java
import io.github.aglibs.recordcompanion.builder.Builder;

@Builder
public record User(String name, int age, String email) {}
```

That's it! The library generates a builder class at compile time.

## Usage

The generated builder gives you three ways to work with your records:

```java
// Create new instances with a fluent API
User user = UserBuilder.builder()
    .name("John Doe")
    .age(30)
    .email("john@example.com")
    .build();

// Copy and modify existing records
User updated = UserBuilder.builder(user)
    .age(31)
    .build();

// Functional updates
User modified = UserBuilder.with(user, builder ->
    builder.name("Jane Doe").email("jane@example.com")
);
```

## What Gets Generated

For each `@Builder` annotated record, you get two types:

**A concrete Builder class** with all the fluent methods:

```java
public final class UserBuilder implements UserUpdater {
  public UserBuilder name(String name) { ... }
  public UserBuilder age(int age) { ... }
  public UserBuilder email(String email) { ... }
  public User build() { ... }
  
  // Static factory methods
  public static UserBuilder builder() { ... }
  public static UserBuilder builder(User existing) { ... }
  public static User with(User existing, Consumer<UserUpdater> updater) { ... }
}
```

**An Updater interface** for flexible composition:

```java
public interface UserUpdater {
  UserUpdater name(String name);
  UserUpdater age(int age);
  UserUpdater email(String email);
}
```

The interface is useful when you want to pass update logic around without depending on the concrete builder class.

## Requirements

- Java 17 or later
- Maven 3.6+ or Gradle 6.0+

## Validation Support

If you need validation, there's an optional `@ValidCheck` annotation that integrates with the [ValidCheck library](https://github.com/validcheck/validcheck). It automatically maps standard Bean Validation annotations to ValidCheck API calls.

**Add the dependency:**

```xml
<dependency>
  <groupId>io.github.ag-libs.record-companion</groupId>
  <artifactId>record-companion-validcheck</artifactId>
  <version>0.1.5</version>
  <scope>provided</scope>
</dependency>
```

**Usage example:**

```java
@Builder
@ValidCheck
public record UserProfile(
    @NotNull @Size(min = 3, max = 20) String username,
    @Min(18) @Max(120) Integer age,
    @Pattern(regexp = "\\w+@\\w+\\.\\w+") String email) {

  public UserProfile {
    UserProfileCheck.validate(username, age, email);
  }
}
```

This generates validation methods that check your constraints and throw exceptions if validation fails. Fields without `@NotNull` are automatically treated as optional.

## Contributing

Contributions are welcome! Feel free to submit a pull request.

## License

Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

