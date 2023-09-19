package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.bi.BiJoinerComber;
import ai.timefold.solver.constraint.streams.common.bi.DefaultBiJoiner;
import ai.timefold.solver.constraint.streams.common.penta.DefaultPentaJoiner;
import ai.timefold.solver.constraint.streams.common.penta.PentaJoinerComber;
import ai.timefold.solver.constraint.streams.common.quad.DefaultQuadJoiner;
import ai.timefold.solver.constraint.streams.common.quad.QuadJoinerComber;
import ai.timefold.solver.constraint.streams.common.tri.DefaultTriJoiner;
import ai.timefold.solver.constraint.streams.common.tri.TriJoinerComber;
import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.impl.score.stream.JoinerType;

public record JoinerDefinition<LeftMapping_>(LeftMapping_ leftMapping, Function<?, Object> rightMapping,
        JoinerType joinerType) {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Combined<?, ?> empty() {
        return new Combined<>(0, new JoinerDefinition[] {}, Optional.empty());
    }

    @SafeVarargs
    public static <A, B> Combined<Function<A, Object>, BiPredicate<A, B>> from(BiJoiner<A, B>... joinerList) {
        BiJoinerComber<A, B> merged = BiJoinerComber.comb(joinerList);
        DefaultBiJoiner<A, B> indexed = merged.getMergedJoiner();
        BiPredicate<A, B> filter = merged.getMergedFiltering();
        @SuppressWarnings("unchecked")
        JoinerDefinition<Function<A, Object>>[] indexedJoiners = new JoinerDefinition[indexed.getJoinerCount()];

        for (int i = 0; i < indexedJoiners.length; i++) {
            indexedJoiners[i] =
                    new JoinerDefinition<>(indexed.getLeftMapping(i), indexed.getRightMapping(i), indexed.getJoinerType(i));
        }
        return new Combined<>(1, indexedJoiners, Optional.ofNullable(filter));
    }

    @SafeVarargs
    public static <A, B, C> Combined<BiFunction<A, B, Object>, TriPredicate<A, B, C>> from(TriJoiner<A, B, C>... joinerList) {
        TriJoinerComber<A, B, C> merged = TriJoinerComber.comb(joinerList);
        DefaultTriJoiner<A, B, C> indexed = merged.getMergedJoiner();
        TriPredicate<A, B, C> filter = merged.getMergedFiltering();
        @SuppressWarnings("unchecked")
        JoinerDefinition<BiFunction<A, B, Object>>[] indexedJoiners = new JoinerDefinition[indexed.getJoinerCount()];

        for (int i = 0; i < indexedJoiners.length; i++) {
            indexedJoiners[i] =
                    new JoinerDefinition<>(indexed.getLeftMapping(i), indexed.getRightMapping(i), indexed.getJoinerType(i));
        }
        return new Combined<>(2, indexedJoiners, Optional.ofNullable(filter));
    }

    @SafeVarargs
    public static <A, B, C, D> Combined<TriFunction<A, B, C, Object>, QuadPredicate<A, B, C, D>> from(
            QuadJoiner<A, B, C, D>... joinerList) {
        QuadJoinerComber<A, B, C, D> merged = QuadJoinerComber.comb(joinerList);
        DefaultQuadJoiner<A, B, C, D> indexed = merged.getMergedJoiner();
        QuadPredicate<A, B, C, D> filter = merged.getMergedFiltering();
        @SuppressWarnings("unchecked")
        JoinerDefinition<TriFunction<A, B, C, Object>>[] indexedJoiners = new JoinerDefinition[indexed.getJoinerCount()];

        for (int i = 0; i < indexedJoiners.length; i++) {
            indexedJoiners[i] =
                    new JoinerDefinition<>(indexed.getLeftMapping(i), indexed.getRightMapping(i), indexed.getJoinerType(i));
        }
        return new Combined<>(3, indexedJoiners, Optional.ofNullable(filter));
    }

    @SafeVarargs
    public static <A, B, C, D, E> Combined<QuadFunction<A, B, C, D, Object>, PentaPredicate<A, B, C, D, E>> from(
            PentaJoiner<A, B, C, D, E>... joinerList) {
        PentaJoinerComber<A, B, C, D, E> merged = PentaJoinerComber.comb(joinerList);
        DefaultPentaJoiner<A, B, C, D, E> indexed = merged.getMergedJoiner();
        PentaPredicate<A, B, C, D, E> filter = merged.getMergedFiltering();
        @SuppressWarnings("unchecked")
        JoinerDefinition<QuadFunction<A, B, C, D, Object>>[] indexedJoiners = new JoinerDefinition[indexed.getJoinerCount()];

        for (int i = 0; i < indexedJoiners.length; i++) {
            indexedJoiners[i] =
                    new JoinerDefinition<>(indexed.getLeftMapping(i), indexed.getRightMapping(i), indexed.getJoinerType(i));
        }
        return new Combined<>(4, indexedJoiners, Optional.ofNullable(filter));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        JoinerDefinition<?> that = (JoinerDefinition<?>) object;
        return Objects.equals(leftMapping, that.leftMapping) && Objects.equals(rightMapping,
                that.rightMapping) && joinerType == that.joinerType;
    }

    public boolean bytecodeEquals(JoinerDefinition<LeftMapping_> joinerDefinition,
            Map<Object, List<Object>> lambdaToBytecodeMap) {
        return AbstractConstraintStreamNode.bytecodeEqual(leftMapping, joinerDefinition.leftMapping, lambdaToBytecodeMap) &&
                AbstractConstraintStreamNode.bytecodeEqual(rightMapping, joinerDefinition.rightMapping, lambdaToBytecodeMap) &&
                joinerType == joinerDefinition.joinerType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftMapping, rightMapping, joinerType);
    }

    public record Combined<LeftMapping_, Filter_>(int leftTupleSize, JoinerDefinition<LeftMapping_>[] indexedJoiners,
            Optional<Filter_> filter) {
        public int getIndexedJoinerCount() {
            return indexedJoiners.length;
        }

        public JoinerDefinition<LeftMapping_> getIndexedJoiner(int i) {
            return indexedJoiners[i];
        }

        @Override
        public boolean equals(Object object) {
            if (this == object)
                return true;
            if (object == null || getClass() != object.getClass())
                return false;
            Combined<?, ?> combined = (Combined<?, ?>) object;
            return Arrays.equals(indexedJoiners, combined.indexedJoiners) && Objects.equals(filter,
                    combined.filter);
        }

        @SuppressWarnings("unchecked")
        public boolean bytecodeEquals(Combined<?, ?> combined, Map<Object, List<Object>> lambdaToBytecodeMap) {
            if (indexedJoiners.length != combined.indexedJoiners.length) {
                return false;
            }
            for (int i = 0; i < indexedJoiners.length; i++) {
                if (!indexedJoiners[i].bytecodeEquals((JoinerDefinition<LeftMapping_>) combined.indexedJoiners[i],
                        lambdaToBytecodeMap)) {
                    return false;
                }
            }
            return AbstractConstraintStreamNode.bytecodeEqual(filter, combined.filter, lambdaToBytecodeMap);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(filter);
            result = 31 * result + Arrays.hashCode(indexedJoiners);
            return result;
        }

        public void visitBytecode(Consumer<Object> bytecodeVisitor) {
            for (JoinerDefinition<LeftMapping_> joinerDefinition : indexedJoiners) {
                bytecodeVisitor.accept(joinerDefinition.leftMapping);
                bytecodeVisitor.accept(joinerDefinition.rightMapping);
            }
            filter.ifPresent(bytecodeVisitor);
        }
    }
}
