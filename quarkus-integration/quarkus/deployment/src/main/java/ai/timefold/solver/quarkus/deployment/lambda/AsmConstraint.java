package ai.timefold.solver.quarkus.deployment.lambda;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AsConstraintNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ImpactByConfigurableMatchWeightNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ImpactByConstantNode;

public class AsmConstraint<Solution_>
        extends AbstractConstraint<Solution_, AsmConstraint<Solution_>, AsmConstraintFactory<Solution_>>
        implements Constraint {
    final ConstraintFactory constraintFactory;
    final AsConstraintNode terminalNode;

    public AsmConstraint(AsmConstraintFactory<Solution_> constraintFactory, AsConstraintNode terminalNode) {
        super(constraintFactory,
                terminalNode.getConstraintPackage(),
                terminalNode.getConstraintName(),
                workingSolution -> terminalNode.getConstraintWeight(constraintFactory.getSolutionDescriptor(), workingSolution),
                terminalNode.getSourceImpactNode().getScoreImpactType(),
                terminalNode.getSourceImpactNode() instanceof ImpactByConfigurableMatchWeightNode ||
                        terminalNode.getSourceImpactNode() instanceof ImpactByConstantNode,
                terminalNode.getJustificationMapping(),
                terminalNode.getIndictmentMapping());
        this.constraintFactory = constraintFactory;
        this.terminalNode = terminalNode;
        terminalNode.setAsmConstraint(this);
    }

    public Score<?> extractConstraintWeight(SolutionDescriptor<Solution_> solutionDescriptor, Solution_ workingSolution) {
        return terminalNode.getConstraintWeight(solutionDescriptor, workingSolution);
    }

    public AsConstraintNode getTerminalNode() {
        return terminalNode;
    }
}
