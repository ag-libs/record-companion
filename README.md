# RecordCompanion

A Java annotation processing library that enhances Java records with builder pattern support through code generation.

## Features

- **@Builder Annotation**: Generates builder pattern implementations for Java records
- **Clean Code Generation**: Creates readable Builder classes and Updater interfaces with fluent APIs
- **Zero Runtime Dependencies**: Pure compile-time annotation processing
- **Java 17+ Support**: Built for modern Java with record support

## Usage

Add the `@Builder` annotation to your record classes:

```java
import io.github.recordcompanion.annotations.Builder;

@Builder
public record User(String name, int age, String email) {}
```

Use the generated builder:

```java
User user = UserBuilder.builder()
    .name("John Doe")
    .age(30)
    .email("john@example.com")
    .build();
```

## Project Structure

- `record-companion-annotations/` - Annotation definitions
- `record-companion-processor/` - Annotation processor implementation
- `record-companion-tests/` - Integration tests

## Building

```bash
mvn clean compile
mvn test
```

## Requirements

- Java 17+
- Maven 3.6+

