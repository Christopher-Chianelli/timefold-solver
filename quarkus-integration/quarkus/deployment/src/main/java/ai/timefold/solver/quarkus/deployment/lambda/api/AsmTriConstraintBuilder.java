package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.util.Collection;

import ai.timefold.solver.constraint.streams.common.tri.InnerTriConstraintStream;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintBuilder;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraintFactory;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;

public class AsmTriConstraintBuilder<A, B, C, Score_ extends Score<Score_>> implements TriConstraintBuilder<A, B, C, Score_> {
    final AsmConstraintFactory<?> constraintFactory;
    final AbstractConstraintStreamNode leafNode;
    QuadFunction<A, B, C, Score_, ?> justificationMapping;
    TriFunction<A, B, C, Collection<Object>> indictedObjectsMapping;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AsmTriConstraintBuilder(AsmConstraintFactory<?> constraintFactory, AbstractConstraintStreamNode leafNode) {
        this.constraintFactory = constraintFactory;
        this.leafNode = leafNode;
        this.justificationMapping =
                (QuadFunction<A, B, C, Score_, ?>) InnerTriConstraintStream.createDefaultJustificationMapping();
        this.indictedObjectsMapping = (TriFunction) InnerTriConstraintStream.createDefaultIndictedObjectsMapping();
    }

    @Override
    public Constraint asConstraint(String constraintName) {
        AsConstraintNode terminalNode = new AsConstraintNode(constraintFactory.getDefaultConstraintPackage(), constraintName,
                justificationMapping, indictedObjectsMapping);
        return new AsmConstraint<>(constraintFactory, leafNode.withChild(terminalNode));
    }

    @Override
    public Constraint asConstraint(String constraintPackage, String constraintName) {
        AsConstraintNode terminalNode =
                new AsConstraintNode(constraintPackage, constraintName, justificationMapping, indictedObjectsMapping);
        return new AsmConstraint<>(constraintFactory, leafNode.withChild(terminalNode));
    }

    @Override
    public <ConstraintJustification_ extends ConstraintJustification> TriConstraintBuilder<A, B, C, Score_> justifyWith(
            QuadFunction<A, B, C, Score_, ConstraintJustification_> justificationMapping) {
        this.justificationMapping = justificationMapping;
        return this;
    }

    @Override
    public TriConstraintBuilder<A, B, C, Score_> indictWith(TriFunction<A, B, C, Collection<Object>> indictedObjectsMapping) {
        this.indictedObjectsMapping = indictedObjectsMapping;
        return this;
    }
}
