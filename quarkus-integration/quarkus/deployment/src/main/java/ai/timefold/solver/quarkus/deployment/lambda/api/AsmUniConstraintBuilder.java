package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.uni.InnerUniConstraintStream;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraint;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraintFactory;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;

public class AsmUniConstraintBuilder<A, Score_ extends Score<Score_>> implements UniConstraintBuilder<A, Score_> {
    final AsmConstraintFactory<?> constraintFactory;
    final AbstractConstraintStreamNode leafNode;
    BiFunction<A, Score_, ?> justificationMapping;
    Function<A, Collection<Object>> indictedObjectsMapping;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AsmUniConstraintBuilder(AsmConstraintFactory<?> constraintFactory, AbstractConstraintStreamNode leafNode) {
        this.constraintFactory = constraintFactory;
        this.leafNode = leafNode;
        justificationMapping = (BiFunction<A, Score_, ?>) InnerUniConstraintStream.createDefaultJustificationMapping();
        indictedObjectsMapping = (Function) InnerUniConstraintStream.createDefaultIndictedObjectsMapping();
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
    public <ConstraintJustification_ extends ConstraintJustification> UniConstraintBuilder<A, Score_> justifyWith(
            BiFunction<A, Score_, ConstraintJustification_> justificationMapping) {
        this.justificationMapping = justificationMapping;
        return this;
    }

    @Override
    public UniConstraintBuilder<A, Score_> indictWith(Function<A, Collection<Object>> indictedObjectsMapping) {
        this.indictedObjectsMapping = indictedObjectsMapping;
        return this;
    }
}
