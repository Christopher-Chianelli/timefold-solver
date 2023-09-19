package ai.timefold.solver.quarkus.deployment.lambda.api;

import ai.timefold.solver.constraint.streams.common.RetrievalSemantics;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.bi.InnerBiConstraintStream;
import ai.timefold.solver.constraint.streams.common.quad.InnerQuadConstraintStream;
import ai.timefold.solver.constraint.streams.common.tri.InnerTriConstraintStream;
import ai.timefold.solver.constraint.streams.common.uni.InnerUniConstraintStream;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraintFactory;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.IfExistsNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.IfNotExistsNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ImpactByConfigurableConstantNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ImpactByConstantNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.JoinNode;

public class AbstractAsmConstraintStream implements ConstraintStream {
    private final AsmConstraintFactory<?> constraintFactory;
    private final AbstractConstraintStreamNode leafNode;
    private final RetrievalSemantics retrievalSemantics;
    private final boolean guaranteeDistinct;

    protected AbstractAsmConstraintStream(AsmConstraintFactory<?> constraintFactory,
            AbstractConstraintStreamNode leafNode,
            RetrievalSemantics retrievalSemantics) {
        this.constraintFactory = constraintFactory;
        this.leafNode = leafNode;
        this.retrievalSemantics = retrievalSemantics;
        this.guaranteeDistinct = leafNode.guaranteeDistinct();
    }

    protected AbstractAsmConstraintStream(AbstractAsmConstraintStream parent,
            JoinNode<?> ast) {
        this.constraintFactory = parent.constraintFactory;
        this.leafNode = ast;
        this.retrievalSemantics = parent.retrievalSemantics;
        this.guaranteeDistinct = leafNode.guaranteeDistinct();
    }

    protected AbstractAsmConstraintStream(AbstractAsmConstraintStream parent,
            IfExistsNode<?> ast) {
        this.constraintFactory = parent.constraintFactory;
        this.leafNode = ast;
        this.retrievalSemantics = parent.retrievalSemantics;
        this.guaranteeDistinct = leafNode.guaranteeDistinct();
    }

    protected AbstractAsmConstraintStream(AbstractAsmConstraintStream parent,
            IfNotExistsNode<?> ast) {
        this.constraintFactory = parent.constraintFactory;
        this.leafNode = ast;
        this.retrievalSemantics = parent.retrievalSemantics;
        this.guaranteeDistinct = leafNode.guaranteeDistinct();
    }

    protected AbstractAsmConstraintStream(AbstractAsmConstraintStream parent,
            AbstractConstraintStreamNode child) {
        this.constraintFactory = parent.constraintFactory;
        this.leafNode = parent.withChild(child);
        this.retrievalSemantics = parent.retrievalSemantics;
        this.guaranteeDistinct = leafNode.guaranteeDistinct();
    }

    protected AbstractConstraintStreamNode withChild(AbstractConstraintStreamNode child) {
        return leafNode.withChild(child);
    }

    protected <A> AsmUniConstraintStream<A> createUni(Class<A> sourceClass) {
        return switch (retrievalSemantics) {
            case STANDARD -> constraintFactory.forEach(sourceClass);
            case LEGACY -> constraintFactory.from(sourceClass);
        };
    }

    protected <A> AsmUniConstraintStream<A> createIfExistUni(Class<A> sourceClass) {
        return switch (retrievalSemantics) {
            case STANDARD -> constraintFactory.forEach(sourceClass);
            case LEGACY -> constraintFactory.source(sourceClass);
        };
    }

    protected <A> AsmUniConstraintStream<A> createUniWithNulls(Class<A> sourceClass) {
        return switch (retrievalSemantics) {
            case STANDARD -> constraintFactory.forEachIncludingNullVars(sourceClass);
            case LEGACY -> constraintFactory.fromUnfiltered(sourceClass);
        };
    }

    protected <A> AsmUniConstraintStream<A> createIfExistUniWithNulls(Class<A> sourceClass) {
        return switch (retrievalSemantics) {
            case STANDARD -> constraintFactory.forEachIncludingNullVars(sourceClass);
            case LEGACY -> constraintFactory.source(sourceClass);
        };
    }

    protected AbstractConstraintStreamNode getLeafNode() {
        return leafNode;
    }

    @Override
    public AsmConstraintFactory<?> getConstraintFactory() {
        return constraintFactory;
    }

