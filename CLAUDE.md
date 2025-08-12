# Claude Code Instructions

## File Formatting Guidelines

### Always add a newline at end of files

- All text files (Java, XML, properties, markdown, etc.) should end with a newline character
- This follows POSIX standards and prevents git warnings
- Ensures proper file concatenation and tool compatibility

### Java Code Formatting

- Format all new Java code using Google Java Style Guide
- Use 2-space indentation (not 4 spaces or tabs)
- Line length limit: 100 characters
- Use Google Java formatter for consistent styling

### Example:

```java
public class Example {
  // code here with 2-space indentation
}
// <- newline should be here
```

## Project-Specific Guidelines

### Build and Test Commands

- To build: `mvn clean compile`
- To run tests: `mvn test`
- To clean: `mvn clean`

### Module Structure

- `record-companion-annotations/` - Contains annotation definitions
- `record-companion-processor/` - Contains annotation processor implementation
- `record-companion-tests/` - Contains integration tests

