package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.CascadingUpdateCommand;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.Pair;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * The listener might update only one shadow variables since the targetVariableDescriptorList contains a single field.
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SingleCascadingUpdateShadowVariableListener<Solution_>
        extends AbstractCascadingUpdateShadowVariableListener<Solution_> {

    private final VariableDescriptor<Solution_> targetVariableDescriptor;

    SingleCascadingUpdateShadowVariableListener(ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            List<VariableDescriptor<Solution_>> targetVariableDescriptorList, MemberAccessor targetMethod,
            CascadingUpdateCommand<Pair<Integer, Object>> indexElementCommand) {
        super(sourceListVariableDescriptor, targetVariableDescriptorList, targetMethod, indexElementCommand);
        this.targetVariableDescriptor = targetVariableDescriptorList.get(0);
    }

    @Override
    boolean execute(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var oldValue = targetVariableDescriptor.getValue(entity);
        scoreDirector.beforeVariableChanged(entity, targetVariableDescriptor.getVariableName());
        targetMethod.executeGetter(entity);
        var newValue = targetVariableDescriptor.getValue(entity);
        scoreDirector.afterVariableChanged(entity, targetVariableDescriptor.getVariableName());
        return !Objects.equals(oldValue, newValue);
    }
}
