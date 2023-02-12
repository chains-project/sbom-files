package io.github.chains_project;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.github.chains_project.data.Dependency;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "diff", mixinStandardHelpOptions = true)
public class DiffCommand implements Runnable {

    @Parameters(paramLabel = "<PATH>", 
        description = "The path of the first file.")
    String pathOfFirst;
    @Parameters(paramLabel = "<PATH>", description = "The path of the first file.")
    String pathOfSecond;
    @Override
    public void run() {
        FileReader jsonReader = new JsonReader();
        List<Dependency> first = jsonReader.readFile(Path.of(pathOfFirst));
        List<Dependency> second = jsonReader.readFile(Path.of(pathOfSecond));
        Set<Dependency> firstSet = new HashSet<>(first);
        Set<Dependency> secondSet = new HashSet<>(second);
        var diffFirst = new HashSet<>(firstSet);
        diffFirst.removeAll(secondSet);
        var diffSecond = new HashSet<>(secondSet);
        diffSecond.removeAll(firstSet);
        System.out.println("First file has " + diffFirst.size()
                + " dependencies that are not in the second file.");
        diffFirst.forEach(System.out::println);
        System.out.println("Second file has " + diffSecond.size()
                + " dependencies that are not in the first file.");
        diffSecond.forEach(System.out::println);
    }

}
