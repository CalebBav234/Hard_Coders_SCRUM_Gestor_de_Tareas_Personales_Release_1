package com.hardcoders.taskmanager.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba la dirección de dependencias de la constitución (puntos 1, 2 y 5) leyendo los imports
 * del código fuente: dominio y aplicación no dependen de frameworks ni de adaptadores.
 */
class HexagonalArchitectureTest {

    private static final Path ROOT = Path.of("src/main/java/com/hardcoders/taskmanager");

    private static final List<String> FORBIDDEN_IN_CORE = List.of(
            "import org.springframework", "import jakarta.", "import javax.persistence", "import org.hibernate",
            "import com.fasterxml", "import tools.jackson", "import lombok",
            "import com.hardcoders.taskmanager.adapter", "import com.hardcoders.taskmanager.configuration");

    @Test
    void domainIsFrameworkFreeAndDoesNotDependOnOuterLayers() throws IOException {
        assertNoImports(ROOT.resolve("domain"), concat(FORBIDDEN_IN_CORE,
                List.of("import com.hardcoders.taskmanager.application")));
    }

    @Test
    void applicationIsFrameworkFreeAndDoesNotDependOnAdapters() throws IOException {
        assertNoImports(ROOT.resolve("application"), FORBIDDEN_IN_CORE);
    }

    private static void assertNoImports(Path directory, List<String> forbidden) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> lines(path).stream()
                            .filter(line -> forbidden.stream().anyMatch(line::startsWith))
                            .map(line -> ROOT.relativize(path) + ": " + line))
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    private static List<String> lines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
