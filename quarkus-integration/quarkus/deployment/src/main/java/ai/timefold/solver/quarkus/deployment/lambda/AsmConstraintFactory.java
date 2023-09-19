package ai.timefold.solver.quarkus.deployment.lambda;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import ai.timefold.solver.constraint.streams.common.InnerConstraintFactory;
import ai.timefold.solver.constraint.streams.common.RetrievalSemantics;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.deployment.lambda.api.AsmUniConstraintStream;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.FilterNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.SourceNode;

public final class AsmConstraintFactory<Solution_> extends InnerConstraintFactory<Solution_, AsmConstraint<Solution_>> {
    final SolutionDescriptor<Solution_> solutionDescriptor;
    final EnvironmentMode environmentModel;
    final AtomicLong idGenerator;

    public AsmConstraintFactory(SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentModel) {
        this.solutionDescriptor = solutionDescriptor;
        this.environmentModel = environmentModel;
        idGenerator = new AtomicLong();
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @Override
    public String getDefaultConstraintPackage() {
        return solutionDescriptor.getSolutionClass().getPackageName();
    }

    private <A> Predicate<A> getNullityFilter(Class<A> fromClass) {
        EntityDescriptor<Solution_> entityDescriptor = getSolutionDescriptor().findEntityDescriptor(fromClass);
        if (entityDescriptor != null && entityDescriptor.hasAnyGenuineVariables()) {
            return (Predicate<A>) entityDescriptor.getHasNoNullVariables();
        }
        return null;
    }

    @Override
    public <A> AsmUniConstraintStream<A> forEach(Class<A> sourceClass) {
        assertValidFromType(sourceClass);
        AbstractConstraintStreamNode ast = new SourceNode(sourceClass);
        ast.setIdGenerator(idGenerator);
        Predicate<A> nullityFilter = getNullityFilter(sourceClass);
        if (nullityFilter != null) {
            ast = ast.withChild(new FilterNode<>(nullityFilter));
        }
        return new AsmUniConstraintStream<>(this, ast, RetrievalSemantics.STANDARD);
    }

    public <A> AsmUniConstraintStream<A> source(Class<A> sourceClass) {
        assertValidFromType(sourceClass);
        AbstractConstraintStreamNode ast = new SourceNode(sourceClass);
        ast.setIdGenerator(idGenerator);
        return new AsmUniConstraintStream<>(this, ast, RetrievalSemantics.LEGACY);
    }

    @Override
    public <A> AsmUniConstraintStream<A> forEachIncludingNullVars(Class<A> sourceClass) {
        assertValidFromType(sourceClass);
        SourceNode ast = new SourceNode(sourceClass);
        ast.setIdGenerator(idGenerator);
        return new AsmUniConstraintStream<>(this, ast, RetrievalSemantics.STANDARD);
    }

    @Override
    public <A> AsmUniConstraintStream<A> from(Class<A> fromClass) {
        assertValidFromType(fromClass);
        EntityDescriptor<Solution_> entityDescriptor = getSolutionDescriptor().findEntityDescriptor(fromClass);
        AbstractConstraintStreamNode ast = new SourceNode(fromClass);
        ast.setIdGenerator(idGenerator);
        if (entityDescriptor != null && entityDescriptor.hasAnyGenuineVariables()) {
            ast = ast.withChild(new FilterNode<>(entityDescriptor.getIsInitializedPredicate()));
        }
        return new AsmUniConstraintStream<>(this, ast, RetrievalSemantics.LEGACY);
    }

    @Override
    public <A> AsmUniConstraintStream<A> fromUnfiltered(Class<A> fromClass) {
        assertValidFromType(fromClass);
        SourceNode ast = new SourceNode(fromClass);
        ast.setIdGenerator(idGenerator);
        return new AsmUniConstraintStream<>(this, ast, RetrievalSemantics.LEGACY);
    }
}
