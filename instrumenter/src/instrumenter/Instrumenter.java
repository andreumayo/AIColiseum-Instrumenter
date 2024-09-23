package instrumenter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.signature.SignatureReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class with static methods and attributes to manage the instrumentation.
 * <p>
 * This class iterates over all directories and files of a package and uses the
 * class, method, and signature instrumenters to instrument their code. It also
 * implements methods to validate the classes and methods used.
 */
class Instrumenter {
    private static final Logger log = LogManager.getRootLogger();

    private static Map<String, Integer> methodBytecodeCosts;
    private static Set<String> allowedLibraries, disallowedClasses;
    private static Map<String, List<String>> disallowedMethods;

    static String packageName;

    static void instrumentPackage(String packageName, String path, String outputPath,
                                  Map<String, Integer> methodBytecodeCosts, Set<String> allowedLibraries,
                                  Set<String> disallowedClasses, Map<String, List<String>> disallowedMethods) throws IOException {
        log.debug("Instrumenter::instrumentPackage " + packageName + " " + path + " " + outputPath);
        // Setup static attributes
        Instrumenter.packageName = packageName;
        Instrumenter.methodBytecodeCosts = methodBytecodeCosts;
        Instrumenter.allowedLibraries = allowedLibraries;
        Instrumenter.disallowedClasses = disallowedClasses;
        Instrumenter.disallowedMethods = disallowedMethods;
        // Instrument the whole directory
        File dir = new File(path);
        if (!dir.isDirectory()) throw new RuntimeException("Input path '" + path + "' is not a directory");
        instrumentDir(dir, outputPath);
    }

    private static void instrumentDir(File dir, String outputPath) throws IOException {
        log.debug("Instrumenter::instrumentDir " + dir + " " + outputPath);
        new File(outputPath).mkdirs();
        for (File file : dir.listFiles()) {
            String newOutputPath = outputPath + "/" + file.getName();
            if (file.isFile()) {
                instrumentFile(file, newOutputPath);
            } else if (file.isDirectory()) {
                instrumentDir(file, newOutputPath);
            }
        }
    }

    private static void instrumentFile(File file, String outputPath) throws IOException {
        log.debug("Instrumenter::instrumentFile " + file + " " + outputPath);
        // Setup reader, writer and visitor ASM classes
        FileInputStream fis = new FileInputStream(file);
        ClassReader cr = new ClassReader(fis);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassInstrumenter(cw);
        // Visit and instrument
        cr.accept(cv, 0);
        // Write instrumented file
        byte[] b = cw.toByteArray();
        FileOutputStream fos = new FileOutputStream(outputPath);
        fos.write(b);
        fos.close();
    }

    static int getMethodBytecodeCost(String method) {
        Integer ans = methodBytecodeCosts.get(method);
        if (ans == null) return 1;
        return ans;
    }

    static void checkValidClass(String className) {
        if (className == null) return;
        // Check the library. Allow from own package and from allowed libraries
        int indexBetweenLibAndClass = className.lastIndexOf('/');
        if (indexBetweenLibAndClass != -1) {
            String library = className.substring(0, indexBetweenLibAndClass + 1);
            if (library.startsWith(packageName + '/')) return;
            if (!allowedLibraries.contains(library)) {
                log.error("Library `" + library + "` is prohibited (used in `" + className + "`)");
                System.exit(1);
            }
        }
        // Check the class
        if (disallowedClasses.contains(className)) {
            log.error("Class `" + className + "` is prohibited");
            System.exit(1);
        }
    }

    static void checkValidClasses(String[] classNames) {
        if (classNames == null) return;
        for (String className : classNames) checkValidClass(className);
    }

    static void checkValidMethod(String owner, String name) {
        // Get disallowed methods from the class `owner`
        List<String> methods = disallowedMethods.get(owner);
        if (methods == null) return;
        // Check if method `name` from class `owner` is valid
        if (methods.contains(name)) {
            log.error("Method `" + name + "` from `" + owner + "` is prohibited");
            System.exit(1);
        }
    }

    static void checkValidDescriptor(String descriptor) {
        if (descriptor == null) return;
        if (descriptor.isEmpty()) return;
        // Method descriptors have, first, the parameters descriptors between parenthesis, then the return descriptor
        if (descriptor.charAt(0) == '(') {
            int indexEndParameters = descriptor.lastIndexOf(')');
            // Recursive call to check parameters
            checkValidDescriptor(descriptor.substring(1, indexEndParameters));
            // Recursive call to check return
            checkValidDescriptor(descriptor.substring(indexEndParameters + 1));
        }
        // Validate descriptor
        for (int i = 0; i < descriptor.length(); i++) {
            // L indicates a class name comes next, ended by semicolon (other characters indicate basic types or array)
            // Check only the classes. Ignore the basic types
            if (descriptor.charAt(i) == 'L') {
                int indexEndClass = descriptor.indexOf(';', i);
                checkValidClass(descriptor.substring(i + 1, indexEndClass));
                i = indexEndClass;
            }
        }
    }

    static void checkValidSignature(String signature) {
        if (signature == null) return;
        if (signature.isEmpty()) return;
        SignatureReader reader = new SignatureReader(signature);
        reader.accept(new SignatureInstrumenter());
    }
}
