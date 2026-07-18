package io.github.chains_project;

import java.nio.file.Path;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.data.Dependency;

@ApplicationScoped
public class JsonReader implements FileReader {
  
  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public List<Dependency> readFile(Path path) {
    try {
      return mapper.readValue(path.toFile(), mapper.getTypeFactory().constructCollectionType(List.class, Dependency.class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
