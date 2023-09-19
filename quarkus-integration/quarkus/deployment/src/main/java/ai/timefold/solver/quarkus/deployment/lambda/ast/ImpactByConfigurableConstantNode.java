package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public final class ImpactByConfigurableConstantNode extends AbstractConstraintStreamNode implements ImpactNode {
    final ScoreImpactType scoreImpactType;

    public ImpactByConfigurableConstantNode(ScoreImpactType scoreImpactType) {
        this.scoreImpactType = scoreImpactType;
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
        if (other instanceof ImpactByConfigurableConstantNode impactByConfigurableConstantNode) {
            return scoreImpactType == impactByConfigurableConstantNode.scoreImpactType;
        }
        return false;
    }

    @Override
    protected boolean hasSameProperties(AbstractConstraintStreamNode other, Map<Object, List<Object>> lambdaToBytecodeMap) {
        if (other instanceof ImpactByConfigurableConstantNode impactByConfigurableConstantNode) {
            return scoreImpactType == impactByConfigurableConstantNode.scoreImpactType;
        }
        return false;
    }

    @Override
    public void visitBytecode(Consumer<Object> bytecodeVisitor) {

    }

    public ScoreImpactType getScoreImpactType() {
        return scoreImpactType;
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
