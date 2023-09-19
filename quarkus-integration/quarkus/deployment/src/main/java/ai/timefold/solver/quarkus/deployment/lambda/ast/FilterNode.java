package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class FilterNode<Predicate_> extends AbstractConstraintStreamNode {
    final Predicate_ predicate;

    public FilterNode(Predicate_ predicate) {
        this.predicate = predicate;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        return getOnlyParent().getResultTupleTypes();
    }

    @Override
    public int getResultTupleCardinality() {
        return getInputTupleCardinality();
    }

    @Override
    public boolean guaranteeDistinct() {
        return getOnlyParent().guaranteeDistinct();
    }

    @Override
    public boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof FilterNode<?> otherFilter) {
            return predicate == otherFilter.predicate;
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof FilterNode<?> otherFilter) {
            return bytecodeEqual(predicate, otherFilter.predicate, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        bytecodeVisitor.accept(predicate);
    }

    public Predicate_ getPredicate() {
        return predicate;
    }
}
