package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GroupByNode<Grouper_, Collector_> extends AbstractConstraintStreamNode {
    final Grouper_[] groupers;
    final Collector_[] collectors;

    public GroupByNode(Grouper_[] groupers, Collector_[] collectors) {
        this.groupers = groupers;
        this.collectors = collectors;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        Class<?>[] out = new Class<?>[groupers.length + collectors.length];
        Arrays.fill(out, Object.class);
        return out;
    }

    @Override
    public int getResultTupleCardinality() {
        return groupers.length + collectors.length;
    }

    @Override
    public boolean guaranteeDistinct() {
        return true;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof GroupByNode<?, ?> groupByNode) {
            // TODO: Check if collectors are equal?
            return arrayIdentityEqual(groupers, groupByNode.groupers) &&
                    Arrays.equals(collectors, groupByNode.collectors);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof GroupByNode<?, ?> groupByNode) {
            // TODO: Check if collectors are equal?
            return arrayBytecodeEqual(groupers, groupByNode.groupers, lambdaToBytecodeMap) &&
                    Arrays.equals(collectors, groupByNode.collectors);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        for (Grouper_ grouper : groupers) {
            bytecodeVisitor.accept(grouper);
        }
    }

    public Grouper_[] getGroupers() {
        return groupers;
    }

    public Collector_[] getCollectors() {
        return collectors;
    }

    public static GroupByNodeBuilder builder() {
        return new GroupByNodeBuilder();
    }

    public static class GroupByNodeBuilder {
        Object[] groupers = new Object[] {};
        Object[] collectors = new Object[] {};

        private GroupByNodeBuilder() {
        }

        public GroupByNode<?, ?> build() {
            return new GroupByNode<>(groupers, collectors);
        }

        public GroupByNodeBuilder withGroupers(Object... groupers) {
            this.groupers = groupers;
            return this;
        }

        public GroupByNodeBuilder withCollectors(Object... collectors) {
            this.collectors = collectors;
            return this;
        }
    }
}
