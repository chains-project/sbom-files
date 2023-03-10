package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import io.github.chains_project.data.AnalyzerResult;
import io.github.chains_project.data.Dependency;
import io.github.chains_project.data.Metrics;
import io.github.chains_project.data.Result;

public class CsvWriterAverage {
  public static void main(String[] args) throws IOException {
    createCSVAverage();
  }

  public static void createCSVAverage() throws IOException {
    var list = new CreateDataTree().createData(Path.of("./results"));
    createCSV(list, "resultsTreeAverage");
    // createCSV(new CreateDataGraph().createData(Path.of("./results")), "resultsGraphAverage");
    createCSV(new CreateDataTreeAllDeps().createData(Path.of("./results")), "resultsTreeAllAverage");

  }

  private static void createCSV(List<AnalyzerResult> list, String fileName) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("analyzer,D_P,D_R,D_F1,T_P,T_R,T_F1");
    sb.append(System.lineSeparator());
    calculateAverage(new ArrayList<>(list), v -> false).forEach(v -> {
      sb.append(v.name());
      sb.append(",");
      sb.append(v.directPrecision());
      sb.append(",");
      sb.append(v.directRecall());
      sb.append(",");
      sb.append(v.directF1());
      sb.append(",");
      sb.append(v.transitivePrecision());
      sb.append(",");
      sb.append(v.transitiveRecall());
      sb.append(",");
      sb.append(v.transitiveF1());
      sb.append(System.lineSeparator());
    });
    Files.writeString(Path.of("./" + fileName + ".csv"), sb.toString());
    sb.setLength(0);
    sb.append("analyzer,D_P,D_R,D_F1,T_P,T_R,T_F1");
    sb.append(System.lineSeparator());
    calculateAverage(new ArrayList<>(
        list), v -> v.direct().truePositive().isEmpty() && v.direct().falsePositive()
        .isEmpty()).forEach(v -> {
      sb.append(v.name());
      sb.append(",");
      sb.append(v.directPrecision());
      sb.append(",");
      sb.append(v.directRecall());
      sb.append(",");
      sb.append(v.directF1());
      sb.append(",");
      sb.append(v.transitivePrecision());
      sb.append(",");
      sb.append(v.transitiveRecall());
      sb.append(",");
      sb.append(v.transitiveF1());
      sb.append(System.lineSeparator());
    });
    Files.writeString(Path.of("./" + fileName + "WithoutFailures.csv"), sb.toString());
  }
  /**
   * Calculates the average of the metrics for each analyzer.
   * @param results the results
   * @param filter  the filter to remove results from the calculation (e.g. to remove results with no true positives), Any result that matches the filter will be removed from the list.
   * @return  the average metrics for each analyzer
   */
  private static List<AverageMetrics> calculateAverage(List<AnalyzerResult> results, Predicate<AnalyzerResult> filter) {
    List<AverageMetrics> averages = new ArrayList<>();
    results.removeIf(filter);
    var map = results.stream().collect(Collectors.groupingBy(v -> v.name()));
    for (var entry : map.entrySet()) {
      List<Metrics> directMetrics = entry.getValue().stream().map(v -> calculateMetrics(v.direct()))
          .collect(Collectors.toList());
      List<Metrics> transitiveMetrics = entry.getValue().stream()
          .map(v -> calculateMetrics(v.transitive())).collect(Collectors.toList());

      double directPrecision =
          directMetrics.stream().mapToDouble(v -> v.precision()).average().orElse(0);
      double directRecall = directMetrics.stream().mapToDouble(v -> v.recall()).average().orElse(0);
      double directF1 = directMetrics.stream().mapToDouble(v -> v.f1()).average().orElse(0);
      double transitivePrecision =
          transitiveMetrics.stream().mapToDouble(v -> v.precision()).average().orElse(0);
      double transitiveRecall =
          transitiveMetrics.stream().mapToDouble(v -> v.recall()).average().orElse(0);
      double transitiveF1 = transitiveMetrics.stream().mapToDouble(v -> v.f1()).average().orElse(0);

       averages.add(new AverageMetrics(entry.getKey() ,directPrecision, directRecall, directF1, transitivePrecision,
          transitiveRecall, transitiveF1));

    }
    return averages;
  }
    
  record AverageMetrics(String name, double directPrecision, double directRecall, double directF1,
      double transitivePrecision, double transitiveRecall, double transitiveF1) {
  }

  private static void appendDirect(StringBuilder sb, AnalyzerResult analyzerResult) {
    DecimalFormat df = new DecimalFormat("#.##");
    Metrics direct = calculateMetrics(analyzerResult.direct());
    sb.append(df.format(direct.precision()));
    sb.append(",");
    sb.append(df.format(direct.recall()));
    sb.append(",");
    sb.append(df.format(direct.f1()));
    sb.append(",");
    sb.append(df.format(direct.size()));
    sb.append(",");
  }

  private static void appendTransitives(StringBuilder sb, AnalyzerResult analyzerResult) {
    DecimalFormat df = new DecimalFormat("#.##");
    Metrics transitive = calculateMetrics(analyzerResult.transitive());
    sb.append(df.format(transitive.precision()));
    sb.append(",");
    sb.append(df.format(transitive.recall()));
    sb.append(",");
    sb.append(df.format(transitive.f1()));
    sb.append(",");
    sb.append(df.format(transitive.size()));
    sb.append(System.lineSeparator());
  }

  private static Metrics calculateMetrics(Result result) {
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
    return new Metrics(precision, recall, f1, truePositive.size()+falseNegative.size());
  }
}
