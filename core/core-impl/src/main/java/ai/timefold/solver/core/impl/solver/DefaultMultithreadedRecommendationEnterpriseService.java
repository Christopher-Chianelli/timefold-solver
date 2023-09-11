package ai.timefold.solver.core.impl.solver;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.enterprise.MultithreadedRecommendationEnterpriseService;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class DefaultMultithreadedRecommendationEnterpriseService implements MultithreadedRecommendationEnterpriseService {
    @Override
    public <Solution_, In_, Out_, Score_ extends Score<Score_>> RecommendationProcessor<Solution_, Out_, Score_>
            buildProcessor(int moveThreadCount, InnerScoreDirector<Solution_, Score_> scoreDirector,
                    Function<In_, Out_> valueResultFunction, Score_ originalScore, In_ clonedElement) {
        return new MultiThreadedRecommendationProcessor<>(moveThreadCount, scoreDirector, valueResultFunction, originalScore,
                clonedElement);
    }

}
