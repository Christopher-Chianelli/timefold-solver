package ai.timefold.solver.core.impl.testdata.domain.shadow;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class TestdataShadowedEasyScoreCalculator
        implements EasyScoreCalculator<TestdataShadowedSolution, SimpleScore> {

    @Override
    public SimpleScore calculateScore(TestdataShadowedSolution workingSolution) {
        int score = 0;
        for (TestdataShadowedEntity left : workingSolution.getEntityList()) {
            TestdataValue value = left.getValue();
            if (value == null) {
                continue;
            }
            for (TestdataShadowedEntity right : workingSolution.getEntityList()) {
                if (Objects.equals(right.getValue(), value)) {
                    score -= 1;
                }
            }
        }
        return SimpleScore.of(score);
    }
}
