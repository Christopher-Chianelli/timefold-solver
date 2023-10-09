package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

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

        // Recommendation API scales linearly, no need for artificial upper move thread count limit.
        var moveThreadCount = solverFactory.resolveMoveThreadCount(false);
        var processor = moveThreadCount == null
                ? new SingleThreadedFitProcessor<>(solverFactory, propositionFunction, originalScore, clonedElement)
                : new MultiThreadedFitProcessor<>(solverFactory, propositionFunction, originalScore, clonedElement,
                        moveThreadCount);
        return processor.apply(scoreDirector); // TODO enterprise
    }

}
