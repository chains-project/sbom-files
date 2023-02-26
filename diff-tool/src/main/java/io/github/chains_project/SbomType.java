package io.github.chains_project;

public class SbomType {
  
  public static String fileNameToType(String fileName) {
    return switch (fileName) {

      case "build-info-go" -> "cyclonedx";
      case "cdxgen" -> "cyclonedx";
      case "cyclonedx-maven-plugin" -> "cyclonedx";
      case "depscan" -> "cyclonedx";
      case "jbom" -> "jbom"; // it is almost cyclonedx but not quite
      case "maven-dependency-tree" -> "truth";
      case "openrewrite" -> "openrewrite";
      // case "bom" -> "spdx";
      /* 
      case "ort" -> "ort";
      case "sbom-tool" -> "spdx";
      case "scancode" -> "scancode";
      case "scanoss" -> "scanoss";
      case "spdx-maven-plugin" -> "spdx";
      case "spdx-sbom-generator" -> "spdx";
      case "syft" -> "syft";
      */
      default -> "";
    };
  }
}
