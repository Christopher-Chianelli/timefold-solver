package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.impl.solver.thread.DefaultSolverThreadFactory;

final class MultiThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements FitProcessor<Solution_, Out_, Score_> {

    private final ThreadLocal<ThreadState<Solution_, In_, Out_, Score_>> threadLocalState;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final ExecutorService executorService;
    private final Map<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> recommendationBuffer =
            new ConcurrentHashMap<>();
    private long unsignedCounter = 0;

    public MultiThreadedFitProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
        this.threadLocalState = ThreadLocal.withInitial(() -> {
            var sd = scoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD);
            var e = sd.lookUpWorkingObject(originalElement);
            var buffer = new ArrayList<RecommendedFit<Out_, Score_>>();
            recommendationBuffer.put(sd, buffer);
            return new ThreadState<>(sd, e, buffer);
        });
        this.executorService = Executors.newFixedThreadPool(moveThreadCount,
                new DefaultSolverThreadFactory("FitRecommender"));
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
    }

    @Override
    public CompletableFuture<Void> execute(Move<Solution_> move) {
        long id = unsignedCounter++;
        return CompletableFuture.runAsync(() -> {
            try {
                var state = threadLocalState.get();
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
        for (var buffer : recommendationBuffer.values()) {
            buffer.sort(null);
            result.addAll(buffer);
        }
        result.sort(null);
        return result;
    }

    @Override
    public void close() {
        for (var scoreDirector : recommendationBuffer.keySet()) {
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
