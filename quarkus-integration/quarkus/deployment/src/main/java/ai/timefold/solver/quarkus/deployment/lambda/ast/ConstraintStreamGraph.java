package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;

public class ConstraintStreamGraph {
    final Set<AbstractConstraintStreamNode> canonicalNodeSet;
    final List<SourceNode> rootNodeList;
    final List<AsConstraintNode> leafNodeList;
    final Map<Long, List<ChildNode>> idToChildren;

    private ConstraintStreamGraph(Set<AbstractConstraintStreamNode> canonicalNodeSet,
            List<SourceNode> rootNodeList,
            List<AsConstraintNode> leafNodeList,
            Map<Long, List<ChildNode>> idToChildren) {
        this.canonicalNodeSet = canonicalNodeSet;
        this.rootNodeList = rootNodeList;
        this.leafNodeList = leafNodeList;
        this.idToChildren = idToChildren;
    }

    public static <Solution_> ConstraintStreamGraph fromConstraints(List<AsmConstraint<Solution_>> constraintList) {
        return fromConstraints(constraintList, AbstractConstraintStreamNode::hasSameDownstreamTuples);
    }

    public static <Solution_> ConstraintStreamGraph fromConstraints(List<AsmConstraint<Solution_>> constraintList,
            Map<Object, List<Object>> lambdaToBytecodeMap) {
        return fromConstraints(constraintList, (a, b) -> a.hasSameDownstreamTuples(b, lambdaToBytecodeMap));
    }

    public static <Solution_> ConstraintStreamGraph fromConstraints(List<AsmConstraint<Solution_>> constraintList,
            BiPredicate<AbstractConstraintStreamNode, AbstractConstraintStreamNode> nodeEqualityFunction) {
        final Set<AbstractConstraintStreamNode> allNodeSet = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<AbstractConstraintStreamNode> canonicalNodeSet = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Long, NavigableSet<Long>> equalivantNodeSetMap = new HashMap<>();
        final Map<Long, AbstractConstraintStreamNode> nodeIdToNodeMap = new HashMap<>();
        final Map<Long, List<AbstractConstraintStreamNode>> nodeIdToChildrenMap = new HashMap<>();
        final List<SourceNode> rootNodeList = new ArrayList<>();
        final List<AsConstraintNode> leafNodeList = new ArrayList<>();
        final Map<Long, List<ChildNode>> idToChildren = new HashMap<>();

        for (AsmConstraint<?> constraint : constraintList) {
            constraint.getTerminalNode().visit(node -> {
                allNodeSet.add(node);
                nodeIdToNodeMap.put(node.getId(), node);
                TreeSet<Long> idSet = new TreeSet<>();
                idSet.add(node.getId());
                equalivantNodeSetMap.put(node.getId(), idSet);
                for (AbstractConstraintStreamNode parent : node.getParentNodeList()) {
                    nodeIdToChildrenMap.computeIfAbsent(parent.getId(), ignored -> new ArrayList<>()).add(node);
                }
            });
        }

        for (AbstractConstraintStreamNode left : allNodeSet) {
            for (AbstractConstraintStreamNode right : allNodeSet) {
                if (left.getId() != right.getId() && nodeEqualityFunction.test(left, right)) {
                    if (left.getId() < right.getId()) {
                        NavigableSet<Long> idSet = equalivantNodeSetMap.get(left.getId());
                        idSet.addAll(equalivantNodeSetMap.get(right.getId()));
                        equalivantNodeSetMap.put(right.getId(), idSet);
                    } else {
                        NavigableSet<Long> idSet = equalivantNodeSetMap.get(right.getId());
                        idSet.addAll(equalivantNodeSetMap.get(left.getId()));
                        equalivantNodeSetMap.put(left.getId(), idSet);
                    }
                }
            }
        }

        addChildren: for (AbstractConstraintStreamNode node : allNodeSet) {
            AbstractConstraintStreamNode canonicalNode = nodeIdToNodeMap.get(equalivantNodeSetMap.get(node.getId()).first());
            if (nodeIdToChildrenMap.get(node.getId()) != null) {
                Set<AbstractConstraintStreamNode> canonicalChildren = nodeIdToChildrenMap.get(node.getId())
                        .stream().map(child -> nodeIdToNodeMap.get(equalivantNodeSetMap.get(child.getId()).first()))
                        .collect(Collectors.toSet());
                List<ChildNode> existingChildrenList =
                        idToChildren.computeIfAbsent(canonicalNode.getId(), ignored -> new ArrayList<>());

                for (AbstractConstraintStreamNode canonicalChild : canonicalChildren) {
                    for (ChildNode childNode : existingChildrenList) {
                        if (childNode.node == canonicalChild) {
                            continue addChildren;
                        }
                    }
                    if (canonicalChild instanceof JoinerNode) {
                        long canonicalLeftParentId =
                                equalivantNodeSetMap.get(canonicalChild.getParentNodeList().get(0).getId()).first();
                        long canonicalRightParentId =
                                equalivantNodeSetMap.get(canonicalChild.getParentNodeList().get(1).getId()).first();
                        if (canonicalNode.getId() == canonicalLeftParentId && canonicalNode.getId() == canonicalRightParentId) {
                            existingChildrenList.add(new ChildNode(ParentKind.LEFT_RIGHT_PARENT, canonicalChild));
                        } else if (canonicalNode.getId() == canonicalLeftParentId) {
                            existingChildrenList.add(new ChildNode(ParentKind.LEFT_PARENT, canonicalChild));
                        } else {
                            existingChildrenList.add(new ChildNode(ParentKind.RIGHT_PARENT, canonicalChild));
                        }
                    } else {
                        existingChildrenList.add(new ChildNode(ParentKind.ONLY_PARENT, canonicalChild));
                    }
                }
            }
        }

        for (AbstractConstraintStreamNode node : allNodeSet) {
            AbstractConstraintStreamNode canonicalNode = nodeIdToNodeMap.get(equalivantNodeSetMap.get(node.getId()).first());
            canonicalNodeSet.add(canonicalNode);
        }

        for (AbstractConstraintStreamNode node : canonicalNodeSet) {
            if (node instanceof SourceNode forEachNode) {
                rootNodeList.add(forEachNode);
            } else if (node instanceof AsConstraintNode asConstraintNode) {
                leafNodeList.add(asConstraintNode);
            }
        }

        return new ConstraintStreamGraph(canonicalNodeSet, rootNodeList, leafNodeList, idToChildren);
    }

