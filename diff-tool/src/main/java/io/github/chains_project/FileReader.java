package io.github.chains_project;

import java.nio.file.Path;
import java.util.List;
import io.github.chains_project.data.Dependency;


public interface FileReader {

  List<Dependency> readFile(Path path);

}
