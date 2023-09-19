package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class IfNotExistsNode<Joiner_> extends AbstractConstraintStreamNode implements JoinerNode {
    final JoinerDefinition.Combined<?, ?> combinedJoiners;

    public IfNotExistsNode(JoinerDefinition.Combined<?, ?> combinedJoiners) {
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
        if (other instanceof IfNotExistsNode<?> ifNotExistNode) {
            return Objects.equals(combinedJoiners, ifNotExistNode.combinedJoiners);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof IfNotExistsNode<?> ifNotExistNode) {
            combinedJoiners.bytecodeEquals(ifNotExistNode.combinedJoiners, lambdaToBytecodeMap);
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

    public IfNotExistsNode<Joiner_> withParents(AbstractConstraintStreamNode left, AbstractConstraintStreamNode right) {
        setIdGenerator(left.getIdGenerator());
        getParentNodeList().add(left);
        getParentNodeList().add(right);
        return this;
    }
}
