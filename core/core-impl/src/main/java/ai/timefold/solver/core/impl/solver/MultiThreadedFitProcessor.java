package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> recommendationBuffer = new IdentityHashMap<>();
    private final ThreadLocal<List<RecommendedFit<Out_, Score_>>> threadLocalBuffer;

    public MultiThreadedFitProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
        this.threadLocalScoreDirector = ThreadLocal.withInitial(() -> scoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD));
        this.threadLocalClonedElement = ThreadLocal.withInitial(() -> threadLocalScoreDirector.get().lookUpWorkingObject(originalElement));
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> {
            var buffer = new ArrayList<RecommendedFit<Out_, Score_>>();
            recommendationBuffer.put(threadLocalScoreDirector.get(), buffer);
            return buffer;
        });
        this.executorService = Executors.newFixedThreadPool(moveThreadCount);
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
    }

    @Override
    public CompletableFuture<Void> execute(Move<Solution_> move) {
        return CompletableFuture.runAsync(() -> {
            try {
                var scoreDirector = threadLocalScoreDirector.get();
                var undo = move.rebase(scoreDirector)
                        .doMove(scoreDirector);
                var newScore = scoreDirector.calculateScore();
                var result = valueResultFunction.apply(threadLocalClonedElement.get());
                undo.doMoveOnly(scoreDirector);
                var newScoreDifference = newScore.subtract(originalScore)
                        .withInitScore(0);
                var recommendedFit = new DefaultRecommendedFit<>(result, newScoreDifference);
                threadLocalBuffer.get().add(recommendedFit);
            } catch (Exception ex) {
                throw new IllegalStateException("Recommendation execution threw an exception.", ex);
            }
        }, executorService);
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> getRecommendations() {
        return recommendationBuffer.values()
                .stream()
                .flatMap(Collection::stream)
                .sorted()
                .toList();
    }

    @Override
    public void close() {
        for (var scoreDirector : recommendationBuffer.keySet()) {
            scoreDirector.calculateScore(); // Return solution to original state.
            scoreDirector.close();
        }
        executorService.shutdownNow();
    }
}
