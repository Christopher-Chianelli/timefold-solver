package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

final class MultiThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements FitProcessor<Solution_, Out_, Score_> {

    private final ThreadLocal<InnerScoreDirector<Solution_, Score_>> threadLocalScoreDirector;
    private final ThreadLocal<In_> threadLocalClonedElement;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final ExecutorService executorService;

    public MultiThreadedFitProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
        this.threadLocalScoreDirector = ThreadLocal.withInitial(() -> scoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD));
        this.threadLocalClonedElement = ThreadLocal.withInitial(() -> threadLocalScoreDirector.get().lookUpWorkingObject(originalElement));
        this.executorService = Executors.newWorkStealingPool(moveThreadCount);
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
    }

    @Override
    public CompletableFuture<RecommendedFit<Out_, Score_>> execute(Move<Solution_> move) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var scoreDirector = threadLocalScoreDirector.get();
                var undo = move.rebase(scoreDirector)
                        .doMove(scoreDirector);
                var newScore = scoreDirector.calculateScore();
                var result = valueResultFunction.apply(threadLocalClonedElement.get());
                undo.doMoveOnly(scoreDirector);
                var newScoreDifference = newScore.subtract(originalScore)
                        .withInitScore(0);
                return new DefaultRecommendedFit<>(result, newScoreDifference);
            } catch (Exception ex) {
                throw new IllegalStateException("Recommendation execution threw an exception.", ex);
            }
        }, executorService);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
