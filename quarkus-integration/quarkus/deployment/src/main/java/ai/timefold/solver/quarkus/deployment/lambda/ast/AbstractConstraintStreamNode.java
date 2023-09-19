package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.objectweb.asm.MethodVisitor;

public sealed abstract class AbstractConstraintStreamNode
        permits AsConstraintNode, DistinctNode, ExpandNode, FilterNode, FlattenLastNode, SourceNode, GroupByNode, IfExistsNode,
        IfNotExistsNode, ImpactByConfigurableConstantNode, ImpactByConfigurableMatchWeightNode, ImpactByConstantNode,
        ImpactByMatchWeightNode, JoinNode, MapNode {
    private final List<AbstractConstraintStreamNode> parentNodeList = new ArrayList<>();
    AtomicLong idGenerator;
    long id;

    protected AbstractConstraintStreamNode() {
    }

    @SafeVarargs
    protected static <T> T[] expandArray(T[] original, T... extra) {
        T[] out = Arrays.copyOf(original, original.length + extra.length);
        System.arraycopy(extra, 0, out, original.length, extra.length);
        return out;
    }

    public abstract Class<?>[] getResultTupleTypes();

    public abstract int getResultTupleCardinality();

    public Class<?>[] getLeftParameterTypes() {
        return Stream.concat(Stream.of(int.class), Stream.of(parentNodeList.get(0).getResultTupleTypes()))
                .toArray(Class[]::new);
    }

    public Class<?>[] getRightParameterTypes() {
        return Stream.concat(Stream.of(int.class), Stream.of(parentNodeList.get(1).getResultTupleTypes()))
                .toArray(Class[]::new);
    }

    public Class<?>[] getOnlyParameterTypes() {
        return Stream.concat(Stream.of(int.class), Stream.of(parentNodeList.get(0).getResultTupleTypes()))
                .toArray(Class[]::new);
    }

    public abstract boolean guaranteeDistinct();

    protected abstract boolean hasSameProperties(AbstractConstraintStreamNode other);

    protected abstract boolean hasSameProperties(AbstractConstraintStreamNode other,
            Map<Object, List<Object>> lambdaToBytecodeMap);

    public abstract void visitBytecode(Consumer<Object> bytecodeVisitor);

    public abstract void writeBytecode(MethodVisitor methodVisitor,
            List<AbstractConstraintStreamNode> parents,
            Map<AbstractConstraintStreamNode, Integer> parentToSlot);

    protected static <T> boolean arrayIdentityEqual(T[] a, T[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    protected static <T> boolean bytecodeEqual(T a, T b, Map<Object, List<Object>> lambdaToBytecodeMap) {
        return a == b || (lambdaToBytecodeMap.get(a) != null &&
                lambdaToBytecodeMap.get(b) != null &&
                lambdaToBytecodeMap.get(a).equals(lambdaToBytecodeMap.get(b)));
    }

    protected static <T> boolean arrayBytecodeEqual(T[] a, T[] b, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (!bytecodeEqual(a[i], b[i], lambdaToBytecodeMap)) {
                return false;
            }
        }

        return true;
    }

    private boolean parentHasSameDownstreamTuples(AbstractConstraintStreamNode other) {
        if (parentNodeList.size() != other.parentNodeList.size()) {
            return false;
        }
        for (int i = 0; i < parentNodeList.size(); i++) {
            if (!parentNodeList.get(i).hasSameDownstreamTuples(other.parentNodeList.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean parentHasSameDownstreamTuples(AbstractConstraintStreamNode other,
            Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (parentNodeList.size() != other.parentNodeList.size()) {
            return false;
        }
        for (int i = 0; i < parentNodeList.size(); i++) {
            if (!parentNodeList.get(i).hasSameDownstreamTuples(other.parentNodeList.get(i), lambdaToBytecodeMap)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasSameDownstreamTuples(AbstractConstraintStreamNode other) {
        return hasSameProperties(other) && parentHasSameDownstreamTuples(other);
    }

    public boolean hasSameDownstreamTuples(AbstractConstraintStreamNode other,
            Map<Object, List<Object>> lambdaToBytecodeMap) {
        return hasSameProperties(other, lambdaToBytecodeMap) && parentHasSameDownstreamTuples(other, lambdaToBytecodeMap);
    }

    public int getInputTupleCardinality() {
        int inputSize = 0;
        for (AbstractConstraintStreamNode parent : parentNodeList) {
            inputSize += parent.getResultTupleCardinality();
        }
        return inputSize;
    }

    public <T extends AbstractConstraintStreamNode> T withChild(T child) {
        child.setIdGenerator(idGenerator);
        child.getParentNodeList().add(this);
        return child;
    }

    public List<AbstractConstraintStreamNode> getParentNodeList() {
        return parentNodeList;
    }

    protected AbstractConstraintStreamNode getOnlyParent() {
        return parentNodeList.get(0);
    }

    public long getId() {
        return id;
    }

    public AtomicLong getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(AtomicLong idGenerator) {
        // intentional identity equals
        this.idGenerator = idGenerator;
        this.id = idGenerator.getAndIncrement();
    }

    protected void visit(Consumer<? super AbstractConstraintStreamNode> visitor) {
        Queue<AbstractConstraintStreamNode> toVisit = new ArrayDeque<>();
        Set<AbstractConstraintStreamNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        toVisit.add(this);
        visited.add(this);
        while (!toVisit.isEmpty()) {
            AbstractConstraintStreamNode next = toVisit.poll();
            visitor.accept(next);
            for (AbstractConstraintStreamNode parent : next.parentNodeList) {
                if (!visited.contains(parent)) {
                    toVisit.add(parent);
                    visited.add(parent);
                }
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
