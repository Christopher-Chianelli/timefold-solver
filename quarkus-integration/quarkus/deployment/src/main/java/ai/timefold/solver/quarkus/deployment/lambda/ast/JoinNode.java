package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class JoinNode<Joiner_> extends AbstractConstraintStreamNode implements JoinerNode {
    final JoinerDefinition.Combined<?, ?> combinedJoiners;

    public JoinNode(JoinerDefinition.Combined<?, ?> combinedJoiners) {
        this.combinedJoiners = combinedJoiners;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        Class<?>[] leftParameters = getParentNodeList().get(0).getResultTupleTypes();
        Class<?>[] rightParameters = getParentNodeList().get(1).getResultTupleTypes();
        Class<?>[] out = new Class<?>[leftParameters.length + rightParameters.length];
        System.arraycopy(leftParameters, 0, out, 0, leftParameters.length);
        System.arraycopy(rightParameters, 0, out, leftParameters.length, rightParameters.length);
        return out;
    }

    @Override
    public int getResultTupleCardinality() {
        // input tuple cardinality will sum both parents cardinalities
        return getInputTupleCardinality();
    }

    @Override
    public boolean guaranteeDistinct() {
        return getParentNodeList().get(0).guaranteeDistinct() && getParentNodeList().get(1).guaranteeDistinct();
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof JoinNode<?> joinNode) {
            return Objects.equals(combinedJoiners, joinNode.combinedJoiners);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof JoinNode<?> joinNode) {
            return combinedJoiners.bytecodeEquals(joinNode.combinedJoiners, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        combinedJoiners.visitBytecode(bytecodeVisitor);
    }

    @Override
    public JoinerDefinition.Combined<?, ?> getCombinedJoiners() {
        return combinedJoiners;
    }

    public JoinNode<Joiner_> withParents(AbstractConstraintStreamNode left, AbstractConstraintStreamNode right) {
        setIdGenerator(left.getIdGenerator());
        getParentNodeList().add(left);
        getParentNodeList().add(right);
        return this;
    }
}
