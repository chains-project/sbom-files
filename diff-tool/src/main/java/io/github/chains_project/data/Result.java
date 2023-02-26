package io.github.chains_project.data;

import java.util.List;

public record Result(List<Dependency> truePositive, List<Dependency> falsePositive,
    List<Dependency> falseNegative) {

      public static Result empty() {
        List<Dependency> truePositive = List.of();
        List<Dependency> falsePositive = List.of();
        List<Dependency> falseNegative = List.of();
        return new Result(truePositive, falsePositive, falseNegative);
      }
}