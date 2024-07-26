package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.CascadingUpdateCommand;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractCascadingUpdateShadowVariableListener<Solution_> implements VariableListener<Solution_, Object> {

    private final CascadingUpdateCommand<Object> nextElementCommand;
    final List<VariableDescriptor<Solution_>> targetVariableDescriptorList;
    final MemberAccessor targetMethod;

    AbstractCascadingUpdateShadowVariableListener(List<VariableDescriptor<Solution_>> targetVariableDescriptorList,
            MemberAccessor targetMethod, CascadingUpdateCommand<Object> nextElementCommand) {
        this.targetVariableDescriptorList = targetVariableDescriptorList;
        this.targetMethod = targetMethod;
        this.nextElementCommand = nextElementCommand;
    }

    abstract boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity);

    private Object getNextElement(Object value) {
        return nextElementCommand.getValue(value);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var currentEntity = entity;
        while (currentEntity != null) {
            if (!execute(scoreDirector, currentEntity)) {
                break;
            }
            currentEntity = getNextElement(currentEntity);
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

}
