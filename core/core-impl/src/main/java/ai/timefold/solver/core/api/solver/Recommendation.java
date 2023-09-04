package ai.timefold.solver.core.api.solver;

import ai.timefold.solver.core.api.score.Score;

public interface Recommendation<Result_, Score_ extends Score<Score_>> {

    Result_ result();

    Score_ scoreDifference();

}
