package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class SingleThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements FitProcessor<Solution_, Out_, Score_> {

    private final InnerScoreDirector<Solution_, Score_> scoreDirector;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final In_ clonedElement;
    private final List<RecommendedFit<Out_, Score_>> recommendationList = new ArrayList<>();
    private long unsignedCounter = 0;

    public SingleThreadedFitProcessor(InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ clonedElement) {
        this.scoreDirector = scoreDirector;
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
        this.clonedElement = clonedElement;
    }

    @Override
    public void execute(Move<Solution_> move, Semaphore finishedMoveSemaphore) {
        try {
            var undo = move.doMove(scoreDirector);
            var newScore = scoreDirector.calculateScore();
            var newScoreDifference = newScore.subtract(originalScore)
                    .withInitScore(0);
            var result = valueResultFunction.apply(clonedElement);
            var recommendation = new DefaultRecommendedFit<>(unsignedCounter++, result, newScoreDifference);
            recommendationList.add(recommendation);
            undo.doMoveOnly(scoreDirector);
        } finally {
            finishedMoveSemaphore.release();
        }
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> getRecommendations() {
        recommendationList.sort(null);
        return recommendationList;
    }

    @Override
    public void close() {
        // No need to do anything.
    }
}
