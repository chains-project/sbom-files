package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WriteDataJson {
  public static void main(String[] args) throws IOException {
   ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(Path.of("./data.json").toFile(), new CreateDataTree().createData(Path.of("./results")));
  }
}
