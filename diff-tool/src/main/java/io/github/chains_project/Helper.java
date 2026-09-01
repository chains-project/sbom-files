package io.github.chains_project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.data.Dependency;

public class Helper {

  public static void main(String[] args) throws Exception {
    new Helper()
        .calc(Path.of("/Users/martinwittlinger/Library/CloudStorage/OneDrive-Persönlich/programmieren/sbom-files/results"));
  }

  public void calc(Path resultFolder) throws IOException {

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
          if (sbomType.equals("build-info-go")) {
            // does not work
            continue;
          }
          if (sbomType.equals("truth")) {
            continue;
          }
          command = command.formatted(sbomType, inputFile.toAbsolutePath(), file);
          System.out.println(command);
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
          List<Dependency> truePositive = truePositives(input, truth);
          List<Dependency> falsePositive = falsePositives(input, truth);
          List<Dependency> falseNegative = falseNegatives(input, truth);
          Result result = new Result(truePositive, falsePositive, falseNegative);
          ObjectMapper mapper = new ObjectMapper();
          File output = new File("./resultCalc/" + project.getFileName() + "_"
              + analyzerResult.getFileName() + ".json");
          Files.createDirectories(output.toPath().getParent());
          mapper.writeValue(output, result);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public record Result(List<Dependency> truePositive, List<Dependency> falsePositive,
      List<Dependency> falseNegative) {
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
    Set<Dependency> firstSet = new HashSet<>(input);
    Set<Dependency> secondSet = new HashSet<>(truth);
    var diffFirst = new HashSet<>(secondSet);
    diffFirst.removeAll(firstSet);
    return new ArrayList<>(diffFirst);
  }

  private String fileNameToType(String fileName) {
    return switch (fileName) {

      case "bom" -> ""; //"spdx";
      case "build-info-go" -> "build-info-go";
      case "cdxgen" -> "cyclonedx";
      case "cyclonedx-maven-plugin" -> "cyclonedx";
      case "depscan" -> "cyclonedx";
      case "jbom" -> "";//"cyclonedx";
      case "maven-dependency-tree" -> "truth";
      case "openrewrite" -> "cyclonedx";
      case "ort" -> "ort";
      case "sbom-tool" -> "spdx";
      case "scancode" -> "scancode";
      case "scanoss" -> "scanoss";
      case "spdx-maven-plugin" -> "spdx";
      case "spdx-sbom-generator" -> "spdx";
      case "syft" -> "syft";

      default -> throw new IllegalArgumentException("Unexpected value: " + fileName);
    };
  }

  private Path findJsonFile(Path folder) throws IOException {
    return Files.walk(folder).filter(v -> v.getFileName().toString().endsWith(".json")).findAny()
        .get();
  }
}
