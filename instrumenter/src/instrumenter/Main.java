package instrumenter;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Steps:
 * 1. Parse command line arguments.
 * 2. Load resources from the instrumenter and the engine:
 * - Methods in bytecodes costs
 * - Allowed libraries
 * - Disallowed classes
 * - Disallowed methods
 * 3. Instrument the specified package.
 */
public class Main {
    private static final Logger log = LogManager.getRootLogger();

    public static void main(final String[] args) throws IOException, ParseException {
        // Parse arguments
        Options options = buildArgumentOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(options, args);
        if (!cmdLine.hasOption("package")) {
            log.error("Missing command line argument `package`");
            System.exit(1);
        }

        String engine = cmdLine.getOptionValue("engine").trim();
        String buildPath = cmdLine.getOptionValue("build").trim();
        String packagePath = cmdLine.getOptionValue("package").trim();
        String verboseLevel = cmdLine.getOptionValue("verbose", "INFO").trim();

        // Set logger level
        Logger.getRootLogger().setLevel(Level.toLevel(verboseLevel));

        // Load resources
        Map<String, Integer> methodBytecodeCosts = loadMethodBytecodeCosts();
        Set<String> allowedLibraries = loadAllowedLibraries();
        allowedLibraries.add(engine + "/api/");
        Set<String> disallowedClasses = loadDisallowedClasses();
        Map<String, List<String>> disallowedMethods = loadDisallowedMethods();

        // Instrument
        Instrumenter.instrumentPackage(
                packagePath,
                buildPath + "/" + packagePath,
                buildPath + "/instrumented/" + packagePath,
                methodBytecodeCosts, allowedLibraries, disallowedClasses, disallowedMethods
        );

        log.info("Instrumentation completed successfully!");
    }

    private static Options buildArgumentOptions() {
        Options options = new Options();
        Option engineOpt = new Option("e", "engine", true, "Engine");
        options.addOption(engineOpt);
        Option buildOpt = new Option("b", "build", true, "Build path");
        options.addOption(buildOpt);
        Option packageOpt = new Option("p", "package", true, "Package");
        options.addOption(packageOpt);
        Option verboseOpt = new Option("v", "verbose", true, "Verbose level (error, warn, info, debug)");
        options.addOption(verboseOpt);
        return options;
    }

    private static Map<String, Integer> loadMethodBytecodeCosts() throws IOException {
        BufferedReader reader = getBufferedReader("resources/MethodBytecodeCosts.txt");
        Map<String, Integer> methodBytecodeCosts = new HashMap<>();
        String newLine;
        while ((newLine = reader.readLine()) != null) {
            String[] tokens = newLine.split("\\s+");
            methodBytecodeCosts.put(tokens[0], Integer.parseInt(tokens[1]));
        }
        reader.close();
        return methodBytecodeCosts;
    }

    private static Set<String> loadAllowedLibraries() throws IOException {
        BufferedReader reader = getBufferedReader("resources/AllowedLibraries.txt");
        Set<String> allowedLibraries = new HashSet<>();
        String newLine;
        while ((newLine = reader.readLine()) != null) {
            allowedLibraries.add(newLine + "/");
        }
        reader.close();
        return allowedLibraries;
    }

    private static Set<String> loadDisallowedClasses() throws IOException {
        BufferedReader reader = getBufferedReader("resources/DisallowedClasses.txt");
        Set<String> disallowedClasses = new HashSet<>();
        String newLine;
        while ((newLine = reader.readLine()) != null) {
            disallowedClasses.add(newLine);
        }
        reader.close();
        return disallowedClasses;
    }

    private static Map<String, List<String>> loadDisallowedMethods() throws IOException {
        BufferedReader reader = getBufferedReader("resources/DisallowedMethods.txt");
        Map<String, List<String>> disallowedMethods = new HashMap<>();
        String newLine;
        while ((newLine = reader.readLine()) != null) {
            String[] tokens = newLine.split("\\s+");
            List<String> methodNames = disallowedMethods.computeIfAbsent(tokens[0], k -> new ArrayList<>());
            methodNames.add(tokens[1]);
        }
        reader.close();
        return disallowedMethods;
    }

    private static BufferedReader getBufferedReader(String filePath) {
        InputStream in = Main.class.getClassLoader().getResourceAsStream(filePath);
        if (in == null) {
            log.error("Unable to find file `" + filePath + "`");
            System.exit(1);
        }
        return new BufferedReader(new InputStreamReader(in));
    }
}
