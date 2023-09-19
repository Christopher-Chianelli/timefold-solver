package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.util.Collection;

import ai.timefold.solver.constraint.streams.common.quad.InnerQuadConstraintStream;
import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraintFactory;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;

public class AsmQuadConstraintBuilder<A, B, C, D, Score_ extends Score<Score_>> implements
        QuadConstraintBuilder<A, B, C, D, Score_> {
    final AsmConstraintFactory<?> constraintFactory;
    final AbstractConstraintStreamNode leafNode;
    PentaFunction<A, B, C, D, Score_, ?> justificationMapping;
    QuadFunction<A, B, C, D, Collection<Object>> indictedObjectsMapping;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AsmQuadConstraintBuilder(AsmConstraintFactory<?> constraintFactory, AbstractConstraintStreamNode leafNode) {
        this.constraintFactory = constraintFactory;
        this.leafNode = leafNode;
        this.justificationMapping =
                (PentaFunction<A, B, C, D, Score_, ?>) InnerQuadConstraintStream.createDefaultJustificationMapping();
        this.indictedObjectsMapping = (QuadFunction) InnerQuadConstraintStream.createDefaultIndictedObjectsMapping();
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
    public <ConstraintJustification_ extends ConstraintJustification> QuadConstraintBuilder<A, B, C, D, Score_> justifyWith(
            PentaFunction<A, B, C, D, Score_, ConstraintJustification_> justificationMapping) {
        this.justificationMapping = justificationMapping;
        return this;
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, Score_> indictWith(
            QuadFunction<A, B, C, D, Collection<Object>> indictedObjectsMapping) {
        this.indictedObjectsMapping = indictedObjectsMapping;
        return this;
    }
}
