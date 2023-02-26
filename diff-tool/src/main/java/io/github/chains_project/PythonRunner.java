package io.github.chains_project;

import java.nio.file.Path;

public class PythonRunner {
  
  /**
  * This function invokes the python script in the transformer directory to convert the input file to the desired format
  *
  * @param inputFile: Path to the input file
  * @param file: Path to the output file
  * @param sbomType: The desired output format
  */
  public static boolean invokePython(Path inputFile, Path file, String sbomType) {
    String command = "python3 ./transformer/main.py -s %s -i \"%s\" -o \"%s\"";
    command = command.formatted(sbomType, inputFile.toAbsolutePath(), file);
    ProcessBuilder builder = new ProcessBuilder();
    builder.redirectErrorStream(true);
    builder.inheritIO();
    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      builder.command("cmd.exe", "/c", command);
    } else {
      builder.command("sh", "-c", command);
    }
    int error = 0;
    try {
      Process process = builder.start();
      error = process.waitFor();
    } catch (Exception e) {
      return false;
    }
    if (file.toFile().length() == 0 || error != 0) {
      return false;
    }
    return true;
  }
}
