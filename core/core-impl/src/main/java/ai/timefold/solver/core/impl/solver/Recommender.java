package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Recommendation;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

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
        try {
            List<Recommendation<Out_, Score_>> recommendationList = new ArrayList<>();
            var started = false;
            for (var placement : entityPlacer) {
                if (started) {
                    throw new IllegalStateException("Impossible state: entity placer should only return one element.");
                } else {
                    started = true;
                }
                for (var move : placement) {
                    var undo = move.doMove(scoreDirector);
                    var newScore = scoreDirector.calculateScore();
                    var newScoreDifference = newScore.subtract(originalScore)
                            .withInitScore(0);
                    var result = valueResultFunction.apply(clonedElement);
                    recommendationList.add(new DefaultRecommendation<>(result, newScoreDifference));
                    undo.doMoveOnly(scoreDirector);
                }
                recommendationList.sort(null); // Recommendations are Comparable.
                scoreDirector.calculateScore(); // Return solution to original state.
            }
            return recommendationList;
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

}
