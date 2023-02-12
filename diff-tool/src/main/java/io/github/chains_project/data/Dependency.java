package io.github.chains_project.data;

public record Dependency(String groupId, String artifactId,String classifier, String version, String scope, int depth, String submodule) {
  
}
