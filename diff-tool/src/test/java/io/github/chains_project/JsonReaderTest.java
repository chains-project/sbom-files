package io.github.chains_project;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.github.chains_project.data.Dependency;

 class JsonReaderTest {
  @Test
  void testReadFile() {
    Path path = Path.of("src/test/resources/SimpleJson.json");
    JsonReader jsonReader = new JsonReader();
    List<Dependency> result = jsonReader.readFile(path);
    assertTrue(!result.isEmpty());
    var dep = result.get(0);
    // assertTrue(dep.version().equals("1.6"));
  }
}
