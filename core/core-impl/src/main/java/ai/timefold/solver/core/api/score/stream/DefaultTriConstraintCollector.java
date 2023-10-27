package ai.timefold.solver.core.api.score.stream;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;

final class DefaultTriConstraintCollector<A, B, C, ResultContainer_, Result_>
        implements TriConstraintCollector<A, B, C, ResultContainer_, Result_> {

    private final Supplier<ResultContainer_> supplier;
    private final QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator;
    private final Function<ResultContainer_, Result_> finisher;
    private final ConstraintCollectors.ConstraintCollectorKind collectorKind;
    private final Object[] equalityArgs;

    public DefaultTriConstraintCollector(Supplier<ResultContainer_> supplier,
            QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator,
            Function<ResultContainer_, Result_> finisher,
            ConstraintCollectors.ConstraintCollectorKind collectorKind,
            Object... equalityArgs) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.finisher = finisher;
        this.collectorKind = collectorKind;
        this.equalityArgs = equalityArgs;
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return supplier;
    }

    @Override
    public QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator() {
        return accumulator;
    }

    @Override
    public Function<ResultContainer_, Result_> finisher() {
        return finisher;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        DefaultTriConstraintCollector<?, ?, ?, ?, ?> that = (DefaultTriConstraintCollector<?, ?, ?, ?, ?>) object;
        return collectorKind == that.collectorKind && Arrays.equals(equalityArgs, that.equalityArgs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(collectorKind);
        result = 31 * result + Arrays.hashCode(equalityArgs);
        return result;
    }
}
