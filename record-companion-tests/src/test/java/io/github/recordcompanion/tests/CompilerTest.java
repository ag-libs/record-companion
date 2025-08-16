package io.github.recordcompanion.tests;

import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.JavaFileObjects;
import io.github.recordcompanion.processor.RecordCompanionProcessor;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.tools.JavaFileObject.Kind;
import org.junit.jupiter.api.Test;

public class CompilerTest {
  @Test
  void all() throws IOException {
    var records = Files.list(Path.of("src/test/java/io/github/recordcompanion/tests/records"));

    var sources =
        Stream.concat(Stream.of(), records)
            .map(
                p -> {
                  try {
                    return JavaFileObjects.forResource(p.toUri().toURL());
                  } catch (MalformedURLException e) {
                    throw new IllegalStateException(e);
                  }
                })
            .filter(j -> j.getKind() == Kind.SOURCE);
    javac().withProcessors(new RecordCompanionProcessor()).compile(sources.toList());
  }
}
