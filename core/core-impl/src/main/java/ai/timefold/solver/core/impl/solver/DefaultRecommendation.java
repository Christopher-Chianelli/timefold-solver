package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;

record DefaultRecommendation<Result_, Score_ extends Score<Score_>>(Result_ result, Score_ score)
        implements
            Recommendation<Result_, Score_> {
}
