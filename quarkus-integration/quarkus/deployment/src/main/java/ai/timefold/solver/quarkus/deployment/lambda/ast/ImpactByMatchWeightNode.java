package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.deployment.lambda.ScoreWeightType;

public final class ImpactByMatchWeightNode<MatchScorer_> extends AbstractConstraintStreamNode implements ImpactNode {
    final ScoreImpactType scoreImpactType;
    final ScoreWeightType scoreWeightType;
    final Score<?> baseWeight;
    final MatchScorer_ matchScorer;

    public ImpactByMatchWeightNode(ScoreImpactType scoreImpactType, ScoreWeightType scoreWeightType, Score<?> baseWeight,
            MatchScorer_ matchScorer) {
        this.scoreImpactType = scoreImpactType;
        this.scoreWeightType = scoreWeightType;
        this.baseWeight = baseWeight;
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

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other) {
        if (other instanceof ImpactByMatchWeightNode<?> impactByMatchWeightNode) {
            return scoreImpactType == impactByMatchWeightNode.scoreImpactType &&
                    scoreWeightType == impactByMatchWeightNode.scoreWeightType &&
                    baseWeight.equals(impactByMatchWeightNode.baseWeight) &&
                    matchScorer == impactByMatchWeightNode.matchScorer;
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof ImpactByMatchWeightNode<?> impactByMatchWeightNode) {
            return scoreImpactType == impactByMatchWeightNode.scoreImpactType &&
                    scoreWeightType == impactByMatchWeightNode.scoreWeightType &&
                    baseWeight.equals(impactByMatchWeightNode.baseWeight) &&
                    bytecodeEqual(matchScorer, impactByMatchWeightNode.matchScorer, lambdaToBytecodeMap);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
        bytecodeVisitor.accept(matchScorer);
    }

    public ScoreImpactType getScoreImpactType() {
        return scoreImpactType;
    }

    public ScoreWeightType getScoreWeightType() {
        return scoreWeightType;
    }

    public Score<?> getBaseWeight() {
        return baseWeight;
    }

    public MatchScorer_ getMatchScorer() {
        return matchScorer;
    }

    @Override
    public <Solution_> Score<?> getConstraintWeight(String constraintPackage, String constraintName,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution) {
        return switch (scoreImpactType) {
            case REWARD, MIXED -> baseWeight;
            case PENALTY -> baseWeight.negate();
        };
    }
}
