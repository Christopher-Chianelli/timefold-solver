package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractIfExistsNode;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConditionalAnyMatchTupleLifecycle<Tuple_ extends Tuple, Right_>
        implements TupleLifecycle<Tuple_> {

    private final TupleLifecycle<Tuple_> downstreamLifecycle;
    private final TupleWithRightPredicate<Tuple_, Right_> predicate;
    private final boolean shouldExist;
    private AbstractIfExistsNode<Tuple_, Right_> parent;
    private boolean isActive;

    public ConditionalAnyMatchTupleLifecycle(TupleLifecycle<Tuple_> downstreamLifecycle,
            TupleWithRightPredicate<Tuple_, Right_> predicate,
            boolean shouldExist) {
        this.downstreamLifecycle = Objects.requireNonNull(downstreamLifecycle);
        this.predicate = Objects.requireNonNull(predicate);
        this.shouldExist = shouldExist;
    }

    public void setParent(AbstractIfExistsNode<Tuple_, Right_> parent) {
        this.parent = parent;
    }

    @Override
    public void afterAllFactsInserted(boolean upstreamCanProduceTuples) {
        // It is possible the predicate will always filter everything out, but we cannot know that for certain.
        // We must pass the upstream information downstream, and be active if upstream can send anything to us.
        this.isActive = upstreamCanProduceTuples;
        downstreamLifecycle.afterAllFactsInserted(upstreamCanProduceTuples);
    }

    @Override
    public boolean isActive() {
        return isActive && downstreamLifecycle.isActive();
    }

    @Override
    public void insert(Tuple_ tuple) {
        // TODO: Check if any of the right tuples associated with this tuple
        //       match the predicate.
        //       Use tuple.getStore(parent.getInputStoreIndexLeftTrackerList())
        //       to get the associated AbstractIfExistsNode.FilteringTracker
        //       Update the tracker when iterating.
        downstreamLifecycle.insert(tuple);
    }

    @Override
    public void update(Tuple_ tuple) {
        // TODO: Check if any of the changed right tuples associated with this tuple
        //       match the predicate.
        //       Use tuple.getStore(parent.getInputStoreIndexLeftTrackerList())
        //       to get the associated AbstractIfExistsNode.FilteringTracker
        //       Update the tracker when iterating.
        downstreamLifecycle.update(tuple);
    }

    @Override
    public void retract(Tuple_ tuple) {
        downstreamLifecycle.retract(tuple);
    }

    public TupleWithRightPredicate<Tuple_, Right_> predicate() {
        return predicate;
    }

    @Override
    public String toString() {
        return "Conditional %s %s".formatted(shouldExist ? "any" : "none", downstreamLifecycle);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConditionalAnyMatchTupleLifecycle<?, ?> other
                && Objects.equals(this.downstreamLifecycle, other.downstreamLifecycle)
                && Objects.equals(this.predicate, other.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(downstreamLifecycle, predicate);
    }

    @FunctionalInterface
    public interface TupleWithRightPredicate<Tuple_ extends Tuple, Right_> extends BiPredicate<Tuple_, Right_> {
    }

}
