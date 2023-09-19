package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public final class ImpactByConstantNode extends AbstractConstraintStreamNode implements ImpactNode {
    final ScoreImpactType scoreImpactType;
    final Score<?> impact;

    public ImpactByConstantNode(ScoreImpactType scoreImpactType, Score<?> impact) {
        this.scoreImpactType = scoreImpactType;
        this.impact = impact;
    }

    @Override
    public Class<?>[] getResultTupleTypes() {
        return expandArray(getOnlyParent().getResultTupleTypes(), int.class);
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
        if (other instanceof ImpactByConstantNode impactByConstantNode) {
            return scoreImpactType == impactByConstantNode.scoreImpactType &&
                    impact.equals(impactByConstantNode.impact);
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof ImpactByConstantNode impactByConstantNode) {
            return scoreImpactType == impactByConstantNode.scoreImpactType &&
                    impact.equals(impactByConstantNode.impact);
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {
    }

    public ScoreImpactType getScoreImpactType() {
        return scoreImpactType;
    }

    public Score<?> getImpact() {
        return impact;
    }

    @Override
    public <Solution_> Score<?> getConstraintWeight(String constraintPackage, String constraintName,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution) {
        return switch (scoreImpactType) {
            case REWARD, MIXED -> impact;
            case PENALTY -> impact.negate();
        };
    }
}
