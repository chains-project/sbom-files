package io.github.chains_project.data;

public record AnalyzerResult(String name, String project, Result direct, 
    Result transitive) {
  
}