    public List<ChildNode> getChildren(AbstractConstraintStreamNode parent) {
        return idToChildren.getOrDefault(parent.getId(), Collections.emptyList());
    }

    public List<AbstractConstraintStreamNode> getParents(AbstractConstraintStreamNode child) {
        boolean parentsAreCanonical = true;
        for (AbstractConstraintStreamNode parent : child.getParentNodeList()) {
            if (idToChildren.get(parent.getId()) == null) {
                parentsAreCanonical = false;
                break;
            }
        }
        if (parentsAreCanonical) {
            return child.getParentNodeList();
        }

        // Need to find canonical parents in canonicalNodeSet
        List<AbstractConstraintStreamNode> parentList = new ArrayList<>(child.getParentNodeList().size());
        for (AbstractConstraintStreamNode potentialParent : canonicalNodeSet) {
            for (ChildNode maybeChild : idToChildren.getOrDefault(potentialParent.getId(), Collections.emptyList())) {
                if (maybeChild.node == child) {
                    parentList.add(potentialParent);
                }
            }
        }
        return parentList;
    }

    public int getNestingLevel(AbstractConstraintStreamNode node) {
        if (node.getParentNodeList().isEmpty()) {
            return 0;
        }
        int max = 0;
        for (AbstractConstraintStreamNode parent : getParents(node)) {
            max = Math.max(max, 1 + getNestingLevel(parent));
        }
        return max;
    }

    public void visitOnlyRoots(Consumer<? super SourceNode> visitor) {
        for (SourceNode root : rootNodeList) {
            visitor.accept(root);
        }
    }

    public Set<AbstractConstraintStreamNode> getRequiredNodesForConstraint(AsConstraintNode asConstraintNode) {
        LinkedHashSet<AbstractConstraintStreamNode> requiredNodeSet = new LinkedHashSet<>();
        ArrayDeque<AbstractConstraintStreamNode> toVisitQueue = new ArrayDeque<>();
        toVisitQueue.push(asConstraintNode);
        requiredNodeSet.add(asConstraintNode);

        while (!toVisitQueue.isEmpty()) {
            AbstractConstraintStreamNode toVisit = toVisitQueue.pop();
            for (AbstractConstraintStreamNode parent : getParents(toVisit)) {
                if (!requiredNodeSet.contains(parent)) {
                    requiredNodeSet.add(parent);
                    toVisitQueue.push(parent);
                }
            }
        }
        return requiredNodeSet;
    }

    public void visitAll(Consumer<? super AbstractConstraintStreamNode> visitor) {
        for (AbstractConstraintStreamNode node : canonicalNodeSet) {
            visitor.accept(node);
        }
    }

    public record ChildNode(ParentKind parentKind, AbstractConstraintStreamNode node) {
    }

    public enum ParentKind {
        ONLY_PARENT,
        LEFT_PARENT,
        RIGHT_PARENT,
        LEFT_RIGHT_PARENT
    }
}
