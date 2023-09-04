package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

final class Recommender<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<Recommendation<Out_, Score_>>> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final Solution_ originalSolution;
    private final In_ originalElement;
    private final Function<In_, Out_> valueResultFunction;

    public Recommender(DefaultSolverFactory<Solution_> solverFactory, Solution_ originalSolution, In_ originalElement,
            Function<In_, Out_> valueResultFunction) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
        this.originalSolution = Objects.requireNonNull(originalSolution);
        this.originalElement = originalElement;
        this.valueResultFunction = Objects.requireNonNull(valueResultFunction);
    }

    @Override
    public List<Recommendation<Out_, Score_>> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var originalScore = scoreDirector.calculateScore();
        var clonedElement = scoreDirector.lookUpWorkingObject(originalElement);
        var uninitializedCount = solutionDescriptor.countUninitialized(originalSolution);
        if (uninitializedCount != 1) {
            throw new IllegalStateException("""
                    Solution (%s) has (%d) uninitialized elements.
                    Recommendation API requires exactly one uninitialized element in the solution.
                    """
                    .formatted(originalSolution, uninitializedCount));
        }
        var entityPlacer = buildEntityPlacer();

        var solverScope = new SolverScope<Solution_>();
        solverScope.setWorkingRandom(new Random(0)); // We will evaluate every option; random does not matter.
        solverScope.setScoreDirector(scoreDirector);
        var phaseScope = new ConstructionHeuristicPhaseScope<>(solverScope);
        var stepScope = new ConstructionHeuristicStepScope<>(phaseScope);
        entityPlacer.solvingStarted(solverScope);
        entityPlacer.phaseStarted(phaseScope);
        entityPlacer.stepStarted(stepScope);
        try (var execution =
                new MultiThreadedRecommenderExecution<>(scoreDirector, valueResultFunction, originalScore, clonedElement)) {
            for (var placement : entityPlacer) {
                List<CompletableFuture<Recommendation<Out_, Score_>>> futureList = new ArrayList<>();
                for (var move : placement) {
                    futureList.add(execution.execute(move));
                }
                List<Recommendation<Out_, Score_>> recommendationList = new ArrayList<>(futureList.size());
                for (var future : futureList) {
                    var recommendation = future.get();
                    recommendationList.add(recommendation);
                }
                recommendationList.sort(null); // Recommendations are Comparable.
                scoreDirector.calculateScore(); // Return solution to original state.
                return recommendationList; // There are no other unassigned elements to evaluate.
            }
            throw new IllegalStateException("""
                    Impossible state: entity placer (%s) has no placements.
                    """.formatted(entityPlacer));
        } catch (Exception ex) {
            throw new IllegalStateException("""
                    Recommendation API failed to evaluate the solution (%s).
                    """.formatted(originalSolution), ex);
        } finally {
            entityPlacer.stepEnded(stepScope);
            entityPlacer.phaseEnded(phaseScope);
            entityPlacer.solvingEnded(solverScope);
        }
    }

    private EntityPlacer<Solution_> buildEntityPlacer() {
        DefaultSolver<Solution_> solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        List<Phase<Solution_>> phaseList = solver.getPhaseList();
        var phase = phaseList.get(0);
        if (phase instanceof DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase) {
            return constructionHeuristicPhase.getEntityPlacer();
        } else {
            throw new IllegalStateException("""
                    Recommendation API requires the first solver phase (%s) in the solver config to be a construction heuristic.
                    """
                    .formatted(phase));
        }
    }

    private static final class SingleThreadedRecommenderExecution<Solution_, In_, Out_, Score_ extends Score<Score_>>
            implements RecommenderExecution<Solution_, Out_, Score_> {

        private final InnerScoreDirector<Solution_, Score_> scoreDirector;
        private final Function<In_, Out_> valueResultFunction;
        private final Score_ originalScore;
        private final In_ clonedElement;

        public SingleThreadedRecommenderExecution(InnerScoreDirector<Solution_, Score_> scoreDirector,
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

    private static final class MultiThreadedRecommenderExecution<Solution_, In_, Out_, Score_ extends Score<Score_>>
            implements RecommenderExecution<Solution_, Out_, Score_> {

        private final int moveThreadCount = 8;
        private final AtomicLong createdScoreDirectorCount = new AtomicLong(0);
        private final InnerScoreDirector<Solution_, Score_> parentScoreDirector;
        private final BlockingQueue<InnerScoreDirector<Solution_, Score_>> availableScoreDirectorQueue;
        private final Function<In_, Out_> valueResultFunction;
        private final Score_ originalScore;
        private final In_ originalElement;
        private final ExecutorService executorService;

        public MultiThreadedRecommenderExecution(InnerScoreDirector<Solution_, Score_> scoreDirector,
                Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ originalElement) {
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

    private interface RecommenderExecution<Solution_, Out_, Score_ extends Score<Score_>> extends AutoCloseable {

        CompletableFuture<Recommendation<Out_, Score_>> execute(Move<Solution_> move);

    }

}
