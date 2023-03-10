package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.github.chains_project.data.AnalyzerResult;
import io.github.chains_project.data.Metrics;
import io.github.chains_project.data.Result;

public class LargeTableGenerator {


  public static void main(String[] args) throws Exception {
    new LargeTableGenerator().createData(Path.of("./results"));
  }

  private static final List<String> analyzerNames = List.of("build-info-go", "cdxgen",
      "cyclonedx-maven-plugin", "depscan", "jbom", "openrewrite");

  public void createData(Path resultFolder) throws IOException {
    CreateDataTree createDataTree = new CreateDataTree();
    var result = createDataTree.createData(resultFolder);
    Map<String, List<InnerLargeTableGenerator>> map = new HashMap<>();
    Locale.setDefault(Locale.US);
    for (AnalyzerResult analyzerResult : result) {
      var entry = new InnerLargeTableGenerator(analyzerResult.name(),analyzerResult.project(), analyzerResult.direct(),
          analyzerResult.transitive());
      map.computeIfPresent(analyzerResult.project(), (k, v) -> {
        v.add(entry);
        return v;
      });
      map.computeIfAbsent(analyzerResult.project(), k -> {
        List<InnerLargeTableGenerator> list = new ArrayList<>();
        list.add(entry);
        return list;
      });
    }
    DecimalFormat df = new DecimalFormat("#.##");
    StringBuilder sb = new StringBuilder();
    sb.append(
        "| Project | Analyzer | Direct Precision | Direct Recall | Direct F1 | Transitive Precision | Transitive Recall | Transitive F1|")
        .append(System.lineSeparator());
    sb.append("| --- | --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
    for (Map.Entry<String, List<InnerLargeTableGenerator>> entry : map.entrySet()) {
      for (InnerLargeTableGenerator inner : entry.getValue()) {
        Metrics direct = Metrics.of(inner.direct());
        Metrics transitive = Metrics.of(inner.transitive());
        sb.append("| ").append(entry.getKey()).append(" | ").append(inner.name())
            .append(" | ").append(df.format(
                direct.precision())).append(" | ")
            .append(df.format(direct.recall())).append(" | ")
            .append(df.format(direct.f1())).append(" | ")
            .append(df.format(
                transitive.precision())).append(" | ")
            .append(df.format(
                transitive.recall())).append(" | ")
            .append(df.format(transitive.f1())).append(" | ")
            .append(System.lineSeparator());
      }
  }
  Files.writeString(resultFolder.resolve("large-table.md"), sb.toString());
  }
  
  
  public record InnerLargeTableGenerator(String name, String project, Result direct, Result transitive) {
  }

  




}
