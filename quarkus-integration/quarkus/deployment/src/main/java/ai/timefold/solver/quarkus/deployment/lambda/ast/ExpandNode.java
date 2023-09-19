package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ExpandNode<Expander_> extends AbstractConstraintStreamNode {
    final Expander_[] expanders;

    @SafeVarargs
    public ExpandNode(Expander_... expanders) {
        this.expanders = expanders;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        Class<?>[] out = Arrays.copyOf(getOnlyParent().getResultTupleTypes(),
                getOnlyParent().getResultTupleCardinality() + expanders.length);
        for (int i = 0; i < expanders.length; i++) {
            out[getOnlyParent().getResultTupleCardinality() + i] = Object.class;
        }
        return out;
    }

    @Override
    public int getResultTupleCardinality() {
        return getInputTupleCardinality() + expanders.length;
    }

    @Override
    public boolean guaranteeDistinct() {
        return getOnlyParent().guaranteeDistinct();
    }

    @Override
    public boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof ExpandNode<?> expandNode) {
            return arrayIdentityEqual(expanders, expandNode.expanders);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof ExpandNode<?> expandNode) {
            return arrayBytecodeEqual(expanders, expandNode.expanders, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        for (Expander_ expander : expanders) {
            bytecodeVisitor.accept(expander);
        }
    }

    public Expander_[] getExpanders() {
        return expanders;
    }
}
