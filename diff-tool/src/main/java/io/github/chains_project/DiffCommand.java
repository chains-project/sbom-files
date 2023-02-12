package io.github.chains_project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.chains_project.data.Dependency;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "diff", mixinStandardHelpOptions = true)
public class DiffCommand implements Runnable {

    @Parameters(paramLabel = "<PATH>", 
        description = "The path to result folder.")
    String inputPath;
    @Parameters(paramLabel = "<truth>", description = "The path of the first file.")
    String pathGroundTruth;
    String type;
    @Override
    public void run() {
        try {
            Path file = Files.createTempFile("chains", ".json");
            String command = "python ./transformer/main.py -s %s -i %s -o %s";
            command = String.format(type, type, inputPath, file);
            ProcessBuilder builder = new ProcessBuilder();
            builder.redirectErrorStream(true);
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                builder.command("cmd.exe", "/c", command);
            } else {
                builder.command("sh", "-c", command);
            }
            Process process = builder.start();
            process.waitFor();
            FileReader jsonReader = new JsonReader();
            List<Dependency> input = jsonReader.readFile(Path.of(inputPath));
            List<Dependency> truth = jsonReader.readFile(file);
            List<Dependency> truePositive = truePositives(input, truth);
            List<Dependency> falsePositive = falsePositives(input, truth);
            List<Dependency> falseNegative = falseNegatives(input, truth);
            Result result = new Result(truePositive, falsePositive, falseNegative);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    public record Result(List<Dependency> truePositive, List<Dependency> falsePositive,
            List<Dependency> trueNegative) {
    }
    /**
     * Calculates the true positives, all dependencies that are in both lists.
     * @param input The first list.
     * @param truth  The second list.
     * @return
     */
    private List<Dependency> truePositives(List<Dependency> input, List<Dependency> truth) {
        Set<Dependency> firstSet = new HashSet<>(input);
        Set<Dependency> secondSet = new HashSet<>(truth);
        var diffFirst = new HashSet<>(firstSet);
        diffFirst.retainAll(secondSet);
        return new ArrayList<>(diffFirst);
    }

    private List<Dependency> falsePositives(List<Dependency> input, List<Dependency> truth) {
        Set<Dependency> firstSet = new HashSet<>(input);
        Set<Dependency> secondSet = new HashSet<>(truth);
        var diffFirst = new HashSet<>(firstSet);
        diffFirst.removeAll(secondSet);
        return new ArrayList<>(diffFirst);
    }
    // All that are in the truth but not in the input.
    private List<Dependency> falseNegatives(List<Dependency> input, List<Dependency> truth) {
        Set<Dependency> firstSet = new HashSet<>(input);
        Set<Dependency> secondSet = new HashSet<>(truth);
        var diffFirst = new HashSet<>(secondSet);
        diffFirst.removeAll(firstSet);
        return new ArrayList<>(diffFirst);
    }
}
