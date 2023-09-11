package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class SingleThreadedRecommendationProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements RecommendationProcessor<Solution_, Out_, Score_> {

    private final InnerScoreDirector<Solution_, Score_> scoreDirector;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final In_ clonedElement;

    public SingleThreadedRecommendationProcessor(InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ clonedElement) {
        this.scoreDirector = scoreDirector;
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
        this.clonedElement = clonedElement;
    }

    @Override
    public CompletableFuture<Recommendation<Out_, Score_>> execute(Move<Solution_> move) {
        var undo = move.doMove(scoreDirector);
        var newScore = scoreDirector.calculateScore();
        var newScoreDifference = newScore.subtract(originalScore)
                .withInitScore(0);
        var result = valueResultFunction.apply(clonedElement);
        var recommendation = new DefaultRecommendation<>(result, newScoreDifference);
        undo.doMoveOnly(scoreDirector);
        return CompletableFuture.completedFuture(recommendation);
    }

    @Override
    public void close() {
        // No need to do anything.
    }
}
