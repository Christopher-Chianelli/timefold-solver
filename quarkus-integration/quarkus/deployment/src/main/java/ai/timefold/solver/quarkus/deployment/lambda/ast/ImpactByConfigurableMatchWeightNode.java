package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.deployment.lambda.ScoreWeightType;

public final class ImpactByConfigurableMatchWeightNode<MatchScorer_> extends AbstractConstraintStreamNode
        implements ImpactNode {
    final ScoreImpactType scoreImpactType;
    final ScoreWeightType scoreWeightType;
    final MatchScorer_ matchScorer;

    public ImpactByConfigurableMatchWeightNode(ScoreImpactType scoreImpactType, ScoreWeightType scoreWeightType,
            MatchScorer_ matchScorer) {
        this.scoreImpactType = scoreImpactType;
        this.scoreWeightType = scoreWeightType;
        this.matchScorer = matchScorer;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        return expandArray(getOnlyParent().getResultTupleTypes(), scoreWeightType.getWeightClass());
    }

    @Override
    public int getResultTupleCardinality() {
        return getInputTupleCardinality() + 1;
    }

    @Override
    public boolean guaranteeDistinct() {
        return getOnlyParent().guaranteeDistinct();
    }

    public ScoreImpactType getScoreImpactType() {
        return scoreImpactType;
    }

    public ScoreWeightType getScoreWeightType() {
        return scoreWeightType;
    }

    public MatchScorer_ getMatchScorer() {
        return matchScorer;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof ImpactByConfigurableMatchWeightNode<?> impactByConfigurableMatchWeightNode) {
            return scoreImpactType == impactByConfigurableMatchWeightNode.scoreImpactType &&
                    scoreWeightType == impactByConfigurableMatchWeightNode.scoreWeightType &&
                    matchScorer == impactByConfigurableMatchWeightNode.matchScorer;
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof ImpactByConfigurableMatchWeightNode<?> impactByConfigurableMatchWeightNode) {
            return scoreImpactType == impactByConfigurableMatchWeightNode.scoreImpactType &&
                    scoreWeightType == impactByConfigurableMatchWeightNode.scoreWeightType &&
                    bytecodeEqual(matchScorer, impactByConfigurableMatchWeightNode.matchScorer, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        bytecodeVisitor.accept(matchScorer);
    }

    @Override
    public <Solution_> Score<?> getConstraintWeight(String constraintPackage, String constraintName,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution) {
        Score<?> baseWeight = solutionDescriptor.getConstraintConfigurationDescriptor()
                .findConstraintWeightDescriptor(constraintPackage, constraintName)
                .createExtractor()
                .apply(solution);

        return switch (scoreImpactType) {
            case REWARD, MIXED -> baseWeight;
            case PENALTY -> baseWeight.negate();
        };
    }
}
