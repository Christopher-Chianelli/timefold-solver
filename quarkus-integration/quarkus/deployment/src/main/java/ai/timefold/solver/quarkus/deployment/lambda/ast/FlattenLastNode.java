package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class FlattenLastNode<Flattener_> extends AbstractConstraintStreamNode {
    final Flattener_ flattener;

    public FlattenLastNode(Flattener_ flattener) {
        this.flattener = flattener;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        Class<?>[] out = getOnlyParent().getResultTupleTypes();
        out[out.length - 1] = Object.class;
        return out;
    }

    @Override
    public int getResultTupleCardinality() {
        return getInputTupleCardinality();
    }

    @Override
    public boolean guaranteeDistinct() {
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof FlattenLastNode<?> flattenLastNode) {
            return flattener == flattenLastNode.flattener;
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof FlattenLastNode<?> flattenLastNode) {
            return bytecodeEqual(flattener, flattenLastNode.flattener, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        bytecodeVisitor.accept(flattener);
    }

    public Flattener_ getFlattener() {
        return flattener;
    }
}
