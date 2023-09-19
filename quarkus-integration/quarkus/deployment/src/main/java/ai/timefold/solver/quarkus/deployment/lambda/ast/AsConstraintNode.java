package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class AsConstraintNode extends AbstractConstraintStreamNode {
    final String constraintPackage;
    final String constraintName;
    final Object justificationMapping;
    final Object indictmentMapping;
    AsmConstraint<?> asmConstraint;

    public AsConstraintNode(String constraintPackage,
            String constraintName,
            Object justificationMapping,
            Object indictmentMapping) {
        this.constraintPackage = constraintPackage;
        this.constraintName = constraintName;
        this.justificationMapping = justificationMapping;
        this.indictmentMapping = indictmentMapping;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        return new Class[] {};
    }

    @Override
    public int getResultTupleCardinality() {
        return 0;
    }

    @Override
    public boolean guaranteeDistinct() {
        return getOnlyParent().guaranteeDistinct();
    }

    @Override
    public boolean hasSameProperties(AbstractConstraintStreamNode other) {
        // All terminal nodes are unique
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        // All terminal nodes are unique
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        bytecodeVisitor.accept(justificationMapping);
        bytecodeVisitor.accept(indictmentMapping);
    }

    @Override
    public void writeBytecode(MethodVisitor methodVisitor, List<AbstractConstraintStreamNode> parents,
            Map<AbstractConstraintStreamNode, Integer> parentToSlot) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, parentToSlot.get(parents.get(0)));
    }

    public String getConstraintPackage() {
        return constraintPackage;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public <Solution_> Score<?> getConstraintWeight(SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ workingSolution) {
        return getSourceImpactNode().getConstraintWeight(constraintPackage, constraintName,
                solutionDescriptor,
                workingSolution);
    }

    public ImpactNode getSourceImpactNode() {
        return ((ImpactNode) getParentNodeList().get(0));
    }

    public AsmConstraint<?> getAsmConstraint() {
        return asmConstraint;
    }

    public void setAsmConstraint(AsmConstraint<?> asmConstraint) {
        this.asmConstraint = asmConstraint;
    }

    public Object getJustificationMapping() {
        return justificationMapping;
    }

    public Object getIndictmentMapping() {
        return indictmentMapping;
    }

}
