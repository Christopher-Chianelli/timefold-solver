package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.domain.solution.cloner.ConcurrentMemoization;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

final class MultiThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements FitProcessor<Solution_, Out_, Score_> {

    private final InnerScoreDirector<Solution_, Score_> parentScoreDirector;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final In_ originalElement;
    private final ExecutorService executorService;
    private final ConcurrentMemoization<Thread, ThreadState<Solution_, In_, Out_, Score_>> threadState =
            new ConcurrentMemoization<>();
    private long unsignedCounter = 0;

    public MultiThreadedFitProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
        this.parentScoreDirector = scoreDirector;
        this.executorService = new ForkJoinPool(moveThreadCount);
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
        this.originalElement = originalElement;
    }

    @Override
    public CompletableFuture<Void> execute(Move<Solution_> move) {
        long id = unsignedCounter++;
        return CompletableFuture.runAsync(() -> {
            try {
                var state = threadState.computeIfAbsent(Thread.currentThread(), t -> {
                    var sd = parentScoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD);
                    var e = sd.lookUpWorkingObject(originalElement);
                    var buffer = new ArrayList<RecommendedFit<Out_, Score_>>();
                    return new ThreadState<>(sd, e, buffer);
                });
                var scoreDirector = state.scoreDirector;
                var undo = move.rebase(scoreDirector)
                        .doMove(scoreDirector);
                var newScore = scoreDirector.calculateScore();
                var result = valueResultFunction.apply(state.clonedElement);
                undo.doMoveOnly(scoreDirector);
                var newScoreDifference = newScore.subtract(originalScore)
                        .withInitScore(0);
                var recommendedFit = new DefaultRecommendedFit<>(id, result, newScoreDifference);
                state.bufferList.add(recommendedFit);
            } catch (Exception ex) {
                throw new IllegalStateException("Recommendation execution threw an exception.", ex);
            }
        }, executorService);
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> getRecommendations() {
        List<RecommendedFit<Out_, Score_>> result = new ArrayList<>();
        for (var threadState : threadState.values()) {
            threadState.bufferList.sort(null);
            result.addAll(threadState.bufferList);
        }
        result.sort(null);
        return result;
    }

    @Override
    public void close() {
        for (var state : threadState.values()) {
            var scoreDirector = state.scoreDirector;
            scoreDirector.calculateScore(); // Process the final undo move to return solution back to original state.
            scoreDirector.close();
        }
        executorService.shutdownNow();
    }

    private record ThreadState<Solution_, In_, Out_, Score_ extends Score<Score_>>(
            InnerScoreDirector<Solution_, Score_> scoreDirector, In_ clonedElement,
            List<RecommendedFit<Out_, Score_>> bufferList) {
    }

}
