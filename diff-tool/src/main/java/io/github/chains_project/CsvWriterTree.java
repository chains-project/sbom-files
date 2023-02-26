package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import io.github.chains_project.data.AnalyzerResult;
import io.github.chains_project.data.Dependency;
import io.github.chains_project.data.Metrics;
import io.github.chains_project.data.Result;

public class CsvWriterTree {
  public static void main(String[] args) throws IOException {
    var list = new CreateDataTree().createData(Path.of("./results"));
    StringBuilder sb = new StringBuilder();
    sb.append("project,analyzer,D_P,D_R,D_F,SIZE,T_P,T_R,T_F,SIZE");
    sb.append(System.lineSeparator());
    for (AnalyzerResult analyzerResult : list) {
      sb.append(analyzerResult.project());
      sb.append(",");
      sb.append(analyzerResult.name());
      sb.append(",");
      appendDirect(sb, analyzerResult);
      appendTransitives(sb, analyzerResult);
    }
    Files.writeString(Path.of("./resultsTree.csv"), sb.toString());
    list = new CreateDataTreeAllDeps().createData(Path.of("./results"));
    sb.setLength(0);
    sb.append("project,analyzer,D_P,D_R,D_F,SIZE");
    sb.append(System.lineSeparator());
    for (AnalyzerResult analyzerResult : list) {
      sb.append(analyzerResult.project());
      sb.append(",");
      sb.append(analyzerResult.name());
      sb.append(",");
      appendDirect(sb, analyzerResult);
      sb.append(System.lineSeparator());
    }
    Files.writeString(Path.of("./resultsTreeAll.csv"), sb.toString());
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
