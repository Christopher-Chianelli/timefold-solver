package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SourceNode extends AbstractConstraintStreamNode {
    final Class<?> sourceClass;

    public SourceNode(Class<?> sourceClass) {
        this.sourceClass = sourceClass;
    }

    @Override
    public Class<?>[] getOnlyParameterTypes() {
        return new Class<?>[] { Object.class };
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        return new Class<?>[] { sourceClass };
    }

    @Override
    public int getResultTupleCardinality() {
        return 1;
    }

    @Override
    public boolean guaranteeDistinct() {
        return true;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof SourceNode forEachNode) {
            return sourceClass.equals(forEachNode.sourceClass);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof SourceNode forEachNode) {
            return sourceClass.equals(forEachNode.sourceClass);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {

    }

    public Class<?> getSourceClass() {
        return sourceClass;
    }
}
