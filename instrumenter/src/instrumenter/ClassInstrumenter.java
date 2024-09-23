package instrumenter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.*;

/**
 * Class to instrument classes.
 * <p>
 * Methods:
 * - {@code visit}: Visits the class header.
 * - {@code visitMethod}: Visits the methods header and its content. Custom
 * check for non-public constructor for the unit controller class.
 * - {@code visitField}: Visits the fields. Custom check for {@code static}
 * fields.
 * - {@code visitInnerClass}: Visits information about an inner class. Not sure what this is.
 * - {@code visitOuterClass}: Visits the enclosing class of the class. Not sure what this is.
 * <p>
 * Common parameters:
 * - The access flags. E.g., public or static.
 * - The name of the class, method or field.
 * - The descriptor. For a method, this is basically this is the type of the
 * parameters and the return value.
 * - The signature. Basically to manage generic types, if used.
 */
class ClassInstrumenter extends ClassVisitor {
    private static final Logger log = LogManager.getRootLogger();

    private static String className = null;

    ClassInstrumenter(ClassWriter classWriter) {
        super(Opcodes.ASM9, classWriter);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        log.debug("ClassInstrumenter::visit " + name + " " + signature + " " + superName);
        Instrumenter.checkValidClass(name);
        Instrumenter.checkValidSignature(signature);
        Instrumenter.checkValidClass(superName);
        Instrumenter.checkValidClasses(interfaces);
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        log.debug("ClassInstrumenter::visitMethod " + name + " " + descriptor + " " + signature);
        // Check UnitController constructor
        if (className.equals(Instrumenter.packageName + "/UnitController") && name.equals("<init>")) {
            if ((access & Opcodes.ACC_PUBLIC) == 0) {
                log.error("It is prohibited to implement a non-public UnitController constructor. Instead of " +
                        "implementing a constructor, it is recommended to use the method `public void init(Unit unit)" +
                        "`");
                System.exit(1);
            }
        }
        // Check other parameters
        Instrumenter.checkValidDescriptor(descriptor);
        Instrumenter.checkValidSignature(signature);
        Instrumenter.checkValidClasses(exceptions);
        MethodVisitor methodWriter = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodInstrumenter(methodWriter);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        log.debug("ClassInstrumenter::visitField " + name + " " + descriptor + " " + signature);
        // Check if the class attribute is static
        if ((access & Opcodes.ACC_STATIC) != 0) {
            // Skip switches. They are identified as static fields, but they are not
            if (!name.startsWith("$SwitchMap$")) {
                log.error("Static attributes are prohibited (attribute `" + name + "` in class `" + className + "`)");
                System.exit(1);
            }
        }
        // Check other parameters
        Instrumenter.checkValidDescriptor(descriptor);
        Instrumenter.checkValidSignature(signature);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        log.debug("ClassInstrumenter::visitOuterClass " + owner + " " + name + " " + descriptor);
        Instrumenter.checkValidClass(owner);
        Instrumenter.checkValidClass(name);
        Instrumenter.checkValidDescriptor(descriptor);
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        log.debug("ClassInstrumenter::visitInnerClass " + name + " " + outerName + " " + innerName);
        Instrumenter.checkValidClass(name);
        Instrumenter.checkValidClass(outerName);
        Instrumenter.checkValidClass(innerName);
        super.visitInnerClass(name, outerName, innerName, access);
    }
}
