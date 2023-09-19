package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class IfExistsNode<Joiner_> extends AbstractConstraintStreamNode implements JoinerNode {
    final JoinerDefinition.Combined<?, ?> combinedJoiners;

    public IfExistsNode(JoinerDefinition.Combined<?, ?> combinedJoiners) {
        this.combinedJoiners = combinedJoiners;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        return getParentNodeList().get(0).getResultTupleTypes();
    }

    @Override
    public int getResultTupleCardinality() {
        return getParentNodeList().get(0).getResultTupleCardinality();
    }

    @Override
    public boolean guaranteeDistinct() {
        return getParentNodeList().get(0).guaranteeDistinct();
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof IfExistsNode<?> ifExistsNode) {
            return Objects.equals(combinedJoiners, ifExistsNode.combinedJoiners);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof IfExistsNode<?> ifExistsNode) {
            return combinedJoiners.bytecodeEquals(ifExistsNode.combinedJoiners, lambdaToBytecodeMap);
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

    public IfExistsNode<Joiner_> withParents(AbstractConstraintStreamNode left, AbstractConstraintStreamNode right) {
        setIdGenerator(left.getIdGenerator());
        getParentNodeList().add(left);
        getParentNodeList().add(right);
        return this;
    }
}
