package ai.timefold.solver.core.impl.score.stream.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.IntCounter;

final class CountIntQuadCollector<A, B, C, D> implements QuadConstraintCollector<A, B, C, D, IntCounter, Integer> {
    CountIntQuadCollector() {
    }

    @Override
    public Supplier<IntCounter> supplier() {
        return IntCounter::new;
    }

    @Override
    public PentaFunction<IntCounter, A, B, C, D, Runnable> accumulator() {
        return (counter, a, b, c, d) -> {
            counter.increment();
            return counter::decrement;
        };
    }

    @Override
    public Function<IntCounter, Integer> finisher() {
        return IntCounter::result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountIntQuadCollector;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
