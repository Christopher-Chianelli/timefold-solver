package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.CompletableFuture;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;
import ai.timefold.solver.core.impl.heuristic.move.Move;

public interface RecommendationProcessor<Solution_, Out_, Score_ extends Score<Score_>> extends AutoCloseable {

    CompletableFuture<Recommendation<Out_, Score_>> execute(Move<Solution_> move);

}
