package io.github.chains_project.data;

import java.util.List;

public record Metrics(double precision, double recall, double f1, double size) {
 
  
  public static Metrics of(Result result) {
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
