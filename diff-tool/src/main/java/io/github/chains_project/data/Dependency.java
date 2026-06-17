package io.github.chains_project.data;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Dependency {



  private String groupId;
  private String artifactId;
  private String classifier;
  private String version;
  private String scope;
  private int depth;

  private String submodule; 
  
      
  public Dependency() {
  }
  public Dependency(String groupId, String artifactId, String classifier, String version, String scope, int depth, String submodule) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.classifier = classifier;
    this.version = version;
    this.scope = scope;
    this.depth = depth;
    this.submodule = submodule;
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Dependency)) {
      return false;
    }
    Dependency other = (Dependency) obj;
    return Objects.equals(groupId, other.groupId) && Objects.equals(artifactId, other.artifactId)
        && Objects.equals(version, other.version);
  }
  /**
   * @return the groupId
   */
  public String getGroupId() {
    return groupId;
  }
  /**
   * @param groupId the groupId to set
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
  /**
   * @return the artifactId
   */
  public String getArtifactId() {
    return artifactId;
  }
  /**
   * @param artifactId the artifactId to set
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
  /**
   * @return the classifier
   */
  public String getClassifier() {
    return classifier;
  }
  /**
   * @param classifier the classifier to set
   */
  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }
  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }
  /**
   * @param version the version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }
  /**
   * @return the scope
   */
  public String getScope() {
    return scope;
  }
  /**
   * @param scope the scope to set
   */
  public void setScope(String scope) {
    this.scope = scope;
  }
  /**
   * @return the depth
   */
  public int getDepth() {
    return depth;
  }
  /**
   * @param depth the depth to set
   */
  public void setDepth(int depth) {
    this.depth = depth;
  }
  /**
   * @return the submodule
   */
  public String getSubmodule() {
    return submodule;
  }
  /**
   * @param submodule the submodule to set
   */
  public void setSubmodule(String submodule) {
    this.submodule = submodule;
  }




}
