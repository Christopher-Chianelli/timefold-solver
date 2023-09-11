package ai.timefold.solver.core.enterprise;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.FitProcessor;

public interface MultithreadedRecommendationEnterpriseService {

    static MultithreadedRecommendationEnterpriseService load(Integer moveThreadCount) {
        ServiceLoader<MultithreadedRecommendationEnterpriseService> serviceLoader =
                ServiceLoader.load(MultithreadedRecommendationEnterpriseService.class);
        Iterator<MultithreadedRecommendationEnterpriseService> iterator = serviceLoader.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException(
                    "Multi-threaded Recommendation API requested with moveThreadCount (" + moveThreadCount
                            + ") but Timefold Solver Enterprise Edition not found on classpath.\n" +
                            "Either add the ai.timefold.solver.enterprise:timefold-solver-enterprise-core dependency, " +
                            "or remove moveThreadCount from solver configuration.\n" +
                            "Note: Timefold Solver Enterprise Edition is a commercial product.");
        }
        return iterator.next();
    }

    <Solution_, In_, Out_, Score_ extends Score<Score_>> FitProcessor<Solution_, Out_, Score_> buildFitProcessor(
            int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector, Function<In_, Out_> valueResultFunction,
            Score_ originalScore, In_ clonedElement);

}
