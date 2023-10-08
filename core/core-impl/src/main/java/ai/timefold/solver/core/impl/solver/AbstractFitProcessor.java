package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public abstract class AbstractFitProcessor<Solution_, In_, Out_, Score_ extends Score<Score_>>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<RecommendedFit<Out_, Score_>>> {

    protected final SolverFactory<Solution_> solverFactory;
    protected final Score_ originalScore;

    public AbstractFitProcessor(SolverFactory<Solution_> solverFactory, Score_ originalScore) {
        this.solverFactory = solverFactory;
        this.originalScore = originalScore;
    }

    protected RecommendedFit<Out_, Score_> execute(InnerScoreDirector<Solution_, Score_> scoreDirector, Move<Solution_> move,
            long moveIndex, In_ clonedElement, Function<In_, Out_> propositionFunction) {
        var undo = move.doMove(scoreDirector);
        var newScore = scoreDirector.calculateScore();
        var newScoreDifference = newScore.subtract(originalScore)
                .withInitScore(0);
        var result = propositionFunction.apply(clonedElement);
        var recommendation = new DefaultRecommendedFit<>(moveIndex, result, newScoreDifference);
        undo.doMoveOnly(scoreDirector);
        return recommendation;
    }

    protected EntityPlacer<Solution_> buildEntityPlacer() {
        var solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        var phaseList = solver.getPhaseList();
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
