package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.enterprise.MultithreadedRecommendationEnterpriseService;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class Fitter<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> {

    private final DefaultSolverFactory<Solution_> solverFactory;
    private final Solution_ originalSolution;
    private final In_ originalElement;
    private final Function<In_, Out_> propositionFunction;

    public Fitter(DefaultSolverFactory<Solution_> solverFactory, Solution_ originalSolution, In_ originalElement,
            Function<In_, Out_> propositionFunction) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
        this.originalSolution = Objects.requireNonNull(originalSolution);
        this.originalElement = originalElement;
        this.propositionFunction = Objects.requireNonNull(propositionFunction);
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var originalScore = scoreDirector.calculateScore();
        var clonedElement = scoreDirector.lookUpWorkingObject(originalElement);
        var uninitializedCount = solutionDescriptor.countUninitialized(originalSolution);
        if (uninitializedCount > 1) {
            throw new IllegalStateException("""
                    Solution (%s) has (%d) uninitialized elements.
                    Fit Recommendation API requires at most one uninitialized element in the solution.
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
        try (var processor = getProcessor(scoreDirector, originalScore, clonedElement)) {
            for (var placement : entityPlacer) {
                List<RecommendedFit<Out_, Score_>> recommendedFitList = new CopyOnWriteArrayList<>();
                CompletableFuture<Void> allTasksFinished = CompletableFuture.completedFuture(null);
                for (var move : placement) {
                    allTasksFinished = CompletableFuture.allOf(allTasksFinished, processor.execute(move));
                }
                allTasksFinished.join(); // Wait for all tasks to finish.
                scoreDirector.calculateScore(); // Return solution to original state.
                return processor.getRecommendations(); // There are no other unassigned elements to evaluate.
            }
            throw new IllegalStateException("""
                    Impossible state: entity placer (%s) has no placements.
                    """.formatted(entityPlacer));
        } catch (Exception ex) {
            throw new IllegalStateException("""
                    Fit Recommendation API failed to evaluate the solution (%s).
                    """.formatted(originalSolution), ex);
        } finally {
            entityPlacer.stepEnded(stepScope);
            entityPlacer.phaseEnded(phaseScope);
            entityPlacer.solvingEnded(solverScope);
        }
    }

    private FitProcessor<Solution_, Out_, Score_>
            getProcessor(InnerScoreDirector<Solution_, Score_> parentScoreDirector, Score_ originalScore, In_ clonedElement) {
        // Recommendation API scales linearly, no need for artificial upper move thread count limit.
        var moveThreadCount = solverFactory.resolveMoveThreadCount(false);
        if (moveThreadCount == null) {
            return new SingleThreadedFitProcessor<>(parentScoreDirector, propositionFunction, originalScore,
                    clonedElement);
        } else {
            return MultithreadedRecommendationEnterpriseService.load(moveThreadCount)
                    .buildFitProcessor(moveThreadCount, parentScoreDirector, propositionFunction, originalScore, clonedElement);
        }
    }

    private EntityPlacer<Solution_> buildEntityPlacer() {
        DefaultSolver<Solution_> solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        List<Phase<Solution_>> phaseList = solver.getPhaseList();
        var phase = phaseList.get(0);
        if (phase instanceof DefaultConstructionHeuristicPhase<Solution_> constructionHeuristicPhase) {
            return constructionHeuristicPhase.getEntityPlacer();
        } else {
            throw new IllegalStateException(
                    "Fit Recommendation API requires the first solver phase (%s) in the solver config to be a construction heuristic."
                            .formatted(phase));
        }
    }

}
