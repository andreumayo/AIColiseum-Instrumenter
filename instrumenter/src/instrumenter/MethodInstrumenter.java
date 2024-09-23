package instrumenter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Class to instrument methods.
 * <p>
 * It goes through all the ASM instructions in a method. Each instruction visit
 * calls a different visit method, depending on the instruction (e.g., jump,
 * call a method, access a field...).
 * <p>
 * Pretty much all instruction are instrumented, meaning that a call to
 * incrementing the bytecodes count is injected right afterward.
 * <p>
 * Whenever classes or methods are used, they are checked.
 */
class MethodInstrumenter extends MethodVisitor {
    private static final Logger log = LogManager.getRootLogger();

    MethodInstrumenter(MethodVisitor methodWriter) {
        super(Opcodes.ASM9, methodWriter);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        log.debug("MethodInstrumenter::visitFieldInsn " + opcode + " " + owner + " " + name + " " + descriptor);
        Instrumenter.checkValidClass(owner);
        Instrumenter.checkValidDescriptor(descriptor);
        super.visitFieldInsn(opcode, owner, name, descriptor);
        incrementBytecodeCounter();
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        log.debug("MethodInstrumenter::visitIincInsn");
        super.visitIincInsn(varIndex, increment);
        incrementBytecodeCounter();
    }

    @Override
    public void visitInsn(int opcode) {
        log.debug("MethodInstrumenter::visitInsn " + opcode);
        super.visitInsn(opcode);
        incrementBytecodeCounter();
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        log.debug("MethodInstrumenter::visitIntInsn " + opcode + " " + operand);
        super.visitIntInsn(opcode, operand);
        incrementBytecodeCounter();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bmh, Object... bma) {
        log.debug("MethodInstrumenter::visitInvokeDynamicInsn " + name + " " + descriptor + " " + bmh.getOwner() + " "
                + bmh.getName() + " " + bmh.getDesc());
        Instrumenter.checkValidDescriptor(descriptor);
        Instrumenter.checkValidMethod(bmh.getOwner(), bmh.getName());
        super.visitInvokeDynamicInsn(name, descriptor, bmh, bma);
        incrementBytecodeCounter();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        log.debug("MethodInstrumenter::visitJumpInsn " + opcode);
        super.visitJumpInsn(opcode, label);
        incrementBytecodeCounter();
    }

    @Override
    public void visitLabel(Label label) {
        log.debug("MethodInstrumenter::visitLabel");
        super.visitLabel(label);
        incrementBytecodeCounter();
    }

    @Override
    public void visitLdcInsn(Object value) {
        log.debug("MethodInstrumenter::visitLdcInsn");
        super.visitLdcInsn(value);
        incrementBytecodeCounter();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        log.debug("MethodInstrumenter::visitLookupSwitchInsn");
        super.visitLookupSwitchInsn(dflt, keys, labels);
        incrementBytecodeCounter();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        log.debug("MethodInstrumenter::visitMethodInsn[deprecated] " + opcode + " " + owner + " " + name + " " + descriptor);
        Instrumenter.checkValidClass(owner);
        Instrumenter.checkValidMethod(owner, name);
        Instrumenter.checkValidDescriptor(descriptor);
        super.visitMethodInsn(opcode, owner, name, descriptor);
        incrementBytecodeCounter(owner + "/" + name);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        log.debug("MethodInstrumenter::visitMethodInsn " + opcode + " " + owner + " " + name + " " + descriptor);
        Instrumenter.checkValidClass(owner);
        Instrumenter.checkValidMethod(owner, name);
        Instrumenter.checkValidDescriptor(descriptor);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        incrementBytecodeCounter(owner + "/" + name);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        log.debug("MethodInstrumenter::visitMultiANewArrayInsn " + descriptor);
        Instrumenter.checkValidDescriptor(descriptor);
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        incrementBytecodeCounter();
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        log.debug("MethodInstrumenter::visitTableSwitchInsn");
        super.visitTableSwitchInsn(min, max, dflt, labels);
        incrementBytecodeCounter();
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        log.debug("MethodInstrumenter::visitTryCatchBlock " + type);
        Instrumenter.checkValidClass(type);
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        log.debug("MethodInstrumenter::visitTypeInsn " + opcode + " " + type);
        Instrumenter.checkValidClass(type);
        super.visitTypeInsn(opcode, type);
        incrementBytecodeCounter();
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        log.debug("MethodInstrumenter::visitVarInsn " + opcode);
        super.visitVarInsn(opcode, varIndex);
        incrementBytecodeCounter();
    }

    private void incrementBytecodeCounter(int bytecodes) {
        super.visitLdcInsn(bytecodes);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "pirates/threading/ThreadManager", "addBytecodes",
                "(I)V", false);
    }

    private void incrementBytecodeCounter() {
        incrementBytecodeCounter(1);
    }

    private void incrementBytecodeCounter(String methodName) {
        incrementBytecodeCounter(Instrumenter.getMethodBytecodeCost(methodName));
    }
}
