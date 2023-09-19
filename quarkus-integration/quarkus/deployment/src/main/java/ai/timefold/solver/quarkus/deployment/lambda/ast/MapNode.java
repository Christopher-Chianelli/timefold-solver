package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MapNode<Mapper_> extends AbstractConstraintStreamNode {
    final Mapper_[] mappers;

    @SafeVarargs
    public MapNode(Mapper_... mappers) {
        this.mappers = mappers;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        Class<?>[] out = new Class<?>[mappers.length];
        Arrays.fill(out, Object.class);
        return out;
    }

    @Override
    public int getResultTupleCardinality() {
        return mappers.length;
    }

    @Override
    public boolean guaranteeDistinct() {
        return false;
    }

    public Mapper_[] getMappers() {
        return mappers;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof MapNode<?> mapNode) {
            return arrayIdentityEqual(mappers, mapNode.mappers);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof MapNode<?> mapNode) {
            return arrayBytecodeEqual(mappers, mapNode.mappers, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        for (Mapper_ mapper : mappers) {
            bytecodeVisitor.accept(mapper);
        }
    }
}
