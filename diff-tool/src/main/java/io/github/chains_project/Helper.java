package io.github.chains_project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.data.Dependency;

public class Helper {

  public static void main(String[] args) throws Exception {
    new Helper().createData(Path.of("./results"));
  }
//TODO: openrewrite+errorprone
  public void createData(Path resultFolder) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("project,analyzer,D_TP,D_FP,D_FN,D_P,D_R,D_F1,D_SIZE,T_TP,T_FP,T_FN,T_P,T_R,T_F1,T_SIZE").append(System.lineSeparator());
    for (Path project : Files.list(resultFolder).toArray(Path[]::new)) {
      for (Path analyzerResult : Files.list(project).toArray(Path[]::new)) {
        try {
          Path inputFile = findJsonFile(analyzerResult);
          Path truthJson = findJsonFile(Files.walk(project)
              .filter(v -> v.getFileName().toString().equals("maven-dependency-tree")).findAny()
              .get());
          Path file = Files.createTempFile("chains", ".json");
          String command = "python3 ./transformer/main.py -s %s -i \"%s\" -o \"%s\"";
          String sbomType = fileNameToType(analyzerResult.getFileName().toString());
          if (sbomType.isEmpty()) {
            continue;
          }
          if (sbomType.equals("truth")) {
            continue;
          }
          sb.append(project.getFileName()).append(",");
          sb.append(analyzerResult.getFileName()).append(",");
          command = command.formatted(sbomType, inputFile.toAbsolutePath(), file);
          ProcessBuilder builder = new ProcessBuilder();
          builder.redirectErrorStream(true);
          builder.inheritIO();
          if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            builder.command("cmd.exe", "/c", command);
          } else {
            builder.command("sh", "-c", command);
          }
          Process process = builder.start();
          process.waitFor();
          FileReader jsonReader = new JsonReader();
          List<Dependency> input = jsonReader.readFile(file);
          List<Dependency> truth = jsonReader.readFile(truthJson);
          var directDependencyResult =
              calculateResult(input.stream().filter(this::isDirectDependency).toList(),
                  truth.stream().filter(v -> v.getDepth() == 1).toList());
          var transitiveDeps =
              calculateResult(input.stream().filter(v -> v.getDepth() > 1).toList(),
                  truth.stream().filter(v -> v.getDepth() > 1).toList());
          ObjectMapper mapper = new ObjectMapper();
          File output = new File("./resultCalc/" + project.getFileName() + "_"
              + analyzerResult.getFileName() + ".json");
          Files.createDirectories(output.toPath().getParent());
          PrintOut value = new PrintOut(directDependencyResult, transitiveDeps);
          appendDirectDeps(sb, truth, value);
          appendTransitiveDeps(sb, truth, value);
          sb.append(System.lineSeparator());
          mapper.writeValue(output, value);
        } catch (Exception e) {
          sb.append("0,0,0,0,0,0,0,0,0,0,0,0,0,0").append(System.lineSeparator());
          e.printStackTrace();
        }
      }
    }
    Files.writeString(Path.of("./Results.csv"), sb.toString().lines().filter(v -> !v.equals("0,0,0,0,0,0,0,0,0,0,0,0,0,0")).collect(Collectors.joining(System.lineSeparator()))
    );
  }

  private void appendTransitiveDeps(StringBuilder sb, List<Dependency> truth, PrintOut value) {
    var metrics = calculateMetrics(value.transitiveDeps());
    sb.append(value.transitiveDeps().truePositive().size()).append(",");
    sb.append(value.transitiveDeps().falsePositive().size()).append(",");
    sb.append(value.transitiveDeps().falseNegative().size()).append(",");
    sb.append(metrics.precision()).append(",");
    sb.append(metrics.recall()).append(",");
    sb.append(metrics.f1()).append(",");
    sb.append(truth.stream().filter(v -> v.getDepth() > 1).distinct().toList().size()).append(",");
  }

  private void appendDirectDeps(StringBuilder sb, List<Dependency> truth, PrintOut value) {
    var metrics = calculateMetrics(value.directDeps());
    sb.append(value.directDeps().truePositive().size()).append(",");
    sb.append(value.directDeps().falsePositive().size()).append(",");
    sb.append(value.directDeps().falseNegative().size()).append(",");
    sb.append(metrics.precision()).append(",");
    sb.append(metrics.recall()).append(",");
    sb.append(metrics.f1()).append(",");
    sb.append(truth.stream().filter(v -> v.getDepth() == 1).distinct().toList().size()).append(",");
  }

  private boolean isDirectDependency(Dependency v) {
    return v.getDepth() == 1;
  }

  public record Result(List<Dependency> truePositive, List<Dependency> falsePositive,
      List<Dependency> falseNegative) {
  }
  record PrintOut(Result directDeps, Result transitiveDeps) {
  };

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
    return Files.walk(folder).filter(v -> v.getFileName().toString().endsWith(".json")).findAny()
        .get();
  }

  private Result calculateResult(List<Dependency> input, List<Dependency> truth) {
    List<Dependency> truePositive = truePositives(input, truth);
    List<Dependency> falsePositive = falsePositives(input, truth);
    List<Dependency> falseNegative = falseNegatives(input, truth);
    return new Result(truePositive, falsePositive, falseNegative);
  }

  private Metrics calculateMetrics(Result result) {
    int precision = 0;
    int recall = 0;
    int f1 = 0;
    List<Dependency> truePositive = result.truePositive().stream().distinct().toList();

    List<Dependency> falsePositive = result.falsePositive().stream().distinct().toList();
    List<Dependency> falseNegative = result.falseNegative().stream().distinct().toList();

    if (truePositive.size() + falsePositive.size() > 0) {
      precision = (int) (truePositive.size() * 100.0
          / (truePositive.size() + result.falsePositive().size()));
    }
    if (truePositive.size() + result.falseNegative().size() > 0) {
      recall = (int) (truePositive.size() * 100.0
          / (truePositive.size() + falseNegative.size()));
    }
    if (precision + recall > 0) {
      f1 = (int) (2 * precision * recall / (precision + recall));
    }
    return new Metrics(precision, recall, f1);
  }
}
