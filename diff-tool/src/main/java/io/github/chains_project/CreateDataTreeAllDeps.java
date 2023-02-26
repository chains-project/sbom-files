package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.chains_project.data.AnalyzerResult;
import io.github.chains_project.data.Dependency;
import io.github.chains_project.data.Result;

public class CreateDataTreeAllDeps {

  public static void main(String[] args) throws Exception {
    new CreateDataTreeAllDeps().createData(Path.of("./results"));
  }

  private static final List<String> analyzerNames = List.of("build-info-go", "cdxgen",
      "cyclonedx-maven-plugin", "depscan", "jbom", "openrewrite");

  public List<AnalyzerResult> createData(Path resultFolder) throws IOException {
    List<AnalyzerResult> results = new ArrayList<>();
    StringBuilder sb = new StringBuilder();

    for (Path project : Files.list(resultFolder).toArray(Path[]::new)) {
      Path truthJson = getMavenTruth(project);
      if (truthJson == null) {
        System.out.println("No truth for " + project.getFileName());
        continue;
      }
      for (String analyzerName : analyzerNames) {
        try {
          Path analyzerResult = project.resolve(analyzerName);
          if (!Files.exists(analyzerResult)) {
            addEmptyResult(results, project, analyzerName);
            continue;
          }
          String sbomType = SbomType.fileNameToType(analyzerResult.getFileName().toString());
          if (sbomType.isEmpty() || sbomType.equals("truth")) {
            continue;
          }

          Path file = Files.createTempFile("chains", ".json");
          Path inputFile = findJsonFile(analyzerResult);
          if (inputFile == null) {
            addEmptyResult(results, project, analyzerName);
            continue;
          }

          if (!PythonRunner.invokePython(inputFile, file, sbomType)) {
            addEmptyResult(results, project, analyzerName);
            continue;
          }

          FileReader jsonReader = new JsonReader();
          List<Dependency> input = jsonReader.readFile(file);
          List<Dependency> truth = jsonReader.readFile(truthJson);
          var directDependencyResult = calculateResult(input, truth);
          AnalyzerResult result = new AnalyzerResult(analyzerName, project.getFileName().toString(),
              directDependencyResult, directDependencyResult);
          results.add(result);
        } catch (Exception e) {
          // todo why does this Happen?
          sb.append("0,0,0,0,0,0,0,0,0,0,0,0,0,0").append(System.lineSeparator());
          e.printStackTrace();
        }
      }
    }
    return results;
  }

  private void addEmptyResult(List<AnalyzerResult> results, Path project, String analyzerName) {
    results.add(new AnalyzerResult(analyzerName, project.getFileName().toString(),
        Result.empty(), Result.empty()));
  }

  private List<Dependency> getDirectDeps(List<Dependency> truth) {
    return truth.stream().filter(this::isDirectDependency).toList();
  }

  private List<Dependency> getTransitiveDeps(List<Dependency> input) {
    return input.stream().filter(v -> v.getDepth() > 1).toList();
  }

  private Path getMavenTruth(Path project) throws IOException {
    return findJsonFile(Files.walk(project)
        .filter(v -> v.getFileName().toString().equals("maven-dependency-tree")).findAny().orElse(null));
  }


  private boolean isDirectDependency(Dependency v) {
    return v.getDepth() == 1;
  }


  record PrintOut(Result directDeps, Result transitiveDeps) {
  }

  record Metrics(int precision, int recall, int f1) {
  }

  /**
   * Calculates the true positives, all dependencies that are in both lists.
   * @param input The first list.
   * @param truth  The second list.
   * @return
   */
  private List<Dependency> truePositives(List<Dependency> input, List<Dependency> truth) {
    Set<Dependency> firstSet = new HashSet<>(input);
    Set<Dependency> secondSet = new HashSet<>(truth);
    var diffFirst = new HashSet<>(firstSet);
    diffFirst.retainAll(secondSet);
    return new ArrayList<>(diffFirst);
  }

  private List<Dependency> falsePositives(List<Dependency> input, List<Dependency> truth) {
    Set<Dependency> firstSet = new HashSet<>(input);
    Set<Dependency> secondSet = new HashSet<>(truth);
    var diffFirst = new HashSet<>(firstSet);
    diffFirst.removeAll(secondSet);
    return new ArrayList<>(diffFirst);
  }

  // All that are in the truth but not in the input.
  private List<Dependency> falseNegatives(List<Dependency> input, List<Dependency> truth) {
    var diffFirst = new HashSet<>(truth);
    diffFirst.removeAll(new HashSet<>(input));
    return new ArrayList<>(diffFirst);
  }



  private Path findJsonFile(Path folder) throws IOException {
    return Files.walk(folder).filter(v -> v.getFileName().toString().endsWith(".json")).findAny()
        .orElse(null);
  }

  private Result calculateResult(List<Dependency> input, List<Dependency> truth) {
    List<Dependency> truePositive = truePositives(input, truth);
    List<Dependency> falsePositive = falsePositives(input, truth);
    List<Dependency> falseNegative = falseNegatives(input, truth);
    return new Result(truePositive, falsePositive, falseNegative);
  }
}
