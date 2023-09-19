package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.util.Collection;
import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.common.bi.InnerBiConstraintStream;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintBuilder;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraintFactory;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;

public class AsmBiConstraintBuilder<A, B, Score_ extends Score<Score_>> implements BiConstraintBuilder<A, B, Score_> {
    final AsmConstraintFactory<?> constraintFactory;
    final AbstractConstraintStreamNode leafNode;
    TriFunction<A, B, Score_, ?> justificationMapping;
    BiFunction<A, B, Collection<Object>> indictedObjectsMapping;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AsmBiConstraintBuilder(AsmConstraintFactory<?> constraintFactory, AbstractConstraintStreamNode leafNode) {
        this.constraintFactory = constraintFactory;
        this.leafNode = leafNode;
        this.justificationMapping = (TriFunction<A, B, Score_, ?>) InnerBiConstraintStream.createDefaultJustificationMapping();
        this.indictedObjectsMapping = (BiFunction) InnerBiConstraintStream.createDefaultIndictedObjectsMapping();
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
    public <ConstraintJustification_ extends ConstraintJustification> BiConstraintBuilder<A, B, Score_>
            justifyWith(TriFunction<A, B, Score_, ConstraintJustification_> justificationMapping) {
        this.justificationMapping = justificationMapping;
        return this;
    }

    @Override
    public BiConstraintBuilder<A, B, Score_> indictWith(BiFunction<A, B, Collection<Object>> indictedObjectsMapping) {
        this.indictedObjectsMapping = indictedObjectsMapping;
        return this;
    }
}
