package io.github.chains_project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chains_project.data.AnalyzerResult;
import io.github.chains_project.data.Dependency;

public class Helper {

  public static void main(String[] args) throws Exception {
    new Helper().createData(Path.of("./results"));
  }
  private static final List<String> analyzerNames = List.of("build-info-go", "cdxgen", "cyclonedx-maven-plugin", "depscan","jbom","openrewrite");

  public void createData(Path resultFolder) throws IOException {
    List<AnalyzerResult> results = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    sb.append(
        "project,analyzer,D_TP,D_FP,D_FN,D_P,D_R,D_F1,D_SIZE,T_TP,T_FP,T_FN,T_P,T_R,T_F1,T_SIZE")
        .append(System.lineSeparator());

    for (Path project : Files.list(resultFolder).toArray(Path[]::new)) {
      Path truthJson = getMavenTruth(project);

      for (String analyzerName : analyzerNames) {
        try {
          Path analyzerResult = project.resolve(analyzerName);
          if (!Files.exists(analyzerResult)) {
            sb.append(project.getFileName()).append(",");
            sb.append(analyzerResult.getFileName()).append(",");
            sb.append("0,0,0,0,0,0,0,0,0,0,0,0,0,0").append(System.lineSeparator());
            continue;
          }
          String sbomType = SbomType.fileNameToType(analyzerResult.getFileName().toString());
          if (sbomType.isEmpty() || sbomType.equals("truth")) {
            continue;
          }
          System.out.println("Processing: " + project.getFileName() + " with " + analyzerResult
              .getFileName() + " (" + sbomType + ")");

          Path file = Files.createTempFile("chains", ".json");
          Path inputFile = findJsonFile(analyzerResult);

          sb.append(project.getFileName()).append(",");
          sb.append(analyzerResult.getFileName()).append(",");
          if (inputFile == null) {
            sb.append("0,0,0,0,0,0,0,0,0,0,0,0,0,0").append(System.lineSeparator());
            System.out.println("No input file found for " + analyzerResult.getFileName());
            continue;
          }
          System.out.println("Input: " + inputFile);
          System.out.println("Output: " + file);

          if(!invokePython(inputFile, file, sbomType)) {
            sb.append("0,0,0,0,0,0,0,0,0,0,0,0,0,0").append(System.lineSeparator());
            continue;
          }

          FileReader jsonReader = new JsonReader();
          List<Dependency> input = jsonReader.readFile(file);
          List<Dependency> truth = jsonReader.readFile(truthJson);
          var directDependencyResult = calculateResult(getDirectDeps(input), getDirectDeps(truth));
          var transitiveDeps = calculateResult(getTransitiveDeps(input), getTransitiveDeps(truth));
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
    Files.writeString(Path.of("./Results.csv"),
        sb.toString());
  }

  private List<Dependency> getDirectDeps(List<Dependency> truth) {
    return truth.stream().filter(this::isDirectDependency).toList();
  }

  private List<Dependency> getTransitiveDeps(List<Dependency> input) {
    return input.stream().filter(v -> v.getDepth() > 1).toList();
  }

  private Path getMavenTruth(Path project) throws IOException {
    try {
      return findJsonFile(Files.walk(project)
        .filter(v -> v.getFileName().toString().equals("maven-dependency-tree")).findAny().get());
      
    } catch (Exception e) {
      return null;
      // TODO: handle exception
    }
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
    if(file.toFile().length() == 0 || error != 0) {
      return false;
    }
    return true;
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
      recall = (int) (truePositive.size() * 100.0 / (truePositive.size() + falseNegative.size()));
    }
    if (precision + recall > 0) {
      f1 = (int) (2 * precision * recall / (precision + recall));
    }
    return new Metrics(precision, recall, f1);
  }
}
