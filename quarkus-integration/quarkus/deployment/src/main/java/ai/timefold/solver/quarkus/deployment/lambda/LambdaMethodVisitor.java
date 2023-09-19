package ai.timefold.solver.quarkus.deployment.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LambdaMethodVisitor extends MethodVisitor {
    final IdentityHashMap<Label, Integer> labelToId = new IdentityHashMap<>();
    final List<Object> methodBytecode = new ArrayList<>();
    boolean canBeCompared = true;
    boolean isStatic;

    protected LambdaMethodVisitor(boolean isStatic, int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        this.isStatic = isStatic;
    }

    public List<Object> getMethodBytecode() {
        return methodBytecode;
    }

    private record Instruction(int opcode) {
    }

    public void visitInsn(final int opcode) {
        super.visitInsn(opcode);
        methodBytecode.add(new Instruction(opcode));
    }

    record IntInstruction(int opcode, int operand) {
    }

    public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        methodBytecode.add(new IntInstruction(opcode, operand));
    }

    public void visitVarInsn(final int opcode, final int varIndex) {
        super.visitVarInsn(opcode, varIndex);
        if (!isStatic && opcode == Opcodes.ALOAD && varIndex == 0) {
            canBeCompared = false;
        }
        methodBytecode.add(new IntInstruction(opcode, varIndex));
    }

    record TypeInstruction(int opcode, String type) {
    }

    public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, type);
        methodBytecode.add(new TypeInstruction(opcode, type));
    }

    record FieldInstruction(int opcode, String owner, String name, String descriptor) {
    }

    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        methodBytecode.add(new FieldInstruction(opcode, owner, name, descriptor));
    }

    record MethodInstruction(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    }

    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        methodBytecode.add(new MethodInstruction(opcode, owner, name, descriptor, isInterface));
    }

    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        // A lambda inside a lambda is odd, and invoke dynamic can be used for things other than lambdas
        canBeCompared = false;
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        methodBytecode.add(new IntInstruction(opcode, labelToId.computeIfAbsent(label, ignored -> labelToId.size())));
    }

    public void visitLabel(final Label label) {
        super.visitLabel(label);
        labelToId.computeIfAbsent(label, ignored -> labelToId.size());
    }

    // -----------------------------------------------------------------------------------------------
    // Special instructions
    // -----------------------------------------------------------------------------------------------

    record LdcInstruction(Object value) {
    }

    public void visitLdcInsn(final Object value) {
        super.visitLdcInsn(value);
        methodBytecode.add(new LdcInstruction(value));
    }

    record IncrementInstruction(int varIndex, int increment) {
    }

    public void visitIincInsn(final int varIndex, final int increment) {
        super.visitIincInsn(varIndex, increment);
        methodBytecode.add(new IncrementInstruction(varIndex, increment));
    }

    record TableSwitchInstruction(int min, int max, int defaultHandler, List<Integer> handlers) {
    }

    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        methodBytecode.add(new TableSwitchInstruction(
                min, max,
                labelToId.computeIfAbsent(dflt, ignored -> labelToId.size()),
                Arrays.stream(labels).map(label -> labelToId.computeIfAbsent(label, ignored -> labelToId.size())).toList()));
    }

    record LookupSwitchInstruction(int defaultHandler, List<Integer> keys, List<Integer> labels) {
    }

    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        methodBytecode.add(new LookupSwitchInstruction(
                labelToId.computeIfAbsent(dflt, ignored -> labelToId.size()),
                Arrays.stream(keys).boxed().toList(),
                Arrays.stream(labels).map(label -> labelToId.computeIfAbsent(label, ignored -> labelToId.size())).toList()));
    }

    record MultiANewArrayInstruction(String descriptor, int numDimensions) {
    }

    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        methodBytecode.add(new MultiANewArrayInstruction(descriptor, numDimensions));
    }

    // -----------------------------------------------------------------------------------------------
    // Exceptions table entries, debug information, max stack and max locals
    // -----------------------------------------------------------------------------------------------

    record TryCatchBlock(int start, int end, int handler, String type) {
    }

    public void visitTryCatchBlock(
            final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        methodBytecode.add(new TryCatchBlock(
                labelToId.computeIfAbsent(start, ignored -> labelToId.size()),
                labelToId.computeIfAbsent(end, ignored -> labelToId.size()),
                labelToId.computeIfAbsent(handler, ignored -> labelToId.size()),
                type));
    }
}
