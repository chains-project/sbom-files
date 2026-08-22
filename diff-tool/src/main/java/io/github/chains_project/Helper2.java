package io.github.chains_project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.chains_project.data.Dependency;

public class Helper2 {
  public static void main(String[] args) throws IOException {
    Path path = Path.of("resultCalc");
    ObjectMapper mapper = new ObjectMapper();
    StringBuilder sb = new StringBuilder();
    sb.append("|Name| True Positives| False Positives| False Negatives| Sum |").append(System.lineSeparator());
    sb.append("|---|---|---|---|---|").append(System.lineSeparator());
    Files.list(path).sorted(Comparator.comparing(v -> v.getFileName())).forEach(v -> {
      try {
        var result = mapper.readValue(v.toFile(), InnerHelper2.class);
        sb.append("|" + v.getFileName() + "|" + result.truePositive().size() + "|"
            + result.falsePositive().size() + "|" + result.falseNegative().size() + "|"
            + (result.truePositive().size() + result.falsePositive().size()
                + result.falseNegative().size())
            + "|").append(System.lineSeparator());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    Files.writeString(Path.of("./resultMarkdown.md"), sb);
}


/**
 * InnerHelper2
 */
public record InnerHelper2(List<Dependency> truePositive, List<Dependency> falsePositive,
    List<Dependency> falseNegative) {

}}
