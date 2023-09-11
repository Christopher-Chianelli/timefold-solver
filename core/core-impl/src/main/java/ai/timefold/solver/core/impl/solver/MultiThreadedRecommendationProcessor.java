package ai.timefold.solver.core.impl.solver;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

final class MultiThreadedRecommendationProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements RecommendationProcessor<Solution_, Out_, Score_> {

    private final int moveThreadCount;
    private final AtomicLong createdScoreDirectorCount = new AtomicLong(0);
    private final InnerScoreDirector<Solution_, Score_> parentScoreDirector;
    private final BlockingQueue<InnerScoreDirector<Solution_, Score_>> availableScoreDirectorQueue;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final In_ originalElement;
    private final ExecutorService executorService;

    public MultiThreadedRecommendationProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
        this.moveThreadCount = moveThreadCount;
        this.executorService = Executors.newFixedThreadPool(moveThreadCount);
        this.parentScoreDirector = scoreDirector;
        this.availableScoreDirectorQueue = new LinkedBlockingQueue<>(); // Don't make it bounded.
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
        this.originalElement = originalElement;
    }

    @Override
    public CompletableFuture<Recommendation<Out_, Score_>> execute(Move<Solution_> move) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var scoreDirector = getOrCreateScoreDirector();
                Out_ result;
                Score_ newScore;
                try {
                    var undo = move.rebase(scoreDirector)
                            .doMove(scoreDirector);
                    newScore = scoreDirector.calculateScore();
                    var clonedElement = scoreDirector.lookUpWorkingObject(originalElement);
                    result = valueResultFunction.apply(clonedElement);
                    undo.doMoveOnly(scoreDirector);
                } finally {
                    /*
                     * Once we've taken the score director, we must return it.
                     * This call will always pass because the queue is not bounded.
                     */
                    this.availableScoreDirectorQueue.offer(scoreDirector);
                }
                var newScoreDifference = newScore.subtract(originalScore)
                        .withInitScore(0);
                return new DefaultRecommendation<>(result, newScoreDifference);
            } catch (Exception ex) {
                throw new IllegalStateException("Recommendation execution threw an exception.", ex);
            }
        }, executorService);
    }

    private InnerScoreDirector<Solution_, Score_> getOrCreateScoreDirector() throws InterruptedException {
        var createdScoreDirectorCount = this.createdScoreDirectorCount.getAndAccumulate(1,
                (a, b) -> (a + b) >= moveThreadCount ? moveThreadCount : (a + b));
        return createdScoreDirectorCount < moveThreadCount
                ? parentScoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD)
                : availableScoreDirectorQueue.take();
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        availableScoreDirectorQueue.forEach(InnerScoreDirector::close);
    }
}
