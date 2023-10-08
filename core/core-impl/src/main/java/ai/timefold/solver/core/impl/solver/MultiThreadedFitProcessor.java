package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

final class MultiThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        extends AbstractFitProcessor<Solution_, In_, Out_, Score_> {

    private final Function<In_, Out_> valueResultFunction;
    private final AtomicLong nextAvailableMoveIndex;
    private final In_ clonedElement;
    private final int moveThreadCount;

    public MultiThreadedFitProcessor(SolverFactory<Solution_> solverFactory, Function<In_, Out_> valueResultFunction,
            Score_ originalScore, In_ clonedElement, int moveThreadCount) {
        super(solverFactory, originalScore);
        this.valueResultFunction = valueResultFunction;
        this.nextAvailableMoveIndex = new AtomicLong(0);
        this.clonedElement = clonedElement;
        this.moveThreadCount = moveThreadCount;
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var executor = Executors.newFixedThreadPool(moveThreadCount);
        try {
            var resultSet = new ConcurrentSkipListSet<RecommendedFit<Out_, Score_>>();
            var futureList = new ArrayList<Future<?>>();
            for (int i = 0; i < moveThreadCount; i++) {
                var future = executor.submit(() -> run(scoreDirector, resultSet));
                futureList.add(future);
            }
            for (var future : futureList) {
                future.get();
            }
            return new ArrayList<>(resultSet);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void run(InnerScoreDirector<Solution_, Score_> parentScoreDirector, SortedSet<RecommendedFit<Out_, Score_>> targetCollection) {
        var entityPlacer = buildEntityPlacer();
        var solverScope = new SolverScope<Solution_>();
        var phaseScope = new ConstructionHeuristicPhaseScope<>(solverScope);
        var stepScope = new ConstructionHeuristicStepScope<>(phaseScope);

        try (InnerScoreDirector<Solution_, Score_> childScoreDirector =
                parentScoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD)) {
            solverScope.setWorkingRandom(new Random(0)); // We will evaluate every option; random does not matter.
            solverScope.setScoreDirector(childScoreDirector);

            entityPlacer.solvingStarted(solverScope);
            entityPlacer.phaseStarted(phaseScope);
            entityPlacer.stepStarted(stepScope);

            // On the child score director, we need to look up the working object for the cloned element.
            Function<In_, Out_> threadPropositionFunction =
                    in -> parentScoreDirector.lookUpWorkingObject(valueResultFunction.apply(in));
            var threadClonedElement = childScoreDirector.lookUpWorkingObject(clonedElement);

            for (var placement : entityPlacer) {
                var moveIndex = 0L;
                var nextAvailableMoveIndexForPlacement = nextAvailableMoveIndex.getAndIncrement();
                for (var move : placement) {
                    if (moveIndex == nextAvailableMoveIndexForPlacement) {
                        var recommendedFit =
                                execute(childScoreDirector, move, moveIndex, threadClonedElement, threadPropositionFunction);
                        targetCollection.add(recommendedFit);
                        nextAvailableMoveIndexForPlacement = nextAvailableMoveIndex.getAndIncrement();
                    }
                    moveIndex++;
                }
                return;
            }
            throw new IllegalStateException("""
                    Impossible state: entity placer (%s) has no placements.
                    """.formatted(entityPlacer));
        } finally {
            entityPlacer.stepEnded(stepScope);
            entityPlacer.phaseEnded(phaseScope);
            entityPlacer.solvingEnded(solverScope);
        }
    }

}
