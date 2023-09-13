package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.heuristic.move.Move;

public interface FitProcessor<Solution_, Out_, Score_ extends Score<Score_>> extends AutoCloseable {

    CompletableFuture<Void> execute(Move<Solution_> move);

    List<RecommendedFit<Out_, Score_>> getRecommendations();

}
