package ai.timefold.solver.core.api.score.stream;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;

final class DefaultQuadConstraintCollector<A, B, C, D, ResultContainer_, Result_>
        implements QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> {

    private final Supplier<ResultContainer_> supplier;
    private final PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator;
    private final Function<ResultContainer_, Result_> finisher;
    private final Object[] equalityArgs;

    public DefaultQuadConstraintCollector(Supplier<ResultContainer_> supplier,
            PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator,
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
    public PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator() {
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
        DefaultQuadConstraintCollector<?, ?, ?, ?, ?, ?> that = (DefaultQuadConstraintCollector<?, ?, ?, ?, ?, ?>) object;
        return Arrays.equals(equalityArgs, that.equalityArgs);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(equalityArgs);
    }
}
