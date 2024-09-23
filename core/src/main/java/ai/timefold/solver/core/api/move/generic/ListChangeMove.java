package ai.timefold.solver.core.api.move.generic;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.move.factory.Move;
import ai.timefold.solver.core.api.move.factory.MoveDirector;
import ai.timefold.solver.core.api.move.factory.Rebaser;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel.LocationInList;

public record ListChangeMove<Solution_, Entity_>(ListVariableMetaModel<Solution_, Entity_> variableMetaModel, Object value,
        Object insertAfter)
        implements
            Move<Solution_, ListChangeMove.ListChangeMoveContext<Entity_>> {

    @Override
    public ListChangeMoveContext<Entity_> prepareContext() {
        var sourcePosition = variableMetaModel.positionOf(value);
        var destinationPosition = variableMetaModel.positionOf(insertAfter);
        return new ListChangeMoveContext<>(sourcePosition, destinationPosition);
    }

    @Override
    public void run(MoveDirector<Solution_> moveDirector, ListChangeMoveContext<Entity_> ctx) {
        var sourceEntity = ctx.source().entity();
        var sourceIndex = ctx.source().index();
        var destinationEntity = ctx.destination().entity();
        var destinationIndex = ctx.destination().index();
        if (sourceEntity == destinationEntity) {
            moveDirector.moveValue(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
        } else {
            moveDirector.moveValue(variableMetaModel, sourceEntity, sourceIndex, destinationEntity, destinationIndex + 1);
        }
    }

    @Override
    public Move<Solution_, ListChangeMoveContext<Entity_>> rebase(Rebaser rebaser, ListChangeMoveContext<Entity_> ctx) {
        return new ListChangeMove<>(variableMetaModel, rebaser.apply(value), rebaser.apply(insertAfter));
    }

    @Override
    public Collection<?> getPlanningEntities(ListChangeMoveContext<Entity_> ctx) {
        return List.of(ctx.source.entity(), ctx.destination.entity());
    }

    @Override
    public Collection<?> getPlanningValues(ListChangeMoveContext<Entity_> ctx) {
        return List.of(value);
    }

    @Override
    public String toString(ListChangeMoveContext<Entity_> ctx) {
        return ""; // TODO
    }

    public record ListChangeMoveContext<Entity_>(LocationInList<Entity_> source, LocationInList<Entity_> destination) {
    }

}
