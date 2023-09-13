package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class MultiThreadedRecommendationEvaluator<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements Runnable {
    private final ThreadState<Solution_, In_, Out_, Score_> state;
    private final Function<In_, Out_> valueResultFunction;
    private final Score_ originalScore;
    private final ConcurrentLinkedDeque<MoveRequest<Solution_>> moveRequests = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean isNotDone;
    private final Semaphore newWorkAvailable = new Semaphore(0);

    public MultiThreadedRecommendationEvaluator(
            ThreadState<Solution_, In_, Out_, Score_> state,
            Function<In_, Out_> valueResultFunction,
            Score_ originalScore,
            AtomicBoolean isNotDone) {
        this.state = state;
        this.valueResultFunction = valueResultFunction;
        this.originalScore = originalScore;
        this.isNotDone = isNotDone;
    }

    @Override
    public void run() {
        while (isNotDone.get()) {
            if (newWorkAvailable.tryAcquire()) {
                var move = moveRequests.poll();
                evaluateMove(move.id, move.move, move.semaphore);
            } else {
                Thread.onSpinWait();
            }
        }
    }

    public void queueMove(long id, Move<Solution_> move, Semaphore finishedMoveSemaphore) {
        moveRequests.push(new MoveRequest<>(id, move, finishedMoveSemaphore));
        newWorkAvailable.release(1);
    }

    public void evaluateMove(long id, Move<Solution_> move, Semaphore finishedMoveSemaphore) {
        try {
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
        } finally {
            finishedMoveSemaphore.release();
        }
    }

    private record MoveRequest<Solution_>(long id, Move<Solution_> move, Semaphore semaphore) {
    }

    protected record ThreadState<Solution_, In_, Out_, Score_ extends Score<Score_>>(
            InnerScoreDirector<Solution_, Score_> scoreDirector, In_ clonedElement,
            List<RecommendedFit<Out_, Score_>> bufferList) {
    }
}
