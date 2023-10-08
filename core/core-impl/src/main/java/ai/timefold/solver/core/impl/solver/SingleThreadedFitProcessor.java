package ai.timefold.solver.core.impl.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

final class SingleThreadedFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        extends AbstractFitProcessor<Solution_, In_, Out_, Score_> {

    private final Function<In_, Out_> valueResultFunction;
    private final In_ clonedElement;

    public SingleThreadedFitProcessor(SolverFactory<Solution_> solverFactory, Function<In_, Out_> valueResultFunction,
            Score_ originalScore, In_ clonedElement) {
        super(solverFactory, originalScore);
        this.valueResultFunction = valueResultFunction;
        this.clonedElement = clonedElement;
    }

    @Override
    public List<RecommendedFit<Out_, Score_>> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var entityPlacer = buildEntityPlacer();

        var solverScope = new SolverScope<Solution_>();
        solverScope.setWorkingRandom(new Random(0)); // We will evaluate every option; random does not matter.
        solverScope.setScoreDirector(scoreDirector);
        var phaseScope = new ConstructionHeuristicPhaseScope<>(solverScope);
        var stepScope = new ConstructionHeuristicStepScope<>(phaseScope);
        entityPlacer.solvingStarted(solverScope);
        entityPlacer.phaseStarted(phaseScope);
        entityPlacer.stepStarted(stepScope);

        try (scoreDirector) {
            for (var placement : entityPlacer) {
                var recommendedFitList = new ArrayList<RecommendedFit<Out_, Score_>>();
                var moveIndex = 0L;
                for (var move : placement) {
                    recommendedFitList.add(execute(scoreDirector, move, moveIndex, clonedElement, valueResultFunction));
                    moveIndex++;
                }
                recommendedFitList.sort(null);
                return recommendedFitList;
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
