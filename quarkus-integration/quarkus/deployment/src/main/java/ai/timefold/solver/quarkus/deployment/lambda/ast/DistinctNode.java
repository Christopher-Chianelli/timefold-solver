package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DistinctNode extends AbstractConstraintStreamNode {

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
        return true;
    }

    @Override
    public boolean hasSameProperties(AbstractConstraintStreamNode other) {
        return other instanceof DistinctNode;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        return other instanceof DistinctNode;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
    }
}
