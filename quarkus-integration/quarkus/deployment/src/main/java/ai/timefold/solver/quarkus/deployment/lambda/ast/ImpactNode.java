package ai.timefold.solver.quarkus.deployment.lambda.ast;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public interface ImpactNode {
    <Solution_> Score<?> getConstraintWeight(String constraintPackage, String constraintName,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution);

    ScoreImpactType getScoreImpactType();
}