    public RetrievalSemantics getRetrievalSemantics() {
        return retrievalSemantics;
    }

    public boolean guaranteesDistinct() {
        return guaranteeDistinct;
    }

    private AsConstraintNode buildAsConstraintNode(String constraintPackage, String constraintName) {
        int cardinality = leafNode.getResultTupleCardinality();
        return switch (cardinality) {
            case 1 -> new AsConstraintNode(constraintPackage, constraintName,
                    InnerUniConstraintStream.createDefaultJustificationMapping(),
                    InnerUniConstraintStream.createDefaultIndictedObjectsMapping());
            case 2 -> new AsConstraintNode(constraintPackage, constraintName,
                    InnerBiConstraintStream.createDefaultJustificationMapping(),
                    InnerBiConstraintStream.createDefaultIndictedObjectsMapping());
            case 3 -> new AsConstraintNode(constraintPackage, constraintName,
                    InnerTriConstraintStream.createDefaultJustificationMapping(),
                    InnerTriConstraintStream.createDefaultIndictedObjectsMapping());
            case 4 -> new AsConstraintNode(constraintPackage, constraintName,
                    InnerQuadConstraintStream.createDefaultJustificationMapping(),
                    InnerQuadConstraintStream.createDefaultIndictedObjectsMapping());
            default -> throw new IllegalStateException("Unexpected value: " + cardinality);
        };
    }

    public Constraint penalize(String constraintName, Score<?> constraintWeight) {

        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(new ImpactByConstantNode(ScoreImpactType.PENALTY, constraintWeight));
        AsConstraintNode asConstraintNode =
                buildAsConstraintNode(constraintFactory.getDefaultConstraintPackage(), constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint penalize(String constraintPackage, String constraintName, Score<?> constraintWeight) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(new ImpactByConstantNode(ScoreImpactType.PENALTY, constraintWeight));
        AsConstraintNode asConstraintNode = buildAsConstraintNode(constraintPackage, constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint penalizeConfigurable(String constraintName) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(
                        new ImpactByConfigurableConstantNode(ScoreImpactType.PENALTY));
        AsConstraintNode asConstraintNode =
                buildAsConstraintNode(constraintFactory.getDefaultConstraintPackage(), constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint penalizeConfigurable(String constraintPackage, String constraintName) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(
                        new ImpactByConfigurableConstantNode(ScoreImpactType.PENALTY));
        AsConstraintNode asConstraintNode = buildAsConstraintNode(constraintPackage, constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint reward(String constraintName, Score<?> constraintWeight) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(new ImpactByConstantNode(ScoreImpactType.REWARD, constraintWeight));
        AsConstraintNode asConstraintNode =
                buildAsConstraintNode(constraintFactory.getDefaultConstraintPackage(), constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint reward(String constraintPackage, String constraintName, Score<?> constraintWeight) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(new ImpactByConstantNode(ScoreImpactType.REWARD, constraintWeight));
        AsConstraintNode asConstraintNode = buildAsConstraintNode(constraintPackage, constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint rewardConfigurable(String constraintName) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(
                        new ImpactByConfigurableConstantNode(ScoreImpactType.REWARD));
        AsConstraintNode asConstraintNode =
                buildAsConstraintNode(constraintFactory.getDefaultConstraintPackage(), constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint rewardConfigurable(String constraintPackage, String constraintName) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(
                        new ImpactByConfigurableConstantNode(ScoreImpactType.REWARD));
        AsConstraintNode asConstraintNode = buildAsConstraintNode(constraintPackage, constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint impact(String constraintName, Score<?> constraintWeight) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(new ImpactByConstantNode(ScoreImpactType.MIXED, constraintWeight));
        AsConstraintNode asConstraintNode =
                buildAsConstraintNode(constraintFactory.getDefaultConstraintPackage(), constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }

    @Override
    public Constraint impact(String constraintPackage, String constraintName, Score<?> constraintWeight) {
        AbstractConstraintStreamNode penaltyNode =
                leafNode.withChild(new ImpactByConstantNode(ScoreImpactType.MIXED, constraintWeight));
        AsConstraintNode asConstraintNode = buildAsConstraintNode(constraintPackage, constraintName);
        return new AsmConstraint<>(constraintFactory, penaltyNode.withChild(asConstraintNode));
    }
}
