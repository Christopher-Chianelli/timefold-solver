package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;

record DefaultRecommendation<Result_, Score_ extends Score<Score_>>(Result_ result, Score_ scoreDifference)
        implements
            Recommendation<Result_, Score_>,
            Comparable<DefaultRecommendation<Result_, Score_>> {
    @Override
    public int compareTo(DefaultRecommendation<Result_, Score_> other) {
        int scoreComparison = scoreDifference.compareTo(other.scoreDifference);
        if (scoreComparison != 0) {
            return -scoreComparison; // Better scores first.
        } else if (result instanceof Comparable comparableResult) {
            return comparableResult.compareTo(other.result);
        } else {
            return 0; // Do not reorder recommendations which are otherwise equal.
        }
    }
}
