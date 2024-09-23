package instrumenter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * Class to instrument signatures.
 * <p>
 * Signatures are used, basically, when there are generic types. While visiting
 * a signature, whenever a class is used, it is checked.
 */
class SignatureInstrumenter extends SignatureVisitor {
    private static final Logger log = LogManager.getRootLogger();

    SignatureInstrumenter() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visitClassType(String name) {
        log.debug("SignatureInstrumenter::visitClassType " + name);
        Instrumenter.checkValidClass(name);
    }

    @Override
    public void visitInnerClassType(String name) {
        log.debug("SignatureInstrumenter::visitInnerClassType " + name);
        Instrumenter.checkValidClass(name);
    }
}
