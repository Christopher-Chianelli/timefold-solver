package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.impl.solver.thread.DefaultSolverThreadFactory;

final class MultiThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements FitProcessor<Solution_, Out_, Score_> {

    private final MultiThreadedRecommendationEvaluator<Solution_, In_, Out_, Score_>[] moveEvaluator;
    private final ExecutorService executorService;
    private final Map<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> recommendationBuffer =
            new ConcurrentHashMap<>();
    private long unsignedCounter = 0;
    private final AtomicBoolean isNotDone = new AtomicBoolean(true);

    @SuppressWarnings("unchecked")
    public MultiThreadedFitProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
        this.moveEvaluator = new MultiThreadedRecommendationEvaluator[moveThreadCount];

        for (int i = 0; i < moveThreadCount; i++) {
            moveEvaluator[i] = new MultiThreadedRecommendationEvaluator<>(
                    () -> {
                        var sd = scoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD);
                        var e = sd.lookUpWorkingObject(originalElement);
                        var buffer = new ArrayList<RecommendedFit<Out_, Score_>>();
                        recommendationBuffer.put(sd, buffer);
                        return new MultiThreadedRecommendationEvaluator.ThreadState<>(sd, e, buffer);
                    },
                    valueResultFunction,
                    originalScore,
                    isNotDone);
        }
        this.executorService = Executors.newFixedThreadPool(moveThreadCount,
                new DefaultSolverThreadFactory("FitRecommender"));
        for (MultiThreadedRecommendationEvaluator evaluator : moveEvaluator) {
            executorService.submit(evaluator);
        }
    }

    @Override
    public void execute(Move<Solution_> move, Semaphore finishedMoveSemaphore) {
        long id = unsignedCounter++;
        int threadId = ((int) unsignedCounter) % moveEvaluator.length;
        moveEvaluator[threadId].queueMove(id, move, finishedMoveSemaphore);
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> getRecommendations() {
        isNotDone.set(false);
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
        isNotDone.set(false);
        for (var scoreDirector : recommendationBuffer.keySet()) {
            scoreDirector.calculateScore(); // Process the final undo move to return solution back to original state.
            scoreDirector.close();
        }
        executorService.shutdownNow();
    }

}
