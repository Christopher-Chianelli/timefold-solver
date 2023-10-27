package ai.timefold.solver.core.api.score.stream;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;

final class DefaultUniConstraintCollector<A, ResultContainer_, Result_>
        implements UniConstraintCollector<A, ResultContainer_, Result_> {

    private final Supplier<ResultContainer_> supplier;
    private final BiFunction<ResultContainer_, A, Runnable> accumulator;
    private final Function<ResultContainer_, Result_> finisher;

    private final Object[] equalityArgs;

    public DefaultUniConstraintCollector(Supplier<ResultContainer_> supplier,
            BiFunction<ResultContainer_, A, Runnable> accumulator,
            Function<ResultContainer_, Result_> finisher,
            Object... equalityArgs) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.finisher = finisher;
        this.equalityArgs = equalityArgs;
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return supplier;
    }

    @Override
    public BiFunction<ResultContainer_, A, Runnable> accumulator() {
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
        DefaultUniConstraintCollector<?, ?, ?> that = (DefaultUniConstraintCollector<?, ?, ?>) object;
        return Arrays.equals(equalityArgs, that.equalityArgs);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(equalityArgs);
    }
}
