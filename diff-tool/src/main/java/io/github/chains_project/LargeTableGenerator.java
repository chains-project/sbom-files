package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import io.github.chains_project.data.Dependency;

public class LargeTableGenerator {

  private static final int _1 = 1;

  public static void main(String[] args) throws Exception {
    new LargeTableGenerator().createData(Path.of("./results"));
  }

  private static final List<String> analyzerNames = List.of("build-info-go", "cdxgen",
      "cyclonedx-maven-plugin", "depscan", "jbom", "openrewrite");

  public void createData(Path resultFolder) throws IOException {
    Map<String, List<TableEntry>> map = new HashMap<>();
    for (Path project : Files.list(resultFolder).sorted().toArray(Path[]::new)) {
      Path truthJson = getMavenTruth(project);
      if (truthJson == null) {
        System.out.println("No truth file found for " + project.getFileName());
        continue;
      }
      for (String analyzerName : analyzerNames) {

        try {
          Path analyzerResult = project.resolve(analyzerName);
          if (!Files.exists(analyzerResult)) {
            var inner = new TableEntry(analyzerName, EMPTY_METRICS, EMPTY_METRICS);
            map.computeIfPresent(project.getFileName().toString(), (k, v) -> {
              v.add(inner);
              return v;
            });
            map.computeIfAbsent(project.getFileName().toString(), k -> {
              List<TableEntry> list = new ArrayList<>();
              list.add(inner);
              return list;
            });
            continue;
          }
          String sbomType = fileNameToType(analyzerResult.getFileName().toString());
          if (sbomType.isEmpty() || sbomType.equals("truth")) {
            continue;
          }
          System.out.println("Processing: " + project.getFileName() + " with "
              + analyzerResult.getFileName() + " (" + sbomType + ")");

          Path file = Files.createTempFile("chains", ".json");
          Path inputFile = findJsonFile(analyzerResult);

          if (inputFile == null) {
            var inner = new TableEntry(analyzerName, EMPTY_METRICS, EMPTY_METRICS);
            map.computeIfPresent(project.getFileName().toString(), (k, v) -> {
              v.add(inner);
              return v;
            });
            map.computeIfAbsent(project.getFileName().toString(), k -> {
              List<TableEntry> list = new ArrayList<>();
              list.add(inner);
              return list;
            });
            System.out.println("No input file found for " + analyzerResult.getFileName());
            continue;
          }
          System.out.println("Input: " + inputFile);
          System.out.println("Output: " + file);

          if (!invokePython(inputFile, file, sbomType)) {
            var inner = new TableEntry(analyzerName, EMPTY_METRICS, EMPTY_METRICS);
            map.computeIfPresent(project.getFileName().toString(), (k, v) -> {
              v.add(inner);
              return v;
            });
            map.computeIfAbsent(project.getFileName().toString(), k -> {
              List<TableEntry> list = new ArrayList<>();
              list.add(inner);
              return list;
            });
            continue;
          }

          FileReader jsonReader = new JsonReader();
          List<Dependency> input = jsonReader.readFile(file);
          List<Dependency> truth = jsonReader.readFile(truthJson);
          var inner = new TableEntry(analyzerName,
              calculateMetrics(calculateResult(getDirectDeps(input), getDirectDeps(truth))),
              calculateMetrics(
                  calculateResult(getTransitiveDeps(input), getTransitiveDeps(truth))));
          map.computeIfPresent(project.getFileName().toString(), (k, v) -> {
            v.add(inner);
            return v;
          });
          map.computeIfAbsent(project.getFileName().toString(), k -> {
            List<TableEntry> list = new ArrayList<>();
            list.add(inner);
            return list;
          });

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    DecimalFormat df = new DecimalFormat("#.##");
    StringBuilder sb = new StringBuilder();
    sb.append(
        "| Project | Analyzer | Direct Precision | Direct Recall | Direct F1 | Transitive Precision | Transitive Recall | Transitive F1|")
        .append(System.lineSeparator());
    sb.append("| --- | --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
    for (Map.Entry<String, List<TableEntry>> entry : map.entrySet()) {
      for (TableEntry inner : entry.getValue()) {
        sb.append("| ").append(entry.getKey()).append(" | ").append(inner.analyzerName)
            .append(" | ").append(df.format(inner.directDeps().precision)).append(" | ")
            .append(df.format(inner.directDeps().recall)).append(" | ")
            .append(df.format(inner.directDeps().f1)).append(" | ")
            .append(df.format(inner.transitiveDeps().precision)).append(" | ")
            .append(df.format(inner.transitiveDeps().recall)).append(" | ")
            .append(df.format(inner.transitiveDeps().f1)).append(" | ")
            .append(System.lineSeparator());
      }
    }
    Files.writeString(resultFolder.resolve("large-table.md"), sb.toString());
  }

  private List<Dependency> getDirectDeps(List<Dependency> truth) {
    return truth.stream().filter(this::isDirectDependency).toList();
  }

  private List<Dependency> getTransitiveDeps(List<Dependency> input) {
    return input.stream().filter(Predicate.not(this::isDirectDependency)).toList();
  }

  private Path getMavenTruth(Path project) throws IOException {
    return findJsonFile(
        Files.walk(project).filter(v -> v.getFileName().toString().equals("maven-dependency-tree"))
            .findAny().orElse(null));
  }

  /**
   * This function invokes the python script in the transformer directory to convert the input file to the desired format
   *
   * @param inputFile: Path to the input file
   * @param file: Path to the output file
   * @param sbomType: The desired output format
   */
  private boolean invokePython(Path inputFile, Path file, String sbomType) {
    String command = "python3 ./transformer/main.py -s %s -i \"%s\" -o \"%s\"";
    command = command.formatted(sbomType, inputFile.toAbsolutePath(), file);
    ProcessBuilder builder = new ProcessBuilder();
    builder.redirectErrorStream(true);
    builder.inheritIO();
    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      builder.command("cmd.exe", "/c", command);
    } else {
      builder.command("sh", "-c", command);
    }
    int error = 0;
    try {
      Process process = builder.start();
      error = process.waitFor();
    } catch (Exception e) {
      return false;
    }
    return !(file.toFile().length() == 0 || error != 0);
  }



  /**
   * InnerLargeTableGenerator is a class that generates a large table of dependencies.
   */
  public record TableEntry(String analyzerName, Metrics directDeps, Metrics transitiveDeps) {
  }


  private boolean isDirectDependency(Dependency v) {
    return v.getDepth() == 1 || v.getDepth() == 2 ;
  }

  public record Result(List<Dependency> truePositive, List<Dependency> falsePositive,
      List<Dependency> falseNegative) {
  }
  record PrintOut(Result directDeps, Result transitiveDeps) {
  }

  record Metrics(double precision, double recall, double f1) {
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

  private String fileNameToType(String fileName) {
    return switch (fileName) {

      case "build-info-go" -> "cyclonedx";
      case "cdxgen" -> "cyclonedx";
      case "cyclonedx-maven-plugin" -> "cyclonedx";
      case "depscan" -> "cyclonedx";
      case "jbom" -> "jbom"; // it is almost cyclonedx but not quite
      case "maven-dependency-tree" -> "truth";
      case "openrewrite" -> "openrewrite";
      // case "bom" -> "spdx";
      /* 
      case "ort" -> "ort";
      case "sbom-tool" -> "spdx";
      case "scancode" -> "scancode";
      case "scanoss" -> "scanoss";
      case "spdx-maven-plugin" -> "spdx";
      case "spdx-sbom-generator" -> "spdx";
      case "syft" -> "syft";
      */
      default -> "";
    };
  }

  private Path findJsonFile(Path folder) throws IOException {
    if (folder == null) {
      return null;
    }
    return Files.walk(folder).filter(v -> v.getFileName().toString().endsWith(".json")).findAny()
        .orElse(null);
  }

  private Result calculateResult(List<Dependency> input, List<Dependency> truth) {
    List<Dependency> truePositive = truePositives(input, truth);
    List<Dependency> falsePositive = falsePositives(input, truth);
    List<Dependency> falseNegative = falseNegatives(input, truth);
    return new Result(truePositive, falsePositive, falseNegative);
  }

  private Metrics calculateMetrics(Result result) {
    double precision = 0;
    double recall = 0;
    double f1 = 0;
    List<Dependency> truePositive = result.truePositive().stream().distinct().toList();

    List<Dependency> falsePositive = result.falsePositive().stream().distinct().toList();
    List<Dependency> falseNegative = result.falseNegative().stream().distinct().toList();

    if (truePositive.size() + falsePositive.size() > 0) {
      precision =
          (truePositive.size() * 100.0 / (truePositive.size() + result.falsePositive().size()));
    }
    if (truePositive.size() + result.falseNegative().size() > 0) {
      recall = (truePositive.size() * 100.0 / (truePositive.size() + falseNegative.size()));
    }
    if (precision + recall > 0) {
      f1 = (2 * precision * recall / (precision + recall));
    }
    return new Metrics(precision, recall, f1);
  }

  private Metrics EMPTY_METRICS = new Metrics(-1, -1, -1);
}
