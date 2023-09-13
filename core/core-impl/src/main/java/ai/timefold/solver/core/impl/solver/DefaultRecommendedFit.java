package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;

record DefaultRecommendedFit<Result_, Score_ extends Score<Score_>>(long index, Result_ result, Score_ scoreDifference)
        implements
            RecommendedFit<Result_, Score_>,
            Comparable<DefaultRecommendedFit<Result_, Score_>> {
    @Override
    public int compareTo(DefaultRecommendedFit<Result_, Score_> other) {
        int scoreComparison = scoreDifference.compareTo(other.scoreDifference);
        if (scoreComparison != 0) {
            return -scoreComparison; // Better scores first.
        } else if (result instanceof Comparable comparableResult) {
            int comparison = comparableResult.compareTo(other.result);
            if (comparison != 0) { // The user specified the order in which they want to see the results.
                return comparison;
            }
        }
        // Otherwise maintain insertion order.
        return Long.compareUnsigned(index, other.index); // Unsigned == many more positive values.
    }
}
