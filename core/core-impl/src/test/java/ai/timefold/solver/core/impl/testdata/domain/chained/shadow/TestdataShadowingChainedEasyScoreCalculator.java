package ai.timefold.solver.core.impl.testdata.domain.chained.shadow;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public class TestdataShadowingChainedEasyScoreCalculator
        implements EasyScoreCalculator<TestdataShadowingChainedSolution, SimpleScore> {

    @Override
    public SimpleScore calculateScore(TestdataShadowingChainedSolution testdataShadowingChainedSolution) {
        int score = 0;
        for (var anchor : testdataShadowingChainedSolution.getChainedAnchorList()) {
            score -= countChainLength(anchor);
        }
        return SimpleScore.of(score);
    }

    private int countChainLength(TestdataShadowingChainedObject object) {
        if (object.getNextEntity() == null) {
            return 1;
        } else { // Penalize increasing lengths increasingly more.
            return (int) Math.pow(1 + countChainLength(object.getNextEntity()), 2);
        }
    }
}
