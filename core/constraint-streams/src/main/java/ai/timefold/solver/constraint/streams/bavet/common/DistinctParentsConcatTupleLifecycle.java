package ai.timefold.solver.constraint.streams.bavet.common;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

public class DistinctParentsConcatTupleLifecycle<Tuple_ extends AbstractTuple> extends AbstractNode implements
        LeftTupleLifecycle<Tuple_>, RightTupleLifecycle<Tuple_> {
    private final TupleLifecycle<Tuple_> tupleLifecycle;
    private final StaticPropagationQueue<Tuple_> propagationQueue;

    public DistinctParentsConcatTupleLifecycle(TupleLifecycle<Tuple_> tupleLifecycle) {
        this.tupleLifecycle = tupleLifecycle;
        this.propagationQueue = new StaticPropagationQueue<>(tupleLifecycle);
    }

    @Override
    public void insertLeft(Tuple_ tuple) {
        tupleLifecycle.insert(tuple);
    }

    @Override
    public void insertRight(Tuple_ tuple) {
        tupleLifecycle.insert(tuple);
    }

    @Override
    public void updateLeft(Tuple_ tuple) {
        tupleLifecycle.update(tuple);
    }

    @Override
    public void updateRight(Tuple_ tuple) {
        tupleLifecycle.update(tuple);
    }

    @Override
    public void retractLeft(Tuple_ tuple) {
        tupleLifecycle.retract(tuple);
    }

    @Override
    public void retractRight(Tuple_ tuple) {
        tupleLifecycle.retract(tuple);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }
}
